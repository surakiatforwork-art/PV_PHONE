# Runtime architecture

The root `app` is the original import/camera scaffold. The shipped runtime APK is
built by `.github/workflows/phantom-vphone-rc.yml`: a pinned VirtualApp checkout,
Android compatibility patches, PHANToM source/resource overlays and stable signing.

Guest launch and package queries use VirtualApp services. Real Android permissions
belong to the host UID. GuestPermissionPolicy stores per-package/per-user overrides;
PackageManager, modern PermissionManager and ActivityManager permission hooks read
those policies. Files are atomically replaced so a failed update preserves the old policy.

Capture intents are sent to the chosen real camera only when it handles the action.
A guest-owned output content URI is replaced with an unguessable host-provider URI.
Android grants that URI to the camera. CaptureBridgeProvider, after clearing the
external caller identity, forwards the open-file request to the original guest provider.
Routes expire after 24 hours; stale route metadata is removed on later captures.
ActivityResult callbacks remain attached to the guest activity.

Guest applications share a Linux UID with the host; this is not a strong independent
security sandbox. Camera2/CameraX interception, full browser/GMS compatibility and
integrity/device-binding bypass are not implemented by these PHANToM patches.

The runtime ADB receiver requires Android's privileged DUMP permission. Debugging
is disabled in release artifacts. Device results are recorded in COMPATIBILITY.md.
