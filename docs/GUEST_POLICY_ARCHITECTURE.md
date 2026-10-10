# Guest policy architecture

Schema v1 is a read-only normalization facade over existing configuration. It does not override GuestPermissionPolicy, CameraProvider routing, VirtualStorageManager or GMS behavior.

GuestKey = packageName + virtualUserId. Immutable GuestPolicy contains schemaVersion, source provenance, permissions, providers, location, storage, packageVisibility, clipboard, notifications, gms and network. Desired/configured policy and observed effective policy are separate fields. Unknown/unimplemented fields remain null, not optimistic defaults.

Legacy input is an explicitly collected export: package/user, permission integer modes (0 inherit, 1 allow, 2 deny), global camera package, per-user storage redirect flag, configured location mode and GMS installed-package inventory. Adapter validates package/user and copies input so later mutation cannot change the snapshot. ALLOW means bounded by host grant. Camera is explicitly inherit-global; this phase does NOT create per-guest cameras. MODE_CLOSE means configured device passthrough, not proof of a recent location fix. GMS installed means imported, not working authentication. Visibility policy is isolated as configured, not a security boundary claim. Clipboard/network/notification preferences without a store are unknown.

Snapshots must distinguish missing/corrupt legacy export from valid DEFAULT. No first-read migration writes. Android adapter planned next: read public GuestPermissionPolicy APIs for requested permissions, host camera file/preferences, VirtualStorageManager package/user state and existing launch policy; do not touch private implementation fields or directly rewrite their files.

Versioning: reject unknown future schemas; explicit pure migration functions vN→vN+1 preserve GuestKey, unknown fields and provenance. Persist a new unified store only after per-subsystem dual-read comparison and deterministic rollback. Atomic replacement, schema/generation validation, cross-process ordering and user-ID scoping are mandatory before enforcement. Runtime consumers later receive immutable snapshots via existing service boundaries; no JNI/native policy engine is added now.

Integrate permissions first only if equivalence tests prove deny/allow/default, host-grant ceiling, failed persistence and separate users. Then provider/storage/location one at a time. Camera capture URI/results, device location provenance, host visibility exclusions and experimental GMS remain regression gates. Reverting the adapter cannot change existing runtime configuration.
