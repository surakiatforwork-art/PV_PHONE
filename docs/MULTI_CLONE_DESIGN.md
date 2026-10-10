# Multiple clone design

Reuse VirtualApp virtual users and installed-user inventory. A clone identity is immutable packageName + virtualUserId; displayName never determines a launch or destructive target. Current AppRepository/MultiplePackageAppData already expose multiple users, while NewHomeActivity primarily handles PackageAppData/user0. Do not claim multi-clone management complete.

Clone metadata schema v1: GuestKey, optional displayName/badge, favorite, hidden, createdAt, metadata generation and install-generation/archive identity. Metadata is separate from APK/data ownership. Offline models in this iteration neither allocate users nor launch/clear/delete apps.

First APK integration: adapt both PackageAppData and MultiplePackageAppData into a slot-aware UI model; existing slots remain visible. Clone Again allocates through VUserManager under a serialized host operation, invokes installPackageAsUser, and commits metadata only after verified success. Reconcile interrupted operations with installed-users inventory; never delete an existing user as automatic cleanup. ID allocation cannot use a stale count. Reuse only an explicitly empty safe slot, not a slot with another package's data.

Rename updates metadata only; badge uses stable slot identity. Start all actions with the same GuestKey. Tests need independent preferences/files/permissions for identical APK in users0/1, verified kernel vs virtual UID, correct long-click management, failed clone rollback and reboot persistence. Storage/shared resources can remain intentionally shared and must not be mislabeled private.

Future actions: Favorite changes ordering, Hide affects list visibility (not security), Stop calls scoped process stop, Clear data requires the exact clone's reviewable confirmation, Remove uninstalls as user, Create shortcut must create an actual pinned shortcut containing GuestKey and handle launcher refusal. Current success-only toast is an audit gap. Do not add PIN/private space in the first iteration.

Metadata migration preserves existing user IDs and labels. Removing metadata must never clear guest data; restoring previous UI must keep installed slots intact. Backend slot limits are measured, not copied from Dual Space's commercial client limits.
