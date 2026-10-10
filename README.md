# PHANToM VPhone

Android application container and camera capture intent router based on VirtualApp.

The root app module is the original import/test harness. The runtime application
is built by `.github/workflows/phantom-vphone-rc.yml`, which fetches a pinned
VirtualApp commit and applies `runtime_patch` and `runtime_overlay`.

Version 0.4.5 uses a signed, non-debuggable release build. Guest applications share
the host UID; per-guest Deny is a virtual PackageManager policy, not a Linux sandbox.
Only camera apps that handle the requested capture action can return capture results.
Embedded Camera2/CameraX calls are not redirected by intent routing.

See `RELEASE_NOTES.md` for device validation and limitations.
