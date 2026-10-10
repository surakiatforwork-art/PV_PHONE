# PHANToM VPhone

Android application container and camera capture intent router based on VirtualApp.

The root app module is the original import/test harness. The runtime application
is built by `.github/workflows/phantom-vphone-rc.yml`, which fetches a pinned
VirtualApp commit and applies `runtime_patch` and `runtime_overlay`.

Version 0.4.8 fixes shared-file access, the Android 16 All files access permission loop and File Manager+ native startup crashes. Version 0.4.7 fixes third-party camera provider discovery, including GCam mods with arbitrary package names. Version 0.4.6 improves guest notification, locales, provider identity, signing metadata and PendingIntent routing. DuckDuckGo basic browsing passed on the tested Android 16 device; Chrome and Google Play remain unsupported for normal use.

The signed, non-debuggable release build retains the existing key. Guest applications share
the host UID; per-guest Deny is a virtual PackageManager policy, not a Linux sandbox.
Only camera apps that handle the requested capture action can return capture results.
Embedded Camera2/CameraX calls are not redirected by intent routing.

See `RELEASE_NOTES.md` for device validation and limitations.
See `docs/GUEST_COMPATIBILITY.md` for the runtime changes and paired-guest test procedure.


## Evidence and architecture foundation

The first additive audit/diagnostic iteration retains the v0.4.8 runtime baseline.
Start with [reference gap analysis](docs/REFERENCE_ARCHITECTURE_GAP_ANALYSIS.md),
[implementation status](docs/IMPLEMENTATION_STATUS.md), and
[offline diagnostic tooling](diagnostics/README.md).
The models require scoped runtime evidence for ACTIVE and compatibility PASS;
CI build success cannot activate a subsystem. Android Settings integration,
per-guest enforcement migration and clone-slot actions are subsequent work.
No release is produced automatically for these architecture/tooling changes.
