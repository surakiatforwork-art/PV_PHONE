# Compatibility for 0.4.6

Device: Infinix X6856, Android 16 / API 36, arm64. Date: 10 October 2026.

| Scenario | Result |
|---|---|
| Signed update retaining guests | Passed |
| Channel/group create, read, list, delete, recreate | Passed with two guests |
| Identical channel IDs across packages | Isolated; deletion in one left the other intact |
| Notification posting | Passed; host channel has guest/user namespace |
| Two-way provider caller identity | Passed |
| Application locale set/get/restore | Passed; restart/recreation may be needed |
| Direct and PendingIntent activity transitions | Passed; guest UID and host stub retained |
| Legacy and SigningInfo certificate APIs | Passed; SHA-256 matched apksigner |
| Virtual staged-session query | Passed; empty, no staged backend |
| Transsion camera guest EXTRA_OUTPUT | Passed; RESULT_OK, 3,309,262 bytes during arm64 validation |
| Final universal APK on arm64 | Signer/provider/PendingIntent passed; fresh camera output 3,316,722 bytes |
| DuckDuckGo | Onboarding and HTTPS page passed; full features/background not certified |
| Chrome | Unsupported for normal use: native GPU process failure |
| Google Play / GMS | Unsupported for normal use: device-policy authorization; login not certified |
| Policy rollback and virtual user isolation | JVM harness passed; physical user-1 untested |
| Camera2 / CameraX / getUserMedia redirection | Not implemented |
| Other Android versions, devices and additional ABIs | Untested |

0.4.5 previously passed thumbnail/cancellation, virtual CAMERA Deny/reset, and universal-on-arm64 camera checks.
See ../RELEASE_NOTES.md for current build provenance and limitations.
