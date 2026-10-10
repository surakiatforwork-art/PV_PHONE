# Runtime integration

Runtime: ISEKHON/VirtualApp, pinned to b3c634ad7941765df3da84a207aca94b7861afae.
Source: https://github.com/ISEKHON/VirtualApp/tree/b3c634ad7941765df3da84a207aca94b7861afae

Release 0.4.6 is built from the pinned source plus runtime_patch and runtime_overlay.
The release workflow uses JDK 17, NDK 21.4.7075529, the upstream Gradle wrapper,
and the existing PHANTOM_ALPHA signing secrets. It produces signed release variants.
The root `app` module remains a separate legacy scaffold.

Confirmed gates: signed build; update on Android 16; disposable guest installation
and launch; modern permission checks; virtual Deny/Default; thumbnail and output-URI
capture callbacks. Universal APK smoke testing was performed on the same arm64 device.

Not confirmed: physical multi-user execution; other devices/ABIs; browser isolation;
Google Play/GMS sign-in; full guest compatibility. See COMPATIBILITY.md.

Local regression: `python tests/test_permission_policy.py`.
Disposable device probe: `python tests/android/build_probe.py <outside-repo-output-dir>`.
The probe requires SDK platform android-34, build-tools 36.1.0, JDK 17 and a debug
keystore. Import/launch it as a guest and verify its permission/capture result text.
