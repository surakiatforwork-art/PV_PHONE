# PHANToM VPhone: Isolation, Google Services and Location

## Guest package visibility

PHANToM removes the upstream hard-coded whitelist that exposed selected host-installed
third-party applications such as Facebook, WhatsApp, WeChat and QQ.

Guest package queries are intended to see:
- Virtual applications installed inside PHANToM.
- Android system applications/components required for platform compatibility.
- The PHANToM host where required by the virtualization framework.

Normal third-party applications installed only on the host are not exposed to Guest
package queries unless a future explicit bridge intentionally allows one.

External camera providers remain host-side integrations. Camera routing resolves the
selected provider through the host PackageManager and does not require that provider
to be generally visible to the Guest.

## Google Services

The legacy FakeGms path depended on an Xposed FakeGApps module and is not used by
PHANToM because embedded Xposed is disabled.

The Settings entry now uses an experimental host-import path:
- Detect Google Play services on the physical device.
- Import available Google service packages, GSF and Play Store into the virtual
  environment.
- Handle split APK installations by importing from the package directory.

This is compatibility support, not a guarantee that every Google API will work.
Modern Google services can depend on privileged permissions, device certification,
Play Integrity, hardware-backed Keystore, account infrastructure and other platform
services that a rootless virtual container cannot reproduce completely.

## Location

PHANToM enforces VirtualLocationManager.MODE_CLOSE when launching each Guest.

MODE_CLOSE means the synthetic VirtualApp location layer is disabled. Guest location
calls therefore pass through to Android's real LocationManager under the PHANToM host
process and use the device-provided location.

If Android itself supplies a test/mock location, PHANToM passes that location through
as supplied by Android. PHANToM does not falsify Developer Options state, remove mock
location provenance, change isFromMockProvider-style signals, or hide test-provider
state from applications.
