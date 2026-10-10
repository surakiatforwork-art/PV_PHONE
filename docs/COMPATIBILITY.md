# Compatibility for 0.4.5

Device: Infinix X6856, Android 16 / API 36, arm64. Validation date: 10 October 2026.

| Scenario | Result |
|---|---|
| Signed update from 0.4.3-alpha | Passed; guest data retained |
| Launch disposable guest APK | Passed |
| Default CAMERA via package/self checks | Passed, both granted |
| Virtual Deny CAMERA with host still granted | Passed, both denied |
| Reset policy to Default | Passed, both grants restored |
| Transsion camera thumbnail result | Passed, RESULT_OK + bitmap |
| Transsion camera guest-owned EXTRA_OUTPUT content URI | Passed, RESULT_OK + nonempty JPEG |
| Camera cancellation callback | Passed, RESULT_CANCELED |
| Universal APK on this arm64 device | Passed install, permission and output-capture smoke tests |
| Policy save failure preserves Deny | Passed JVM harness with Android shims |
| Virtual user isolation | Passed JVM harness; physical multi-user untested |
| DuckDuckGo | Unsupported: notification-channel package check fails |
| Chrome | Experimental: onboarding partially works; isolated navigation not certified |
| Google Play / GMS sign-in | Experimental, not certified |
| com.meitu.meiyancamera provider | Failed in external camera during image completion |
| Camera2 / CameraX / getUserMedia redirection | Not implemented; intent routing only |
| Other devices, Android versions, x86_64, armeabi-v7a | Untested |

See ../RELEASE_NOTES.md for measured results, build provenance and known limits.
