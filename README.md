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
CI build success cannot activate a subsystem. The development build now adds a
[read-only Android Settings surface](docs/ANDROID_DIAGNOSTICS_INTEGRATION.md).
Guest probes, per-guest enforcement migration and clone-slot actions remain subsequent work.
No release is produced automatically for these architecture/tooling changes.

The current 0.4.9-dev build also fixes native opening of guest external-cache
capture outputs. DuckDuckGo file-input photo capture now returns a preview on the
tested Android 16 device with Timestamp Camera and the system camera. See the
[capture fix and validation scope](docs/CAMERA_WEB_CAPTURE_FIX.md).

Development code 75 fixes the observed Google Maps guest startup crashes on the
tested Android 16 device. Maps opens, searches and loads map/place content; Google
account login and navigation remain unverified. See [Maps validation](docs/MAPS_GUEST_STARTUP_FIX.md).
