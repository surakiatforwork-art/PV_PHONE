# Architecture / research
## Candidate engines
- https://github.com/ISEKHON/VirtualApp — Android 11-16 port.
- https://github.com/Black00Z/Blacks-BlackBox — Android 14+/16-oriented fork.
- https://github.com/gmh5225/Android-BlackBox — Apache-2.0 upstream, older baseline.

No virtual runtime has been imported or proven compatible. Audit source, license and build before selecting.

## Planned components
Virtual app installer and manager; guest Activity/PackageManager services; camera-intent router; URI grants and ActivityResult handling; sign-in compatibility tests.

Device identity: do not spoof identifiers or bypass integrity/device-binding. Some guest apps cannot sign in in a container.

## Milestones
M0 explicit GCam capture test harness.
M1 choose/build engine.
M2 launch original APK as guest and verify sign-in.
M3 intercept guest IMAGE_CAPTURE and round-trip content URI.
M4 real-device compatibility validation.