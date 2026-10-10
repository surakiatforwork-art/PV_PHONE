# PHANToM VPhone v0.4.5

Signed maintenance release for the guest permission and camera-intent workflows.
Validated on Infinix X6856, Android 16 (API 36), on 10 October 2026.
**Browser/GMS compatibility remains experimental; this is not an all-app compatibility guarantee.**

## Changes
- Preserve virtual Deny policies with atomic file replacement and report failed writes.
- Inspect the launching virtual user's manifest instead of always inspecting user 0.
- Request foreground location first; background access uses a separate Android Settings step.
- Respect virtual Deny during dangerous-permission requests.
- Hook modern PermissionManager and device-aware self-permission checks.
- List capture-capable camera providers; unsupported actions use Android's capture handler.
- Bridge a guest-owned content URI through a private, grantable host provider so EXTRA_OUTPUT can receive the full-resolution image.
- Route modern feature broadcasts through VirtualApp and reject unresolved explicit guest activity targets instead of opening their normal host installation.
- Require android.permission.DUMP for the ADB control receiver.
- Fetch VirtualApp at pinned commit b3c634ad7941765df3da84a207aca94b7861afae.
- Build non-debuggable release APKs with the existing signing certificate (versionCode 64).

## Device validation
- Updating the existing 0.4.3-alpha installation succeeded without uninstalling it or clearing guest data.
- arm64: CAMERA package/self checks both granted with Default; both denied with virtual Deny while host CAMERA stayed granted; resetting restored both grants.
- arm64: Transsion camera returned RESULT_OK with a bitmap thumbnail; full-resolution EXTRA_OUTPUT returned RESULT_OK with 3,280,717 bytes; cancellation returned RESULT_CANCELED.
- universal: installed successfully on the same arm64 device; package/self permission checks granted and full-resolution capture returned RESULT_OK with 3,285,227 bytes.
- Permission policy rollback, invalid mode rejection and virtual user isolation passed the JVM regression harness. Physical multi-user testing and other ABIs were not performed.
- Both APK signatures verified and have the same certificate. All four CI workflows passed for build source efcf423b26e93d6d86df3c50f6a4b4a9f532d2a8.

## Known limitations
- DuckDuckGo still fails an upstream notification-channel package check on this Android 16 device. It is not a supported app for this release.
- Chrome's onboarding can open, but isolated browser navigation is not certified. Google Play/GMS sign-in is not certified and remains experimental.
- The installed third-party provider com.meitu.meiyancamera crashed during image completion. Use the tested Transsion camera on this device.
- Intent routing does not redirect embedded Camera2/CameraX/getUserMedia camera sessions.
- Guests share the host Linux UID. Virtual Deny controls hooked permission queries; it is not a separate Linux security boundary.

## Downloads
Use the arm64 APK for this Infinix device. The universal APK includes additional ABIs, which have not been tested here.
Install as an update; do not uninstall first if guest data should be retained.
SHA256SUMS.txt accompanies the assets. The signing certificate SHA-256 is:
`4b0b9d2a4a1f290129e4e5e83c334e4066613782a62756ce8f0ea85f0c274b01`.

Build evidence: https://github.com/surakiatforwork-art/PV_PHONE/actions/runs/38031444441

## Earlier releases
The initial v0.1.0-alpha root module was a camera/import test harness. Current runtime releases are produced by the patched VirtualApp CI workflow, not that root app module.
