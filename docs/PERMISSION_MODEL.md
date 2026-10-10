[Reading 65 lines from start (total: 65 lines, 0 remaining)]

# PHANToM VPhone Permission Model

## Host / guest model

Guest applications execute inside the PHANToM VPhone host UID. Android therefore
grants runtime permissions and app-ops to PHANToM VPhone, while VirtualApp mirrors
the resulting host permission state to guest applications.

PHANToM only asks for permissions that are present in the guest application's
requestedPermissions list.

## Runtime permissions

Before a guest is launched, GuestPermissionBroker inspects its manifest and asks
Android for any missing dangerous permissions declared by PHANToM VPhone.

The Android permission dialog is intentionally shown to the user. It will display
PHANToM VPhone as the requesting application because PHANToM owns the real process
and UID.

Modern permissions added to the host include Android 10-14 location, phone-number,
activity-recognition, media, Bluetooth and nearby-device permissions.

## Special access

Special permissions cannot be silently granted by an ordinary rootless application.
When a guest requests one of these capabilities, PHANToM opens the matching Android
Settings page for the PHANToM VPhone package:

- MANAGE_EXTERNAL_STORAGE -> All files access
- SYSTEM_ALERT_WINDOW -> Display over other apps
- WRITE_SETTINGS -> Modify system settings
- REQUEST_INSTALL_PACKAGES -> Install unknown apps
- SCHEDULE_EXACT_ALARM -> Alarms & reminders

The guest is launched even if the user declines; features requiring the denied
permission may not work.

## File managers

On Android 11+, broad filesystem managers should request MANAGE_EXTERNAL_STORAGE.
Once the user grants All files access to PHANToM VPhone, guest processes execute
under the same host UID and can use that host-level storage capability, subject to
VirtualApp IO redirection and Android/SELinux restrictions.

READ_EXTERNAL_STORAGE / WRITE_EXTERNAL_STORAGE alone are not equivalent to All files
access on modern Android.

## Limitations

PHANToM cannot grant signature, privileged, role-restricted or hardware-backed
permissions that Android does not allow a normal rootless application to receive.
Some permissions (SMS/call-log roles, device admin, accessibility, VPN, notification
listener, etc.) have separate Android role/service approval flows and may require
future adapters.

Permissions granted to PHANToM VPhone exist at the host UID level. They do not grant
anything to unrelated applications installed normally on the device.

## Xposed

The embedded VirtualApp Xposed runtime is disabled in PHANToM VPhone and the Xposed
controls are removed from Settings. This only affects the embedded runtime inside
PHANToM VPhone. It does not enable, disable, patch or modify Xposed/LSPosed/Magisk or
any normal application installed outside PHANToM VPhone.

[executed on device: PHANToM (60ef0451-feba-4882-8849-367db28e542d)]
## Per-guest permission settings

Each installed Guest has a Permissions page under Settings > App Manage > Guest menu > Permissions.
Every permission requested by that Guest can be set to:

- Default: inherit the real PHANToM host permission state.
- Allow: request/use the PHANToM host permission when Android permits it.
- Deny: force PERMISSION_DENIED for that Guest inside the Virtual Package Manager even when the host has the permission.

Policies are persisted per package and virtual user in the PHANToM private sandbox. The policy store is file-backed so virtual processes and the package-manager service see updates across processes.

On Android 11+, legacy READ_EXTERNAL_STORAGE / WRITE_EXTERNAL_STORAGE requests are bridged to PHANToM's All files access when granted, improving compatibility with older file-manager apps.

Because all Guests still execute under the PHANToM host UID, this virtual Deny layer is API-level policy rather than a separate Linux UID sandbox. Direct kernel-level access available to the host cannot be made fully independent per Guest without a stronger isolation architecture.
