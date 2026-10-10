"""Original, evidence-gated models. These do not change the Android runtime."""
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone
from enum import Enum
from types import MappingProxyType
import re

SCHEMA_VERSION = 1
SUBSYSTEMS = (
    "package_manager", "activity_manager", "activity_task_manager", "instrumentation",
    "binder", "file_io", "native_io", "guest_permissions", "location", "camera_provider",
    "browser_routing", "webview", "notifications", "clipboard", "gms", "native_loading",
    "pending_intent", "broadcast", "service", "content_provider", "role_manager",
    "storage", "android16_transactions",
)


class State(str, Enum):
    UNKNOWN = "UNKNOWN"
    SUPPORTED = "SUPPORTED"
    ACTIVE = "ACTIVE"
    DEGRADED = "DEGRADED"
    FAILED = "FAILED"
    NOT_APPLICABLE = "NOT_APPLICABLE"


class Result(str, Enum):
    PASS = "PASS"
    PARTIAL = "PARTIAL"
    FAIL = "FAIL"
    BLOCKED = "BLOCKED"
    NOT_TESTED = "NOT_TESTED"


def timestamp(value):
    parsed = datetime.fromisoformat(value.replace("Z", "+00:00"))
    if parsed.tzinfo is None:
        raise ValueError("Evidence timestamp must include a timezone")
    return parsed.astimezone(timezone.utc)


def package(value):
    if not isinstance(value, str) or not re.fullmatch(r"[A-Za-z][A-Za-z0-9_]*(?:\.[A-Za-z][A-Za-z0-9_]*)+", value):
        raise ValueError("Invalid package name")
    return value


@dataclass(frozen=True)
class GuestKey:
    package_name: str
    virtual_user_id: int

    def __post_init__(self):
        package(self.package_name)
        if type(self.virtual_user_id) is not int or self.virtual_user_id < 0:
            raise ValueError("Invalid virtual user")


@dataclass(frozen=True)
class Scope:
    sdk: int
    android_release: str
    abi: str
    runtime_build: str
    device_class: str
    package_name: str = ""
    app_version_code: int | None = None
    virtual_user_id: int | None = None

    def __post_init__(self):
        if type(self.sdk) is not int or self.sdk < 1:
            raise ValueError("Invalid SDK")
        if not all(isinstance(x, str) and x for x in
                   (self.android_release, self.abi, self.runtime_build, self.device_class)):
            raise ValueError("Incomplete runtime scope")
        if self.package_name:
            GuestKey(self.package_name, self.virtual_user_id)
            if type(self.app_version_code) is not int or self.app_version_code < 0:
                raise ValueError("Guest evidence needs the exact app versionCode")
        elif self.app_version_code is not None or self.virtual_user_id is not None:
            raise ValueError("Guest scope needs a package")


@dataclass(frozen=True)
class Evidence:
    evidence_id: str
    requirement_id: str
    test_id: str
    kind: str
    result: Result
    scope: Scope
    tested_at: str
    artifacts: tuple[str, ...]
    observation: str

    def __post_init__(self):
        if not all(isinstance(x, str) and x.strip() for x in
                   (self.evidence_id, self.requirement_id, self.test_id, self.observation)):
            raise ValueError("Incomplete evidence")
        if self.kind not in ("RUNTIME", "BUILD", "CONTRACT", "LEGACY"):
            raise ValueError("Invalid evidence kind")
        if not isinstance(self.result, Result) or not isinstance(self.scope, Scope):
            raise ValueError("Invalid result/scope")
        timestamp(self.tested_at)
        if not isinstance(self.artifacts, tuple) or any(not isinstance(x, str) or not x for x in self.artifacts):
            raise ValueError("Evidence artifacts must be immutable references")
        if self.kind == "RUNTIME" and self.result != Result.NOT_TESTED and not self.artifacts:
            raise ValueError("Runtime claims require artifact references")


@dataclass(frozen=True)
class Capability:
    subsystem: str
    state: State
    reason: str
    scope: Scope
    last_tested_at: str | None
    evidence_ids: tuple[str, ...]
    fallback: str

    def __post_init__(self):
        if self.subsystem not in SUBSYSTEMS or not isinstance(self.state, State):
            raise ValueError("Invalid capability identity/state")
        if not isinstance(self.reason, str) or not self.reason.strip():
            raise ValueError("Capability requires a reason")
        if self.state == State.ACTIVE and (not self.evidence_ids or not self.last_tested_at):
            raise ValueError("ACTIVE cannot exist without test evidence")


