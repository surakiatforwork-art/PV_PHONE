# PHANToM VPhone v0.4.7

Fix camera provider selection for third-party cameras, including GCam mods with arbitrary package names.

- Discover capture handlers by explicitly querying every installed package. Android 11+ implicit queries otherwise return system cameras only.
- Show photo/video capture capability per provider. No hardcoded camera package allowlist.
- Test Camera reports unsupported photo capture instead of silently switching to the system camera.
- Signed release update, versionCode 66, preserving guest data and existing signer.

Device validation: Infinix X6856, Android 16/API 36, arm64.
- Picker lists GCam mod com.meitu.meiyancamera, Timestamp Camera and LINE Camera.
- Guest capture opens the selected GCam mod CaptureActivity.
- Timestamp Camera returns RESULT_OK and writes a 300,055-byte image to the guest output URI.
- The installed GCam mod crashes when confirming capture with "received an image, but it did not have any image data". The same failure occurs when launched directly outside PHANToM; this GCam build is not validated for successful capture on this device.
- Universal APK also passed Timestamp Camera capture on the arm64 device. Final installed APK is arm64; the selected provider is the GCam mod.
- Existing limitations for Chrome, Google Play services and embedded Camera2/CameraX previews remain; see v0.4.6 compatibility notes. Routing applies to camera capture intents.

Build source: e1ff1d3. GitHub Actions run 38036244917 passed.

# PHANToM VPhone v0.4.6

Signed maintenance release improving guest notification and identity handling.
Tested on Infinix X6856, Android 16 / API 36, arm64, on 10 October 2026.
**This is not an all-app compatibility guarantee. Chrome and Google Play remain unsupported for normal use on this tested device.**

## Changes
- Translate calling and target packages for modern notification-channel queries.
- Namespace channel/group IDs by guest package and virtual user; translate create/read/delete and normal/foreground-service posting without mutating guest objects.
- Store application locales in guest storage; a guest restart or activity recreation may be needed to apply a language change.
- Translate virtual provider callers to guest package/base UID and external callers to real host identity. Copy the attribution root without overwriting downstream identities.
- Route feature-aware PendingIntent activities using the correct intent-array index.
- Read actual APK signing certificates and SigningInfo through Android's archive parser; repair older cached metadata from the registered guest APK. Remove placeholder/signature-spoof fallbacks.
- Return an empty staged-session list: the virtual installer has no reboot-staged/APEX backend.
- Keep the existing signing certificate and non-debuggable release build; versionCode 65.

## Device validation
- Signed updates retained the existing guest packages and data.
- Two disposable guests used identical channel/group IDs: create, individual/list reads, delete, recreate and posting passed independently. Deleting one guest's group left the other's channel undeleted.
- Provider queries passed both ways with virtual UIDs 10015 / 10017 and correct caller packages.
- Guest locale set/get/restore passed with fr-FR and initially empty application locales.
- Direct and PendingIntent transitions retained the guest UID and resumed inside PHANToM's stub activity.
- SigningInfo SHA-256 matched the probe APK certificate checked with apksigner; staged-session query passed.
- Transsion camera returned RESULT_OK for a guest-owned EXTRA_OUTPUT URI with 3,309,262 bytes during arm64 validation.
- Final universal APK on the arm64 device passed signer/provider/PendingIntent checks and produced a new 3,316,722-byte output image after the probe deleted its previous test image.
- Both final APK signatures verified against the existing certificate; all four CI workflows passed for the build source below.
- DuckDuckGo passed onboarding and displayed an HTTPS page while the resumed activity stayed inside PHANToM.
- Permission-policy JVM tests passed rollback, virtual user isolation, invalid-mode and allow/deny/default checks.

## Known limitations
- Chrome passes the former locale permission failure but aborts with "GPU process isn't usable". A normal installed Chrome screen is not guest success.
- Google Play fails device-policy authorization. GMS login, purchases, downloads and certification are not validated. Correct certificates do not provide system privileges or attestation.
- DuckDuckGo validation covers launch/basic browsing, not all features or background reliability; JobScheduler warnings were observed.
- Dynamic LocaleConfig APIs, privileged notification administration, and old unnamespaced channel migration are not implemented.
- Notification IDs exceeding Android's 1000-character limit after namespacing fail explicitly instead of colliding.
- Camera2/CameraX/getUserMedia are not redirected by capture-intent routing. Use the tested com.transsion.camera provider.
- Guests share the host Linux UID; virtual package/permission policies do not provide a separate Linux boundary.
- Physical virtual-user-1, other devices/Android versions and additional ABIs remain untested.

## Downloads and provenance
Use arm64 on the tested Infinix. Install as an update without uninstalling PHANToM to retain guest data.
Universal includes additional untested ABIs. SHA256SUMS.txt accompanies release assets.
Signing certificate SHA-256: `4b0b9d2a4a1f290129e4e5e83c334e4066613782a62756ce8f0ea85f0c274b01`.

Build source: `14fca40bac152ae7945873e2d53badcae7e28a6b`.
Build evidence: https://github.com/surakiatforwork-art/PV_PHONE/actions/runs/38035133187
Upstream VirtualApp: `b3c634ad7941765df3da84a207aca94b7861afae`, with repository patches/overlays.

## Earlier releases
v0.4.5 added permission/capture fixes. The root app module remains an import/camera harness; released APKs come from the patched VirtualApp workflow.
