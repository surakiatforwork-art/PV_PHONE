# Compatibility matrix design

A row is package + app versionCode/versionName + signer/archive hash when available + host runtime build + Android SDK/release + ABI + device class + virtualUserId + feature + test ID. Two rows for different app/build/device scopes are different claims. Result enum: PASS, PARTIAL, FAIL, BLOCKED, NOT_TESTED.

Features: install, launch, main activity, web rendering, navigation, login, file picker, camera intent, location, notifications, background service, GMS and native libraries. Successful install does not fill other cells. A foreground host-app escape is FAIL for guest launch, even if the normal app appears usable. Login UI is not authentication evidence. NOT_TESTED has no pass implication.

PASS requires eligible runtime evidence with a matching requirement and full environment/app scope. PARTIAL lists passed milestones and missing/failing ones; BLOCKED identifies an external dependency or missing authorized test access. A collector error is FAIL for the collector and NOT_TESTED for the app behavior. BUILD/CONTRACT tests validate tooling only. Exact app version unknown means historical evidence is displayed separately rather than guessed as Chrome 142.

Evidence register records run/test/requirement IDs, timestamp, observations, reference/hashes, original result, sanitizer version and limitation. Raw private artifacts stay local; share only sanitized summaries with explicit export. Paths and account identifiers are redacted. Historical v0.4.6–0.4.8 Markdown is preserved as reported observation, not silently promoted to current-build ACTIVE.

Latest runtime failures supersede older passes for the SAME feature/scope; latest-passing filters must not conceal regressions. An explicit revalidation interval and host/app updates invalidate eligibility without deleting history. A matrix query can answer tested Android/ABI/device classes, never every phone.

CI gates: schema/foreign-key/scope checks, no missing PASS evidence, no build promotion, latest-failure precedence, unknown fields, policy user separation and HTML escaping. CI does not manufacture device trials. Next phase adds the host/guest collector and archived sanitized physical evidence before Settings consumes the matrix.
