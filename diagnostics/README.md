# Offline diagnostics foundation (Phase C, first iteration)

Original standard-library Python 3.11 tooling, separate from the APK. No copied source or new native framework. Models do not enforce policy, change system permissions, allocate users or collect device data. Settings UI, Android collectors, per-guest enforcement and runtime clone actions are future Phase D integrations described in docs.

Run from the repository root:

```sh
python -m unittest discover -s tests -p test_diagnostics_model.py -v
python -m diagnostics.report diagnostics/examples/unmeasured.json --html diagnostics-report.html
```

The HTML is a read-only diagnostic surface with expandable subsystem records; it is not the shipped Android Settings screen. The included report is an explicitly unmeasured example. All 23 subsystems are UNKNOWN. Synthetic contract tests are never device claims.

`model.py` provides State/Result, exact immutable Scope, Evidence, latest-trial requirement reduction, scoped matrix validation, GuestKey, read-only legacy policy normalization and CloneMetadata. `report.py` validates JSON and renders sanitized offline HTML. `reference_sources.json` pins the license/idea audit. No runtime_patch, runtime_overlay, release workflow, version/signing or installed device is changed in this iteration.

## Wire contract v1

`schema_version=1`; unknown future versions are rejected. Scope keys: sdk, android_release, abi, runtime_build, device_class; guest rows additionally require package_name, app_version_code and virtual_user_id. Evidence keys: evidence_id, requirement_id, test_id, kind (RUNTIME/BUILD/CONTRACT/LEGACY), result, scope, tested_at (ISO8601 with timezone), artifacts (sanitized references), observation. Artifact hashes should be part of captured references or the future artifact index; this phase checks references, not remote content authenticity.

Report requirements must include the full catalog; each lists unique, nonempty requirement IDs. Every ACTIVE is derived from fresh matching RUNTIME PASS for every declared requirement. Build, contract and historical evidence do not activate it. Default validity is 30 days; host/app/SDK/ABI/device/user mismatch invalidates reuse. The latest failure cannot be hidden by referencing only an earlier pass in a matrix row. Historical annotations are escaped text, never executable HTML or status inputs.

Legacy policy input uses explicit exports; the adapter never opens live runtime policy stores. Permission modes 0/1/2 become inherit/allow-with-host-grant/deny. Camera stays inherit-global, unknown preferences stay unknown. The model is a snapshot of configured intent, not an observed enforcement guarantee. Clone rename modifies display metadata, never GuestKey.

## Deliberate first-iteration limits

No host collector, JNI/syscall probe, real evidence importer, artifact hash verifier, registry authentication, Android Settings integration, profile backend or clone allocator is implemented. TTL/report models do not authenticate arbitrary manually written records. A subsequent collector must validate nonce/caller/guest scope and content hashes before publishing actual runtime claims. Source access/config intent is distinct from enforcement evidence.

Architecture and license rationale: [gap analysis](../docs/REFERENCE_ARCHITECTURE_GAP_ANALYSIS.md). Roll back only these additive files/workflow; runtime v0.4.8 remains intact. No release is created for this work.
