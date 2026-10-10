# Optional Android profile backend — future design only

Virtual mode remains the current VirtualApp rootless container. Its fast integrated UI and compatibility hooks operate under host Linux UID and Android authority. Profile mode, if later available, would delegate install/process/storage/service identity to an Android-owned work/secondary profile. It is not required for v0.x and no device profile is created in this work.

Mirro's documented Samsung result is useful boundary evidence, not a generic public API guarantee. Public Android managed-profile provisioning, user consent, device/OEM limits, device-owner/profile-owner constraints and lifecycle need their own official-platform review before implementation. Do not assume any ordinary app can create arbitrary secondary users.

Future backend facade: enumerate capabilities, provision with explicit OS flow, install/launch package, observe owned-profile status, stop/pause where allowed, remove with explicit data-impact confirmation. Return BLOCKED/NOT_APPLICABLE when Android authority is unavailable; never emulate a grant. GuestKey must include backend identity to avoid collision with VirtualApp virtualUserId.

Comparison: virtual mode offers quick cloning and host-mediated bridges but weaker isolation and native/GMS limits; profile mode offers real UID/process/storage authority at the cost of setup, OEM restrictions, badges and profile lifecycle. Neither guarantees attestation, banking/DRM/login eligibility.

Fallback is opt-in for a clearly documented unsupported virtual requirement. Never migrate accounts/files automatically between boundaries. A later decision needs reproducible launch/native/WebView/provider/GMS tests across supported OEMs, operational rollback, permission/data transfer review and user-visible explanation. Research only until then.
