import copy
from dataclasses import replace
from datetime import datetime, timezone
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from diagnostics.model import (CloneMetadata, Evidence, GuestKey, Result, Scope,
                               State, SUBSYSTEMS, capability, legacy_policy,
                               validate_matrix_row)
from diagnostics.report import atomic_write, evaluate, render

NOW = datetime(2026, 10, 10, 12, tzinfo=timezone.utc)
SCOPE = Scope(36, "16", "arm64-v8a", "fixture-build", "fixture-device",
              "com.phantom.fixture", 1, 0)


def trial(key="capture", requirement="camera.output", result=Result.PASS,
          at="2026-10-10T11:00:00Z", kind="RUNTIME", scope=SCOPE):
    return Evidence(key, requirement, "test-" + key, kind, result, scope, at,
                    ("sanitized-fixture-artifact",), "Synthetic contract fixture, not a device claim")


class Contracts(unittest.TestCase):
    def test_build_legacy_and_source_are_not_active(self):
        for kind in ("BUILD", "CONTRACT", "LEGACY"):
            self.assertEqual(State.UNKNOWN, capability("camera_provider", ("camera.output",),
                                                      [trial(kind=kind)], SCOPE, now=NOW).state)
        self.assertEqual(State.SUPPORTED, capability("camera_provider", ("camera.output",), [],
                                                    SCOPE, now=NOW, platform_eligible=True).state)

    def test_all_requirements_needed(self):
        req = ("camera.open", "camera.output")
        first = trial(requirement=req[0])
        self.assertEqual(State.DEGRADED, capability("camera_provider", req, [first], SCOPE, now=NOW).state)
        second = trial("output", req[1])
        self.assertEqual(State.ACTIVE, capability("camera_provider", req, [first, second], SCOPE, now=NOW).state)

    def test_latest_failure_overrides_pass_independent_of_order(self):
        old = trial()
        failed = trial("failure", result=Result.FAIL, at="2026-10-10T11:30:00Z")
        for trials in ([old, failed], [failed, old]):
            self.assertEqual(State.FAILED, capability("camera_provider", ("camera.output",),
                                                     trials, SCOPE, now=NOW).state)

    def test_scope_cannot_transfer_across_guest_device_or_build(self):
        for scope in (replace(SCOPE, sdk=35), replace(SCOPE, abi="armeabi-v7a"),
                      replace(SCOPE, runtime_build="new-build"), replace(SCOPE, device_class="other"),
                      replace(SCOPE, app_version_code=2), replace(SCOPE, virtual_user_id=1)):
            self.assertEqual(State.UNKNOWN, capability("camera_provider", ("camera.output",),
                                                      [trial()], scope, now=NOW).state)

    def test_expiry_future_and_ambiguous_trials(self):
        self.assertEqual(State.UNKNOWN, capability("camera_provider", ("camera.output",),
                                                  [trial(at="2026-08-01T00:00:00Z")], SCOPE, now=NOW).state)
        with self.assertRaises(ValueError):
            capability("camera_provider", ("camera.output",), [trial(at="2026-10-11T00:00:00Z")], SCOPE, now=NOW)
        with self.assertRaises(ValueError):
            capability("camera_provider", ("camera.output",), [trial(), trial("conflict", result=Result.FAIL)], SCOPE, now=NOW)

    def test_runtime_claim_requires_artifact_and_timezone(self):
        with self.assertRaises(ValueError):
            replace(trial(), artifacts=())
        with self.assertRaises(ValueError):
            replace(trial(), tested_at="2026-10-10T11:00:00")

    def test_matrix_cannot_cherry_pick_old_pass(self):
        good = trial()
        row = {"scope": SCOPE, "requirement_id": "camera.output", "result": "PASS", "evidence_ids": [good.evidence_id]}
        self.assertEqual(Result.PASS, validate_matrix_row(row, {good.evidence_id: good}, now=NOW))
        bad = trial("failure", result=Result.FAIL, at="2026-10-10T11:30:00Z")
        with self.assertRaises(ValueError):
            validate_matrix_row(row, {good.evidence_id: good, bad.evidence_id: bad}, now=NOW)
        with self.assertRaises(ValueError):
            validate_matrix_row({**row, "evidence_ids": []}, {}, now=NOW)

    def test_policy_is_read_only_and_user_scoped(self):
        source = {"schema_version": 1, "package_name": "com.phantom.fixture", "virtual_user_id": 0,
                  "permissions": {"android.permission.CAMERA": 2}, "storage_redirect": False,
                  "global_camera_package": "com.example.gcam", "configured_location_mode": "DEVICE_PASSTHROUGH"}
        original = copy.deepcopy(source)
        first = legacy_policy(source)
        self.assertEqual(original, source)
        source["permissions"]["android.permission.CAMERA"] = 1
        second = legacy_policy({**source, "virtual_user_id": 1})
        self.assertEqual("DENY", first.permissions["android.permission.CAMERA"])
        self.assertEqual("ALLOW_WITH_HOST_GRANT", second.permissions["android.permission.CAMERA"])
        self.assertNotEqual(first.guest, second.guest)
        self.assertEqual("inherit-global", first.camera_scope)
        self.assertIsNone(first.network)
        with self.assertRaises(TypeError):
            first.permissions["x"] = "ALLOW"
        with self.assertRaises(ValueError):
            legacy_policy({**source, "schema_version": 2})
        with self.assertRaises(ValueError):
            legacy_policy({**source, "permissions": {"x": 999}})
        supplied = {"android.permission.CAMERA": "DENY"}
        direct = replace(first, permissions=supplied)
        supplied["android.permission.CAMERA"] = "INHERIT"
        self.assertEqual("DENY", direct.permissions["android.permission.CAMERA"])
        with self.assertRaises(ValueError):
            replace(first, schema_version=True)

    def test_clone_rename_is_metadata_not_identity(self):
        work = CloneMetadata(GuestKey("com.phantom.fixture", 1), "Work", "1")
        renamed = replace(work, display_name="Shop")
        self.assertEqual(work.guest, renamed.guest)
        self.assertNotEqual(work.guest, GuestKey("com.phantom.fixture", 0))
        with self.assertRaises(ValueError):
            GuestKey("../escape", 0)

    def test_report_does_not_trust_claimed_active_or_html(self):
        from dataclasses import asdict
        doc = {"schema_version": 1, "scope": asdict(SCOPE),
               "requirements": {key: [key + ".health"] for key in SUBSYSTEMS}, "evidence": [],
               "claimed_state": "ACTIVE", "historical_notes": ["<script>alert(1)</script>"]}
        caps, _, _ = evaluate(doc, now=NOW)
        self.assertTrue(all(c.state == State.UNKNOWN for c in caps))
        html = render(doc, now=NOW)
        self.assertNotIn("<script>", html)
        self.assertIn("&lt;script&gt;", html)
        with self.assertRaises(ValueError):
            evaluate({**doc, "schema_version": 2}, now=NOW)

    def test_failed_report_write_preserves_previous_artifact(self):
        with tempfile.TemporaryDirectory() as folder:
            output = Path(folder) / "report.html"
            output.write_text("original", encoding="utf-8")
            with patch("diagnostics.report.os.replace", side_effect=OSError("fixture failure")):
                with self.assertRaises(OSError):
                    atomic_write(output, "new")
            self.assertEqual("original", output.read_text(encoding="utf-8"))
            self.assertEqual([output], list(Path(folder).iterdir()))


if __name__ == "__main__":
    unittest.main()
