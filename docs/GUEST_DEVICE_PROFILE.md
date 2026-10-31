# Guest device metadata profile

**Experimental metadata feature. Garena 1.6.57 is not playable in the tested
VPhone guest runtime; its native process still terminates. High-FPS settings and
144 FPS gameplay have not been validated.**

The supplied `CALL OF DUTY MOBILE UNLOCK [144FPS] v1.4.4.zip` contains a Magisk
installer and fourteen property assignments. It identifies the device as iQOO 13,
model V2408A, product PD2408. Archive SHA-256:
`26ce544d2a54a91e4fe0c1c8984d8c91f8641bf387c4ea9c3cbd8d24e92af4b8`.
Its shell scripts were inspected, not executed or installed. No third-party
installer or executable is bundled into VPhone.

## Use

Add an installed game through VPhone's Add app flow. In that guest's options menu,
choose **Device profile**, then **iQOO 13 (V2408A, experimental)**. Saving stops that guest so its
next launch reads the profile before guest providers and Application initialize.
Select **Device default** to remove the profile and restart with real metadata.
Profiles default off and are keyed by package and virtual user. The current home
menu exposes slot 0; the persistence/runtime layer also isolates other slot IDs.
This feature is separate from permission policy and changes no permission grants.

The runtime updates Build.MODEL, BRAND, MANUFACTURER, PRODUCT and DEVICE only in
the opted-in guest process. Native wrappers map exactly the fourteen property
names from system.prop when reading through supported libc property entry points.
They preserve unrelated values, original callback cookies and property serials.
No global property writes, Magisk installation, ABI, Android SDK, fingerprint,
graphics capability, integrity result or display refresh-rate changes occur.

The native hook coverage log is a bitmask: 1=property_get, 2=property_read,
4=property_read_callback. Missing symbols or the existing short-function safety
guard can leave a bit unset; this must not be reported as complete native coverage.
The guard is retained. Cached values created before guest binding, direct property
area access, missing property handles, external subprocesses and hardware/system
service queries are outside this interception. A metadata profile alone cannot
guarantee a game's high-FPS option or delivered frame rate.

## Validation

Production Java contracts cover two arbitrary package names and users 0 and 7,
opt-in/default behavior, rollback, unknown/corrupt settings and unchanged SDK and
fingerprint. Production C++ wrappers are tested against all fourteen assignments,
unknown-property passthrough, callback identity/serial and partial hook coverage.
Physical build 80 (source 39b78eb77891ed5a5b473bdea3a55b05301db2db)
passed profile opt-in in both disposable packages com.phantom.profileprobe and
com.phantom.otherprofile. They reported V2408A/iQOO/PD2408, and Java
SystemProperties.get("ro.product.model") reported V2408A. The unselected guest
retained Infinix X6856; selecting Device default and restarting restored the first
guest's original model. Android SDK 36 and arm64-v8a remained unchanged, as did
the real device's model queried from outside VPhone.

Observed native coverage was **6**, not 7: property_read and
property_read_callback installed, but property_get was refused by the existing
safety guard. The device lacks the vendor.marketname property; Java handle lookup
still returned an empty value. This is partial module equivalence, not complete
property virtualization. No 144 FPS result is claimed.

Garena cloned with all eight splits, but initially crashed in Play Games'
initialize-shortcuts thread: modern IShortcutService.getShortcuts lacked caller
translation. The additional shared query boundary validates the guest caller,
translates only package/real Android user, preserves match flags and platform
errors, and filters results by existing guest route package and virtual slot.
Contract tests cover two arbitrary packages, slots 0/7, unrelated/host/malformed
routes, denied Android calls and null results. It does not migrate the entire
legacy shortcut mutation API. Build 81 (source
0d3755d20cba614dca7f03b053f7876081b97889) no longer produced the shortcut
exception during startup, but the game still terminated with signal 7 after
Unity reported a missing split_AssetPackConfig.apk. The installer had renamed
that asset to AssetPackConfig.apk, breaking filename-based asset-pack resolution.
The shared installer now retains source split basenames; the development update
command parses base and splits together rather than attempting individual split
installs. Updating the cloned game preserves its guest data.

