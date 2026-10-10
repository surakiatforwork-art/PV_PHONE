# Read-only Android diagnostics integration

Settings → Engine Diagnostics observes the host build version and source commit,
Android SDK/release, device model/ABI, process bitness, actual Linux UID, page size,
host all-files grant and configured camera package. Refresh repeats these local
observations; it does not launch guests, reinject hooks or change preferences.

All 23 guest capability entries remain UNKNOWN / NOT_TESTED. Details explicitly
state that guest hook presence, guest identity/version/user scope, last runtime
trial and evidence artifacts have not been collected. This is a host facts UI,
not an Android port of the Python evidence reducer or a compatibility matrix.
Previously documented v0.4.8 trials are not silently attributed to this build.

The activity is private (`exported=false`). It requests no permissions, exports
no files, enumerates no user documents and contacts no external service. Existing
VirtualApp patches and guest enforcement are retained. The signed CI artifact is
labelled **0.4.9-dev**, code 68; the published stable release remains v0.4.8.
No GitHub release is created. An update install preserves existing host data.

Boundary checks verify catalog parity, private manifest/build wiring and the
absence of runtime/policy mutation paths. Physical UI checks validate navigation,
refresh and host values; they cannot establish guest subsystem compatibility.

Next integration: a separately scoped diagnostic guest fixture and evidence
import with exact build/device/package/version/user identity. Only then can the
existing evidence contract determine ACTIVE, DEGRADED or FAILED.

## Validation on 2026-10-10

Tested signed arm64 development artifact from commit
`af3bd375a04a8a424c2d2bf70dd9a6d3ec91e811`,
[CI run 38041120104](https://github.com/surakiatforwork-art/PV_PHONE/actions/runs/38041120104).
SHA-256: `36f9f8cb2ebbd015821804741d012ec432ef1247fe22144c2757bcec655aed7a`.
Stable signing certificate matches the installed v0.4.8 key; update install passed.
CI produced and verified signatures for arm64 and universal APKs. Physical testing
used arm64 only, Infinix X6856 / Android 16 / SDK 36 / 64-bit process.

- Settings entry, refresh, subsystem detail dialog, all 23 scrollable rows and
  Android back navigation passed.
- Host UID 10196 and page size 4096 match independent ADB observations. Host
  all-files grant is true; all guest capabilities still report UNKNOWN / NOT_TESTED.
- External shell launch is denied because the diagnostics activity is not exported.
- Existing nine visible guests and Pict2Cam selection remain. File Manager+ guest
  reaches its main screen after the update, without an all-files permission loop.
  This is a startup smoke check, not a repeated storage/camera/GMS compatibility test.
- 13 diagnostics boundary/evidence contract tests and existing permission-policy
  regression passed. All five workflows for this build commit succeeded.

This validates the diagnostics UI and its host scope only. No evidence for ACTIVE
guest capabilities was generated or imported. The latest GitHub release remains
v0.4.8; no development tag/release was published.
