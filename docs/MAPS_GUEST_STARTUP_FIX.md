# Google Maps guest startup on Android 16

## Reproduced failures

Device: Infinix X6856, Android 16 / SDK 36, arm64, 4096-byte pages.
Maps guest: `com.google.android.apps.maps`, version 26.40.00.989295350,
versionCode 1068809342, virtual user 0.

Development build 71 returned to the host home after startup. Targeted logs
showed Cronet's `getNetworkInfoForUid` throwing a NETWORK_STACK permission
exception, followed by a fatal native termination. Another launch also aborted
in ART's concurrent GC stack walker while `NativeEngine.onGetCallingUid` was
being called from the replacement of Binder's CriticalNative entry point.

## Changes

- The connectivity proxy translates the current guest's full/base virtual UID
  to the actual host UID for three explicitly named network queries. Other UIDs,
  network arguments, block flags, results and framework exceptions are retained.
- On ART API 28+, leave Binder.getCallingUid's platform native entry point intact.
  The previous replacement called managed Java without a managed JNI transition.
  This guard deliberately does not emulate virtual Binder caller identities on
  these versions. No new privileged permissions or successful fake responses are
  introduced by the guard.
- Accept modern two-argument unbindFinished. ActivityThread invokes that form
  only when Service.onUnbind returns true; older three-argument behavior remains.
- Declare the normal DETECT_SCREEN_CAPTURE permission used by Android 14+
  screenshot observer registration. This observes screenshot events; it does
  not capture screen pixels or grant MediaProjection access.
- For internal providers on API 28+, use the actual Binder caller UID in the
  root attribution source while retaining the guest package and downstream
  attribution chain. The old virtual root UID no longer matches Binder after
  the CriticalNative guard. Older platforms keep their existing virtual UID.

## Validation

Development build 73 (source eb753ba), signed with the existing host certificate,
opened Maps to its main screen on the same device. Continued testing discovered
another main-thread crash: registerScreenCaptureObserver lacked the host manifest
permission. Consequently build 73 is **not** considered a passing fix.

The production connectivity proxy compiles and passes a Java contract harness:
own full/base UIDs mapped, other/negative/system UIDs unchanged, argument and
result identity preserved, block flags preserved, SecurityException propagated.
The permission policy regression and 13 diagnostics contracts pass.

Build 74 removed the screenshot permission failure, but exposed a provider
query crash: `Calling uid: 10196 doesn't match source uid: 10018`. It is also
**not** a passing fix. Build 75 adds the matching internal provider attribution
UID.

Development build 75, source `03792b8e300708b9b92362494e4a779f937ccc39`,
passes the observed Maps startup scenario on this device:

- Maps reaches its main screen and loads map tiles, place details and photos
  after searching for Bangkok. The process runs under the host UID 10196;
  it is the virtual guest, not the separately installed Maps app UID 10132.
- Killing only the Maps guest and reopening it reaches the main screen again.
- Targeted live logs show no Maps fatal exception, Cronet termination or ART
  stack-walk abort during this validation window. Google service worker errors
  remain outside this passing startup scenario.
- Internal provider regression: framework query succeeds with real Binder UID
  10196 and the requesting guest package `com.phantom.releaseprobe`. The fixture
  assertion was updated to require that explicit modern contract, rather than
  incorrectly requiring virtual Binder UID emulation.
- Timestamp Camera returns RESULT_OK for the disposable guest's external-cache
  output URI, high request code 1390560349, 349865 image bytes.
- Removed both disposable probe packages from the host and virtual environment.
  Restored the initial Pict2Cam selection. Original ten guest apps remain intact.

Runtime build/signing CI: run 38047495873, success for arm64 and universal APKs.
Android debug, runtime probe, permission and diagnostics workflows also pass.
Connectivity contract CI passes. Only arm64 was installed on a physical device.
APK SHA256: `dab4c1e05c2071a623d2d2af2a1437c2392b0f5f469465996a1cbc264732206a`.

## Scope and remaining Google service issues

Google Play Services 26.40.31 still reports separate errors in the old ghost Wi-Fi
scanner, cross-user broadcasts and location/Dynamite attribution. Fixing Maps
startup does not certify Google account login, fused location or navigation.
No guest data was cleared and no release was published.

## Platform references

- [Android Binder source](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android16-release/core/java/android/os/Binder.java)
- [Android ActivityThread source](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android16-release/core/java/android/app/ActivityThread.java)
- [JNI transition guidance](https://developer.android.com/ndk/guides/jni-tips)

References informed compatibility diagnosis; no implementation was copied.