def capability(subsystem, requirements, trials, scope, *, now=None, max_age_days=30,
               platform_eligible=False, not_applicable_reason=None, fallback="none recorded"):
    """Latest trial per requirement wins; build/legacy evidence never activates."""
    if (subsystem not in SUBSYSTEMS or not isinstance(requirements, (list, tuple)) or
            not requirements or any(not isinstance(r, str) or not r.strip() for r in requirements) or
            len(set(requirements)) != len(requirements)):
        raise ValueError("Unknown subsystem or invalid requirements")
    if type(max_age_days) is not int or max_age_days < 1:
        raise ValueError("Invalid evidence validity interval")
    now = now or datetime.now(timezone.utc)
    if now.tzinfo is None:
        raise ValueError("Current time must be timezone aware")
    trials = tuple(trials)
    ids = [e.evidence_id for e in trials]
    if len(ids) != len(set(ids)):
        raise ValueError("Duplicate evidence IDs")
    if any(timestamp(e.tested_at) > now + timedelta(seconds=60) for e in trials):
        raise ValueError("Evidence dated in the future")
    if not_applicable_reason:
        return Capability(subsystem, State.NOT_APPLICABLE, not_applicable_reason, scope, None, (), fallback)
    selected = {}
    for trial in trials:
        if trial.kind != "RUNTIME" or trial.scope != scope or trial.requirement_id not in requirements:
            continue
        tested = timestamp(trial.tested_at)
        if now - tested > timedelta(days=max_age_days):
            continue
        previous = selected.get(trial.requirement_id)
        if previous is not None and timestamp(previous.tested_at) == tested:
            # Conflicting simultaneous outcomes cannot be resolved by list order.
            raise ValueError("Ambiguous trials for the same requirement and time")
        if previous is None or timestamp(previous.tested_at) < tested:
            selected[trial.requirement_id] = trial
    chosen = tuple(selected[key] for key in requirements if key in selected)
    passed = sum(e.result == Result.PASS for e in chosen)
    if passed == len(requirements):
        state, reason = State.ACTIVE, "All declared runtime requirements passed in this exact scope"
    elif any(e.result in (Result.PARTIAL, Result.BLOCKED) for e in chosen) or passed:
        state, reason = State.DEGRADED, "Partial, blocked or missing runtime requirements"
    elif any(e.result == Result.FAIL for e in chosen):
        state, reason = State.FAILED, "Latest eligible runtime requirement failed"
    else:
        state = State.SUPPORTED if platform_eligible else State.UNKNOWN
        reason = "Platform eligibility only; runtime not proven" if platform_eligible else "No eligible runtime proof"
    return Capability(subsystem, state, reason, scope,
                      max((e.tested_at for e in chosen), key=timestamp, default=None),
                      tuple(e.evidence_id for e in chosen), fallback)


def validate_matrix_row(row, evidence, *, now=None, max_age_days=30):
    """A compatibility PASS belongs to one guest feature and exact tested scope."""
    if not isinstance(row["scope"], Scope) or not row["scope"].package_name:
        raise ValueError("Matrix rows require an exact guest scope")
    result = Result(row["result"])
    refs = tuple(row.get("evidence_ids", ()))
    if any(key not in evidence for key in refs):
        raise ValueError("Missing matrix evidence")
    if result == Result.PASS:
        cap = capability("activity_manager", (row["requirement_id"],),
                         evidence.values(), row["scope"], now=now,
                         max_age_days=max_age_days)
        if cap.state != State.ACTIVE or not set(cap.evidence_ids).issubset(refs):
            raise ValueError("Matrix PASS has no eligible runtime PASS")
    return result


@dataclass(frozen=True)
class GuestPolicy:
    schema_version: int
    guest: GuestKey
    permissions: object
    camera_package: str | None
    storage_redirect: bool | None
    location_mode: str | None
    visibility: str | None
    clipboard: str | None = None
    notifications: str | None = None
    gms: str | None = None
    network: str | None = None
    camera_scope: str = "inherit-global"
    enforcement: str = "legacy; snapshot does not enforce"
    provenance: tuple[str, ...] = ("explicit legacy export; not a live runtime collector",)


def legacy_policy(export):
    """Normalize an explicit legacy export; never read/write live runtime files."""
    if type(export.get("schema_version")) is not int or export.get("schema_version") != SCHEMA_VERSION:
        raise ValueError("Unsupported legacy export schema")
    guest = GuestKey(export["package_name"], export["virtual_user_id"])
    modes = {0: "INHERIT", 1: "ALLOW_WITH_HOST_GRANT", 2: "DENY"}
    permissions = {}
    for key, value in export.get("permissions", {}).items():
        if not isinstance(key, str) or not key or type(value) is not int or value not in modes:
            raise ValueError("Corrupt legacy permission mode")
        permissions[key] = modes[value]
    camera = export.get("global_camera_package")
    if camera:
        package(camera)
    if camera is not None and not isinstance(camera, str):
        raise ValueError("Invalid legacy camera")
    storage = export.get("storage_redirect")
    if storage is not None and type(storage) is not bool:
        raise ValueError("Invalid storage redirect flag")
    location = export.get("configured_location_mode")
    if location not in (None, "DEVICE_PASSTHROUGH"):
        raise ValueError("Unsupported legacy location mode")
    return GuestPolicy(SCHEMA_VERSION, guest, MappingProxyType(permissions), camera,
                       storage, location, "ISOLATED_CONFIGURED",
                       gms="IMPORTED_NOT_PROVEN" if export.get("gms_imported") is True else None)


@dataclass(frozen=True)
class CloneMetadata:
    guest: GuestKey
    display_name: str | None = None
    badge: str | None = None
    favorite: bool = False
    hidden: bool = False
    schema_version: int = SCHEMA_VERSION

    def __post_init__(self):
        if not isinstance(self.guest, GuestKey) or self.schema_version != SCHEMA_VERSION:
            raise ValueError("Invalid clone identity/schema")
        if self.display_name is not None and (not isinstance(self.display_name, str) or
                                             not self.display_name.strip() or len(self.display_name) > 80):
            raise ValueError("Invalid display name")
        if self.badge is not None and (not isinstance(self.badge, str) or len(self.badge) > 24):
            raise ValueError("Invalid badge")
        if type(self.favorite) is not bool or type(self.hidden) is not bool:
            raise ValueError("Invalid clone flags")
