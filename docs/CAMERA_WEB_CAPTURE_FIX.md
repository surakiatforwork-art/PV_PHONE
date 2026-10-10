# Guest browser capture output fix (development build)

DuckDuckGo guest could launch the selected capture handler, take a photo and
return to the page, while the file input still showed no selected file. This was
reproduced on the user's [Camera API Test Lab](https://surakiatforwork-art.github.io/camera_api_test/).
The website was not modified.

## Observed cause

The guest's external cache directory was created and reported as existing, but
opening the capture file returned `FileNotFoundException: ENOENT`. Existing
VirtualApp IO hooks redirected `mkdir/stat` for app-private external storage,
while public libc `open/openat` entry points were not hooked. Consequently the
camera's grantable host bridge could reach the guest provider, but the provider
opened the original path rather than the private virtual storage path. The camera
then returned `RESULT_CANCELED`, correctly leaving the browser without a new file.

This affects the cache-backed flow used by DuckDuckGo's
[capture contract](https://github.com/duckduckgo/Android/blob/develop/app/src/main/java/com/duckduckgo/app/browser/filechooser/capture/MediaCaptureResultHandler.kt)
and [image mover](https://github.com/duckduckgo/Android/blob/develop/app/src/main/java/com/duckduckgo/app/browser/filechooser/capture/postprocess/MediaCaptureImageMover.kt).
These sources were inspected for diagnosis; no DuckDuckGo code was copied.
A separate original probe reproduced the same failure with an external cache
output and a full-width activity request code. Its internal-storage output passed.

## Change

`native_open_redirect.patch` extends the existing IOUniformer with public
`open/open64/openat/openat64` wrappers. They apply the existing guest path rules
and call the original Android wrapper, retaining flags, mode, relative-dirfd
behavior and errors. Denied paths return EACCES. Alias addresses are hooked once:
on the tested libc, open64 shares open's address and openat64 shares openat's.
The existing conservative short-function guard remains in effect. No hook of
private syscall stubs or new native framework was added.

Capture URI ownership, virtual user scope, grant handling and the original
activity result remain intact. No successful result is fabricated and no guest
policy is relaxed. Bridge diagnostics record metadata and errors, not image bytes.
The Android Engine Diagnostics screen continues to report unmeasured guest
capabilities as UNKNOWN / NOT_TESTED.

## Physical validation, 2026-10-10

Tested build: **0.4.9-dev / code 71**, source `e7438cf`, stable signing key,
arm64 APK on Infinix X6856 / Android 16 API 36 / 4096-byte pages.
APK SHA-256: `f38ba6cfa85bdecc61ff8566967f15149a03598f9d0cf18d696abc95c7e48e84`.
DuckDuckGo version: **5.297.0 / code 52970000**. Scope: guest user 0.

| Check | Result |
|---|---|
| Before fix, internal probe output | RESULT_OK, 319154 bytes |
| Before fix, external cache probe | RESULT_CANCELED, 0 bytes, provider ENOENT |
| Before fix, DuckDuckGo + Timestamp Camera | RESULT_CANCELED, provider ENOENT, no web file |
| After fix, external cache probe, request 1390560349 | RESULT_OK, 350437 bytes |
| After fix, DuckDuckGo + Timestamp Camera Free | RESULT_OK; website reports JPEG 0.33 MB and displays the actual preview |
| After fix, DuckDuckGo + Infinix system camera | RESULT_OK; website reports JPEG 3.51 MB |
| Cancel subsequent capture | Previous web file selection retained; no false success |
| Shared storage probe | Five path reads, root listing, write, rename, delete and rmdir pass |
| File Manager+ guest | Main screen opens after update |
| Cleanup | Disposable real/virtual probe and three created sentinel files removed; original nine visible guests retained |

Native installation logs confirm open and openat hooks installed on this device.
The [signed runtime build](https://github.com/surakiatforwork-art/PV_PHONE/actions/runs/38043504746)
compiled all configured ABIs and verified arm64/universal APK signatures.
The [native wrapper contract test](https://github.com/surakiatforwork-art/PV_PHONE/actions/runs/38043678421)
passed creation modes, read/append/truncate, CLOEXEC, relative and absolute dirfd
paths, missing-file errors and denied-path errors using the production wrappers.
Existing permission-policy regression and 13 diagnostic contract/boundary tests
also passed. Universal APK was built, but physical testing used arm64 only.

This is evidence for the tested file-input photo flow. Other browser versions,
video/audio capture and embedded getUserMedia/Camera2 behavior are not validated
by these tests. Existing experimental GMS service crashes and an intermittent
WebView startup failure remain separate issues; browser launch required a retry
during this session. No GitHub tag or release was created; stable remains v0.4.8.
