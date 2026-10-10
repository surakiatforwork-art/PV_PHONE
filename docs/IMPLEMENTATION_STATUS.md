# First architecture/diagnostics iteration

Baseline APK and latest published release remain v0.4.8. VirtualApp engine, existing patches, host version/signing and connected device are unchanged by this iteration.

| Phase | Delivered | Deferred integration |
|---|---|---|
| A | Pinned source/license audit, current architecture map, 10 gaps, risk/rollback/roadmap | Component license re-review only when proposing actual source reuse |
| B | Six requested architecture documents | Detailed Android collector protocol after fixture implementation |
| C initial | Original Python evidence/capability/matrix models, immutable legacy policy adapter, clone metadata, offline HTML surface, contract tests and CI | Android Settings wiring, live adapters/collectors, persistent clone registry |
| D | Planned acceptance/regression gates | Runtime probes, JNI/raw syscall guest module, slot-aware home/Clone Again/Rename, enforcement migration |

Implementation is intentionally outside the shipped Android build. It is a contract foundation and diagnostic report renderer, not a claim that Settings → Engine Diagnostics or Clone Again is already implemented. Every sample subsystem remains UNKNOWN; historical physical release results are preserved separately.

Validated cases: compiler/legacy/contract passes cannot activate capabilities; all declared requirements required; SDK/ABI/build/device/app version/user mismatch rejected; latest failure supersedes older pass and matrix cannot cherry-pick it; stale/future/ambiguous evidence; runtime artifact/timezone requirement; per-user policy snapshots independent, host-grant ceiling retained, input and snapshot immutable; clone rename preserves identity; future schema rejected; HTML escaped; failed report save preserves old artifact. Existing JVM permission policy regression also passes.

Next bounded integration is a read-only host collector and Settings surface using these contracts, with actual exported build/device scope. After that, implement slot-aware launch/rename/badges before any clear/remove actions. No engine replacement or new native framework is proposed. No release is authorized for this architecture iteration.

## Second bounded integration

Settings → Engine Diagnostics is now wired into the signed **0.4.9-dev** build
(code 68), with a private activity and read-only host observations. All 23 guest
capabilities remain UNKNOWN / NOT_TESTED, independent of host grant/configuration
values. The original engine and compatibility patches remain in place. This
integration does not yet consume the Python reducer or collect guest evidence.
See [Android integration](ANDROID_DIAGNOSTICS_INTEGRATION.md) for boundaries.
The stable published release remains v0.4.8; no new release is created.
