# Shared guest permission and device-location compatibility

The user's requirement is package-independent permission compatibility for
current and future guests. Fixes belong to shared Android service boundaries,
not application-specific allowlists. Contributor rules are in [AGENTS.md](../AGENTS.md).

## Audit of preceding changes

The permission broker and virtual PackageManager use each guest's requested
permissions, package/user policy and the real host grant ceiling. The modern
permission manager and AppOps adapters operate on Android API signatures.
Capture routing identifies the requesting guest/provider dynamically; native
file redirection follows runtime path rules. The Maps startup changes likewise
operate on network UID, Binder, manifest and provider boundaries.

Searching runtime_overlay and runtime_patch found no hardcoded Maps,
DuckDuckGo, Meitu or Timestamp package condition. This audit establishes shared
implementation scope, not universal physical compatibility certification.

## Location failure and changes

The host already had coarse, fine and background location grants. Android 16
changed listener/current/last-location signatures, while the old runtime still
passed guest package names to the system alongside the host UID. A standalone
fixture received real GPS callbacks, confirming device location was available.

GuestLocationHooks handles modern (API 31+) location methods by argument role:
provider/request/callback/listener identity remain intact; only the caller package
and host-invalid attribution tag are translated. Permission checks use the
current guest and virtual user before delegation. GNSS callbacks require fine
permission. Real Android service results, errors and callbacks remain authoritative.
ContextFixer supplies the host UID for system attribution and retains downstream
attribution identities instead of rewriting the entire chain.

Development build 76 received live non-mock GPS in both disposable guests:
com.phantom.locationprobe (1.7m accuracy, 67ms age) and com.phantom.renamedprobe
(1.3m accuracy, 65ms age). Network callbacks also arrived. Maps registered a GPS
listener successfully but its fused location remained stale; this was not a full
Maps location pass.

Google's fused service also hit a legacy GhostWifiScanner AbstractMethodError and
privileged Android-user calls. The subsequent shared changes use the real modern
Wi-Fi scanner Binder, dynamically translate its package arguments, and check guest
Wi-Fi/scan policy. Platform authorization failure callbacks are preserved. Virtual
current-user queries return the runtime slot, and already virtualized broadcasts
are delivered only in the host Android user, retaining the virtual slot in the
intent. No privileged cross-user or scanning grant is fabricated.

Final physical validation used development build 79 (0.4.9-dev), source
ca814ad3e96fa0fd5709b5e91996632d325d8c52, on the connected Infinix X6856,
Android 16 / SDK 36. Both disposable guests received fresh non-mock GPS:
com.phantom.locationprobe: 2.6m accuracy, 73ms age;
com.phantom.renamedprobe: 2.1m accuracy, 76ms age.
Maps ran under the host UID and its current-location detail opened without the
previous stale-location warning. It still displayed a low-accuracy warning;
this does not establish precise Maps fused location or background navigation.
No matching AndroidRuntime errors appeared in the scoped test log.
External-camera output returned RESULT_OK with 333,080 bytes to the renamed
guest. The probe removes location listeners on pause so subsequent camera result
status remains visible. The original Pict2Cam selection was restored and only
the two disposable fixtures were removed; the original ten visible guests and
their supporting guest packages remain.

Installed arm64 APK SHA-256:
267c0f5ae7e22945a279b20282ad3cdd4b6e0a555a0186e25867b49723319d2d.
Runtime build 38051218659 and all five source CI workflows passed. Universal
APK compilation passed; only arm64 was physically tested. Private location
screenshots and exact coordinates are not included in this repository.

## Contracts and scope

Production Java location and Wi-Fi hooks are compiled in contract harnesses with
two unrelated arbitrary package names and users 0 and 7. Cases cover guest denial,
unchanged listener/request/result identity, argument roles and Android errors.
The scanner test also verifies selection of the platform Binder and retention of
failure callbacks. Existing connectivity, permission and diagnostics tests remain.

Shared implementation cannot prove every guest version, OEM or Android release.
These changes do not certify all Google services, background navigation, or a
complete migration of every service's per-guest permission enforcement. In
particular, a coarse-only guest is deliberately blocked when the host has fine
permission, rather than receiving precise callbacks through the host identity.
A precision-filtered coarse-only callback bridge is not implemented. The
contract covers this denial; physical tests have both coarse and fine grants.
Modern device-location hooks serve device GPS; modern synthetic-location behavior
is not part of the validated scenario.

No user guest data is cleared. Disposable fixtures are removed after testing.
No release is published automatically.

## Primary Android references

- [Android 16 location Binder interface](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android16-release/location/java/android/location/ILocationManager.aidl)
- [Android 16 Wi-Fi scanner Binder interface](https://android.googlesource.com/platform/packages/modules/Wifi/+/refs/heads/android16-release/framework/java/android/net/wifi/IWifiScanner.aidl)
- [Android Wi-Fi scanning permission and failure callbacks](https://android.googlesource.com/platform/packages/modules/Wifi/+/refs/heads/android16-release/service/java/com/android/server/wifi/scanner/WifiScanningServiceImpl.java)

Sources were read for API diagnosis; no upstream implementation was copied.
