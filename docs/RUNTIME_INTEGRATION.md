# PHANToM VPhone runtime integration — Android 15

Status: ENGINE EVALUATION; not integrated. No claim of virtual installation or guest launch.

## Candidate
ISEKHON/VirtualApp, Apache-2.0, downloaded as source ZIP into runtime_research/VirtualApp-main (no Git). Repository: https://github.com/ISEKHON/VirtualApp

## Validation gates
1. Build the upstream app and lib on JDK 17 / Android SDK.
2. Install the upstream test APK on Infinix Note 50 Pro Plus Android 15, check process and SELinux errors.
3. Import a simple unmodified test APK, install via VirtualCore, launch and restart it.
4. Verify split APK support, host app visibility, and guest user data isolation.
5. Embed runtime into PHANToM VPhone only after gate 3 passes; migrate existing imported_packages into runtime install requests.
6. Add camera intent redirection after guest launch is stable.

## Constraints
- No root, no modifying or re-signing guest APK.
- No identity spoofing or bypassing integrity/attestation.
- Android 15 hidden API and native hooks are not guaranteed on Transsion ROM.
- Existing v0.3.0 imported APKs are storage copies, NOT installed virtual apps.
- Never expose a Launch button unless engine reports installed state and launch intent.
- Keep runtime candidate separate from shipped APK until validated.
