# Guest compatibility work

System Binder calls require the real host identity. Calls staying in the virtual runtime require the guest package and base virtual UID exposed by NativeEngine. Treating both boundaries identically breaks modern Android ownership checks.

NotificationChannelHooks translates public channel/group calls and filters returned lists by guest/user namespace. Parcel copies retain channel settings and original guest objects. Normal and foreground-service posting translate the notification channel ID. Privileged administration and old-channel migration are outside this change.

LocaleManagerStub stores the guest's own application locale state in guest storage and rejects cross-package access. It does not change the host language, emulate all LocaleConfig methods, or promise automatic activity recreation.

GetContentProvider wraps virtual providers with InternalProviderHook. ProviderHook copies and translates the attribution root while retaining tokens and downstream identities; external calls retain the real host identity. Paired probes verify cross-guest queries in both directions.

The feature-aware PendingIntent proxy now uses its configured intent-array index. The old hard-coded position skipped redirection when the feature argument was inserted. Transition probes compare receiving UID and the resumed host stub activity.

PackageParserEx reads authentic signing metadata through Android's APK archive parser. Older cached packages are repaired from their installed/registered APK, checking package and version. No substitute certificate or signature/system privilege is supplied. The virtual installer has no reboot-staged/APEX backend.

Remaining Chrome work is native child/GPU service compatibility. Google Play still expects device-policy/system privileges. Neither passed normal guest usage on the tested device.

Build paired disposable probes with:
`python tests/android/build_probe.py OUTPUT_DIR`
and
`python tests/android/build_probe.py OUTPUT_DIR_OTHER com.phantom.releaseprobeother`.

Install both as guests. Test channels in both, delete one while checking the other's channel, locale roundtrip, two-way provider queries, signer comparison, and direct/pending activity transitions. Clean channels before removing both probes. Preserve existing user guest data.


### Camera provider discovery correction (0.4.7)

Android 11+ restricts implicit capture intent discovery to preinstalled system cameras. PHANToM now probes installed packages explicitly for photo/video capture support, matching the package-specific routing intent. This makes third-party handlers such as Timestamp Camera and LINE Camera discoverable. A provider that cannot capture a photo no longer silently opens the system camera from Test Camera. Device validation on Android 16: picker shows GCam mod `com.meitu.meiyancamera`, Timestamp Camera and LINE Camera. Guest capture launches the selected GCam mod. Timestamp Camera returns RESULT_OK with 300,055 bytes in the guest output URI. The installed GCam mod crashes during confirmation with missing image data; the same crash occurs when launched directly outside PHANToM.

Reference: https://developer.android.com/about/versions/11/behavior-changes-11#media-capture


### Shared device files (0.4.8)

Storage Redirect off (the default) now leaves the shared storage root accessible, including custom folders, Download, Documents and media folders. Android/data and Android/obb continue to use virtual app storage. Turning Storage Redirect on explicitly retains the virtual SD-card behavior; restart the guest after changing the setting. PHANToM requires Android All files access for broad shared-file access; the existing launch permission broker requests this for guests declaring broad storage permissions. Android still restricts other applications' private directories; this change does not grant root access.

The physical probe verifies five shared-file paths and directory enumeration without reading user file contents. Modern libc short-wrapper hooks are guarded against overwriting adjacent functions, including renameat/rmdir. Android 16 device validation passed on both arm64 and universal APKs: All files access agrees with the host grant; five shared-file paths, root listing and mkdir/write/rename/delete/rmdir passed. File Manager+ 3.8.3 opened within PHANToM, browsed device shared storage and displayed the contents of the test text file. Modern raw/device-aware and cached AppOps checks use the actual host permission mode. MediaStore category counts, removable storage and cloud providers are not covered by this validation.