Build 82 removed the missing-split report but still terminated natively with the
profile enabled and disabled. Build 83 (source
57a90a5857aa685ddc50c413e22567274e2fece3) also eliminated writable-DEX warnings:
private base/split copies are made read-only before writing through an open FD,
and the installer no longer makes base.apk writable afterward. Production file
tests verify byte preservation, read-only modes on Linux, updates and source/data
protection. This follows Android's dynamic-code loading requirement.

The build 83 default-profile launch still exited with signal 7. Unity reported
UnauthorizedAccessException while listing its external-cache Log directory. The
shared native directory wrapper now applies the same path/deny rules as file
opening, delegates to real opendir and preserves DIR handles and errno. Its native
contract covers two unrelated virtual paths, real directory listing, FD_CLOEXEC,
relative paths, missing paths and forbidden paths without platform delegation.
Build 84 installed the directory hook and a disposable guest passed actual
create/write/list/read/delete operations at its virtual external-cache path.
The tested game no longer logged those directory, split, DEX or shortcut errors,
but still exited natively. This is not a game compatibility pass.

A temporary debuggable build allowed scoped inspection of the freshly-created
guest's crash-report files and native signals. The crash database had no native
crash records. Debugger observations included handled ART/JIT signals and
userfaultfd/GC SIGBUS events; these are not sufficient to identify the final
fatal stack. A native SDK changed SIGBUS dispatch during the instrumented run,
but this does not establish its intent or prove an anti-cheat cause.

Build 85 tested the host with native heap pointer tagging disabled, matching the
real game's application flag. It did not resolve startup with either profile.
That experiment is reverted in build 86: the host's original pointer-tagging
safety mode is retained. The temporary debug tools and ADB forward were removed;
the final installed APK is non-debuggable.

Final device validation used **0.4.9-dev code 86**, source
bc2a86aba0719178cebccd6c5b1e4a6295f115aa, runtime CI 41185188279. The fixture
reported V2408A/iQOO/PD2408 through Build fields and the modern model property
query, retaining SDK 36 / arm64-v8a. Its real virtual-cache directory test passed
create/write/list/read/delete. Garena with the selected iQOO profile still exited
with signal 11. **The gameplay goal remains incomplete.**

Installed arm64 APK SHA-256:
`63e8d76b192f71a14f3b58635514a6ebb72dcd8518734dd36e332533c4b488ee`.
Signing certificate SHA-256 remains
`4b0b9d2a4a1f290129e4e5e83c334e4066613782a62756ce8f0ea85f0c274b01`.
Normal and universal runtime APK compilation passed; only arm64 was physically
tested. Device flags confirm non-debuggable operation and restoration of the
original pointer-tagging mode. All disposable probes and their profile settings
were removed; the Garena guest remains with its experimental iQOO profile enabled.
Private logs, raw crash databases, debugger transcripts and screenshots remain
outside the repository. No release was published.

The real installed Garena game, outside VPhone, remained alive and reached its
login screen. No account was signed in or created during this test.

The physical display advertises real 60/120/144 Hz modes; this is hardware display
capability only, not an observed game FPS or proof of an unlocked game setting.

The selected target is Garena `com.garena.game.codm`, installed version 1.6.57
(28028), arm64, with a base APK and eight split APKs on the connected Android 16
Infinix X6856. VPhone was absent on this device at the start of this iteration.
The installed game and its data must remain intact; adding a guest creates its
own data directory and does not transfer the game's existing login or downloads.
No release is published automatically.

Property API signatures and callback semantics were checked against the
[Android Bionic property interface](https://android.googlesource.com/platform/bionic/+/refs/heads/main/libc/include/sys/system_properties.h).
See also [Android read-only dynamic code guidance](https://developer.android.com/about/versions/14/behavior-changes-14#safer-dynamic-code-loading).
[ART signal handling](https://android.googlesource.com/platform/art/+/master/runtime/fault_handler.cc)
describes its special SIGBUS handler for userfaultfd collection. The
[native pointer tagging documentation](https://source.android.com/docs/security/test/tagged-pointers)
describes the opt-out tested and subsequently reverted here.
