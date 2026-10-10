# Diagnostic guest design

Extend the existing disposable tests/android fixture incrementally; it is not a clone concealment module. No reference native code is copied. First iteration supplies models and tests; new native probes require a separate device integration gate.

| Observation | Test and oracle | Boundary/limit |
|---|---|---|
| Java package / ApplicationInfo / PM | Own package/version, real signer, data path versus registered guest fixture | Facade identity only |
| Java UID vs kernel UID | Process.myUid and independently observed host Linux UID | Difference is expected diagnostic evidence |
| Data directory | Own sentinel round trip, two guest/users cannot collide in logical path | Shared host UID is still weak isolation |
| Activity stack / instrumentation | Direct transition and returned virtual UID; focused StubActivity observation | Do not call mere hook registration PASS |
| Binder caller | Disposable cross-provider query records UID/package | Preserve host/virtual boundary distinction |
| libc vs raw filesystem | Original fixture sentinel through Java, JNI libc stat/open and kernel syscall independently | DIFFERENT is recorded; no attempt to hide it |
| /proc/self/maps | Identify fixture library load and linker origin; sanitized summary | Never export complete user path inventory |
| Native loading | Small original JNI library loads and returns a constant; dependent library/split test | ABI/page-size namespace limits scoped separately |
| Storage | Current five-path/all-files/rename/delete/rmdir test; own marker only | No other app-private file probing |
| Camera capture | Thumbnail and EXTRA_OUTPUT RESULT_OK, fresh file and grants, real selected component | Capture intent only |
| Location | Authorized device fix and permission-denied case, provenance preserved | No spoofing/mock-flag changes |
| WebView | Separate data directory slot, renderer PID, known local page and JS result | First frame not navigation/login |
| Provider / broadcast / service | Own component nonce/callback/lifecycle plus two-user isolation | No private production data |
| PendingIntent | Existing transition retained in guest with request nonce | Test cancellation and immutable/update flags later |

Outcome vocabulary distinguishes PASS/FAIL/PARTIAL/BLOCKED/NOT_TESTED and observed DIFFERENT detail. A raw syscall difference is not automatically FAIL when kernel authority is intentionally host-owned. Test each public promise against its requirement.

Transport: private host capture endpoint, per-run random nonce, explicit guest/user key, bounded messages, no arbitrary shell execution, never public global permission grants. Run controls are explicit and stop/cancel supported. Artifacts use UTC timestamps and exact runtime/app build; host reports retain sanitizer/version metadata.

Native audit findings: IOUniformer currently hooks a selected libc symbol set, not comprehensive open/read/raw syscall paths; hook_dlopen is disabled. v0.4.8 guard refuses short far-hook overwrites, reducing unsafe coverage rather than proving every IO path redirected. Audit libc/raw syscall and page-size behavior on at least representative OEM/API/ABI combinations before recommending any new native engine. Do not add Dobby/xDL without a reproducible unresolved defect and a bounded alternative comparison.
