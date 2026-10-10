# Reference architecture gap analysis — PHANToM VPhone

Audit date: 2026-10-10 (Asia/Bangkok). Baseline: PV_PHONE `6c16b92`, release v0.4.8; shipped VirtualApp source pinned to `b3c634ad7941765df3da84a207aca94b7861afae`. The workspace root is the older scaffold; `.codex-review/PV_PHONE` is the existing Git checkout used for GitHub remote delivery. No new repository or engine is created.

## Reference and license audit

- [PrismSpace](https://github.com/mhmdwaelanwr/PrismSpace/blob/990a414f4e0eea68ef50230cb09c3b02ec6d700b/LICENSE): 990a414f4e0eea68ef50230cb09c3b02ec6d700b; Apache-2.0
- [mirro-android-virtualization](https://github.com/obadadallo95/mirro-android-virtualization/blob/74e6a1e3ea1898b2e2a705d7c8b3e730059023b1/LICENSE): 74e6a1e3ea1898b2e2a705d7c8b3e730059023b1; Apache-2.0
- [Dual-Space-App](https://github.com/mansoorulhaq166/Dual-Space-App/blob/12e4a48b80fafd73869b97f2e6737c30c5c3a4a8/LICENSE): 12e4a48b80fafd73869b97f2e6737c30c5c3a4a8; Apache-2.0
- [android-clone-detection](https://github.com/eshginfarzali/android-clone-detection/blob/23145b7b9e81b911c8f2d7a5d00f8ac1a72c3fa9/LICENSE): 23145b7b9e81b911c8f2d7a5d00f8ac1a72c3fa9; MIT
- [app-clone-parity-engineering](https://github.com/buithanhninh/app-clone-parity-engineering/blob/9d51c15bf573c8b2547d30f923d2d5360a9f623e/LICENSE): 9d51c15bf573c8b2547d30f923d2d5360a9f623e; MIT

PrismSpace's repository license is Apache-2.0, but CREDITS identifies NewBlackbox/BlackBox/VirtualApp ancestry and component-specific terms. Mirro-owned code is Apache-2.0 and its third-party review separately tracks upstream concerns. Dual Space's Apache-2.0 client does not include the proprietary DualCore runtime. Both the diagnostic reference and parity-engineering reference have MIT files at the audited commits (the latter is an evidence workflow, not an Android engine). No external implementation, native dependency or binary is copied in this change. New tooling is independently authored from the requested contracts. A future reuse decision requires exact file/header/origin/NOTICE review; repository metadata alone is insufficient.

## Findings and useful concepts

1. **PrismSpace:** record observed capability independently of injector registration; publish coherent policy snapshots. PHANToM should strengthen this with test IDs, exact build/device scope and rejection of evidence-free ACTIVE. Its native dependencies and identity-changing Binder policies are excluded.
2. **Mirro:** map Java identity separately from kernel/Binder authority; record loader and service failures with their boundary. Its profile proof is an OEM-specific research result, not a portable rootless API. Preserve VirtualApp and avoid repeating a facade-only loader architecture.
3. **Dual Space:** package-plus-slot keys, clone badges, explicit rename and per-instance management. Its UI is a concept reference; neither commercial SDK nor its fixed copy limits is imported.
4. **Diagnostic reference:** compare observable Java/libc/kernel facts for debugging. A discrepancy is DIFFERENT/DEGRADED evidence, never a trigger to conceal virtualization.
5. **Parity engineering:** define a bounded requirement/test/evidence chain and freeze the target build. Apply this to compatibility, not to copying another product's backend.

## Existing baseline by area

| Area | Status | Source and practical limit |
|---|---|---|
| VirtualApp process/service core | Already implemented | Pinned runtime; overlays are the shipped app, root app is a scaffold. Shared host Linux UID remains a weak isolation boundary. |
| Virtual users | Partially implemented | AppRepository lists installed users; MultiplePackageAppData has userId. NewHomeActivity click/long-click handles PackageAppData only and launches user 0. Nonzero clone management is incomplete. |
| Guest permissions | Already implemented | GuestPermissionPolicy keyed package/user, atomic files; broker, PM/PermissionManager/AM hooks. ALLOW remains limited by Android's host grant. |
| Package visibility | Already implemented with bounded claims | BaseVirtualInitializer removes host-third-party whitelist; physical cross-provider tests exist. Not a proof against every native/system observation. |
| Storage | Partially implemented | v0.4.8 honors redirect off, preserves private redirect rules, fixes modern AppOps. Shared-file and rmdir tests passed on one API36 arm64 device. Raw syscall/path equivalence untested. |
| Camera provider | Already implemented for capture intents | Explicit per-package discovery and CaptureBridge output grants; global selection. Embedded Camera2/CameraX is not routed. Installed GCam can launch but capture confirmation crashes independently. |
| Location | Partially implemented | LoadingActivity forces MODE_CLOSE, passing Android device location. No recent fix-quality/background location evidence. |
| GMS | Partially implemented, degraded | Experimental host import/splits; real signing metadata, no attestation bypass. Privileged service and lifecycle failures remain. |
| Activity/ATM/instrumentation/role | Partially implemented | Modern patches preserve virtual transitions; Chrome later fails in GPU. Hook presence is not independent ACTIVE evidence. |
| Native IO/linker/WebView | Partially implemented | IOUniformer hooks a selected libc set; modern short-hook guard. open/read coverage and raw syscalls need audit. hook_dlopen is disabled in the inspected source. No WebView renderer compatibility proof. |
| Notifications/locales/providers/PendingIntent | Partially implemented | v0.4.6 physical tests and disposable guest fixture; limited to tested versions/device, not all platform methods. |
| Diagnostics/evidence registry | Missing | Existing Markdown reports and APK fixture, no structured current-build capability registry. |
| Unified GuestPolicy | Missing | Several independent configs; camera is global, permissions/storage use package/user. |
| Clone metadata/rename/badges | Missing | No complete stable metadata facade or user-aware home management. Shortcut currently shows a success toast without creating a shortcut. |
| Profile mode | Future research | No production requirement; Android-owned boundary might help otherwise unsupported apps. |
| Engine swap/native framework/security bypass | Not needed | Does not solve this iteration's measurement and management gaps. |

## Ten prioritized gaps

| Rank | Gap | First safe action | Risk if integrated incorrectly |
|---|---|---|---|
| 1 | Hook existence mistaken for behavior | Evidence-gated capability contract | False compatibility claims |
| 2 | Results lack exact app/build/device scope | Versioned compatibility rows | Old passes applied to new APK |
| 3 | No structured diagnostic collection | Sanitized record schema and read-only report | User data/log disclosure |
| 4 | Nonzero users not consistently routed by Home | Stable GuestKey and clone inventory; design first | Cross-slot launch/clear/remove |
| 5 | Policy fragmentation | Read-only legacy adapter | Permission or storage semantics changed |
| 6 | Native view coverage unknown | Java/libc/raw syscall fixture plan | Native crash/data corruption |
| 7 | Browser/WebView milestones conflated | Separate launch, rendering, navigation/login | Host-app escape counted as success |
| 8 | GMS import conflated with usable APIs | Per-service failure and experimental label | Identity/security boundary crossed |
| 9 | Clone actions have weak UX/truth | Rename/badge metadata; truthful shortcut state | Misleading UI/destructive wrong-user action |
| 10 | No evidence retention/revalidation rule | Content hashes, latest-failure precedence, TTL | Stale ACTIVE remains after regression |

## Immediate, later, excluded

Immediately: architecture documents, original data models, contract tests, offline diagnostic report, compatibility/evidence validation, read-only legacy policy normalization and clone metadata. These artifacts are additive and do not enforce policy, allocate users or alter hooks.

Later: Settings → Engine Diagnostics, host collectors, independently measured subsystem probes; then slot-aware Clone Again/Rename/badge and per-subsystem enforcement with regression checks. Diagnostic Guest extends the existing fixture before any native framework decision. Profile backend stays a design option.

Excluded: engine replacement, copying DualCore, signature/attestation/security bypass, hiding clone observations, automatic PASS from CI, adding Dobby/xDL, PIN/private space and broad UI rewrite.

## Risk and rollback

Phase A/B and offline Phase C tooling have no APK behavior effect. Models reject unsupported schema versions and missing runtime evidence. Integration later raises risk: process/UID scope, cross-slot destructive actions, callback routing, storage grants and stale reports. Keep every existing runtime_patch/overlay intact. This iteration rolls back by reverting only docs/diagnostics tooling/tests/its CI workflow; v0.4.8 runtime and release remain unchanged.

## Phase A–D roadmap and acceptance

A: audit pinned references, licenses and current baseline; classify implemented/partial/missing/not-needed/future. Deliver this report before runtime-core changes.

B: define capability, GuestPolicy, compatibility matrix, diagnostic guest, clone and optional profile architecture. Acceptance: no unknown enforcement is presented as implemented; every status and destructive action has scope.

C (this first iteration): independently implement evidence/capability/policy/clone models, validate sample reports, render a read-only offline diagnostic surface, add contract tests and CI. Default every unmeasured subsystem to UNKNOWN. No core edits, device reconfiguration or new release.

D (subsequent incremental work): collect from running guests, connect Settings, then clone metadata/slot operations, then one policy subsystem at a time. Each needs device regression evidence for camera result URI, permission denial/user isolation, device location, package visibility, shared/private storage, notifications, providers/PendingIntent, GMS experimental state and Android16 lifecycle. Stop at unsupported Android authority boundaries rather than spoofing them.

## Pinned primary references

- [PrismSpace truth layer](https://github.com/mhmdwaelanwr/PrismSpace/blob/990a414f4e0eea68ef50230cb09c3b02ec6d700b/docs/ENGINE_TRUTH_LAYER.md)
- [PrismSpace policy assembly](https://github.com/mhmdwaelanwr/PrismSpace/blob/990a414f4e0eea68ef50230cb09c3b02ec6d700b/Pcore/src/main/java/com/prismspace/container/core/ClonePolicyBuilder.java)
- [PrismSpace origins](https://github.com/mhmdwaelanwr/PrismSpace/blob/990a414f4e0eea68ef50230cb09c3b02ec6d700b/CREDITS.md)
- [Mirro authority/native lessons](https://github.com/obadadallo95/mirro-android-virtualization/blob/74e6a1e3ea1898b2e2a705d7c8b3e730059023b1/docs/09_LESSONS_LEARNED.md)
- [Mirro scoped compatibility](https://github.com/obadadallo95/mirro-android-virtualization/blob/74e6a1e3ea1898b2e2a705d7c8b3e730059023b1/docs/COMPATIBILITY_MATRIX.md)
- [Dual Space clone UX](https://github.com/mansoorulhaq166/Dual-Space-App/blob/12e4a48b80fafd73869b97f2e6737c30c5c3a4a8/app/src/main/java/com/example/dual/space/ui/HomeScreen.kt)
- [Diagnostic view comparison](https://github.com/eshginfarzali/android-clone-detection/blob/23145b7b9e81b911c8f2d7a5d00f8ac1a72c3fa9/clone-guard/src/main/cpp/clone_guard.c)
- [Bounded evidence workflow](https://github.com/buithanhninh/app-clone-parity-engineering/blob/9d51c15bf573c8b2547d30f923d2d5360a9f623e/references/evidence-and-scope.md)
