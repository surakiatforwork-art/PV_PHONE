# Permission and Android service compatibility

The user requires permission compatibility fixes to apply to all guest apps,
including packages with arbitrary names, rather than selected applications.

- Fix the shared runtime service, identity boundary, permission broker or policy
  layer. Do not branch on Maps, GCam, browser names or a package allowlist to make
  Android permission behavior work. Package identity may be read dynamically for
  policy lookup, provider ownership and isolation.
- Scope Android API differences by actual method signature/SDK and argument role.
  Never replace every string or UID indiscriminately; preserve unrelated provider
  names, callback tokens, listener IDs and downstream attribution identities.
- Retain Android permission/AppOps enforcement and the host grant ceiling.
  Per-guest DENY and virtual-user isolation must not be overridden by a shared fix.
  Do not fabricate successful permission checks, location fixes or service results.
- Add meaningful contract tests using at least two unrelated guest package names,
  including an arbitrary renamed package, and virtual users when policy applies.
  Cover granted/denied permissions, unchanged unrelated arguments and real errors.
  Test shared changes against other affected guest flows on the connected device.
- Record the tested APK source/build/device and actual successful operations.
  A generic implementation does not prove every app or every Android version works.
- Keep current runtime patches, signing identity and guest data. Do not clear or
  uninstall user guests. Disposable test packages may be removed after testing.
- Do not automatically publish a release for these compatibility iterations.
