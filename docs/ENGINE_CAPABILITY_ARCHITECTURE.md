# Engine capability architecture

A capability is a scoped observation, not a hook registry entry. Backend remains VirtualApp. The first implementation lives in offline `diagnostics/` tooling; it is not wired into the APK or system services.

## Contract

States: UNKNOWN (no fresh eligible result), SUPPORTED (source/platform eligibility only), ACTIVE (all declared runtime requirements passed), DEGRADED (partial results or bounded fallback), FAILED (runtime failure without a passing requirement), NOT_APPLICABLE (explicit platform rule). Build success cannot create ACTIVE. GMS experimental is a qualifier, not a seventh health state.

Each record identifies subsystem, reason, Android SDK/release, ABI, runtime build revision, device class, virtual user/app/version where applicable, last tested time, evidence IDs, fallback and optional diagnostic details. Page size and kernel UID may be added as environment facts without claiming isolation. Every absent field is UNKNOWN, not inferred PASS.

## Evidence flow

Requirement → test ID → immutable scoped observation → eligible evidence → capability reducer → read-only report. Runtime evidence requires a timezone-aware timestamp, test/requirement IDs, environment scope, result and sanitized artifact references. BUILD and CONTRACT observations remain visible but cannot activate runtime state. An evidence reference records artifact SHA-256 when captured; unavailable historical logs are marked legacy and cannot activate current state.

Requirements are explicit per subsystem. Select the latest runtime trial for EACH required requirement in the exact same scope; a newer FAIL overrides an older PASS even when input order differs. Missing requirements yield DEGRADED when some pass, otherwise UNKNOWN. TTL expiry/build/SDK/ABI/device mismatch prevents old passes transferring. A fresh test on one OEM does not apply to another device class. A host version change requires revalidation; docs-only changes do not invent a new APK identity.

Initial catalog: package manager, activity manager, ATM, instrumentation, Binder, file/IO, native IO, guest permissions, location, camera provider, browser routing, WebView, notifications, clipboard, GMS, native loading, PendingIntent, broadcast, service, provider, RoleManager, storage and Android16 transactions. Catalog existence establishes UNKNOWN only.

## Settings surface (next integration)

Settings → Engine Diagnostics shows the actual current environment and state per subsystem. Tapping shows requirement outcomes, time, evidence, failure reason and fallback; historical results are a separate section. It must not run intrusive guest actions on opening. Export is explicit, sanitized and excludes account/token/location/notification/file contents. The offline HTML surface implements this read-only presentation before APK wiring.

Collectors must live outside hot Binder/native paths and use bounded asynchronous work. A host collector and disposable guest exchange records via a private host endpoint, verifying host UID, guest key and run nonce. No public exported diagnostic receiver; no change to real system permissions or cached service injection in this phase.

Rollback: remove only diagnostic UI/collectors; no capability state controls enforcement until a separate reviewed integration gate.
