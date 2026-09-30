# Version 1.1 cloud validation

Validated 2026-09-30 using `./scripts/build-cloud.sh`.

- `assembleDebug`: BUILD SUCCESSFUL.
- `testDebugUnitTest`: 24 tests passed, 0 failed, 0 skipped.
- `lintDebug`: no issues found.
- `apksigner verify --verbose`: verified with APK Signature Scheme v2.
- `zipalign -c 4`: passed.
- Local APK: `app/build/outputs/apk/debug/app-debug.apk`, 8,905,124 bytes.
- Local APK SHA-256: `c562e9c228ff3a65707c6106bb986885fdb02aa73b8c939c881a9b498d5801e2`.
- JDK 17.0.16, Gradle 8.9, AGP 8.7.3, Kotlin 2.0.21, SDK/build tools 35.

Tests cover protocol parsing and PID support, guide preconditions/timing/abort behavior, summary bounds and per-guide aggregates, Responses API request construction, answer/refusal/incomplete parsing, and mocked HTTP success/failure handling. The API schema was checked against OpenAI's official Python SDK types for the Responses endpoint. Tests do not send a live paid API request.

The GitHub release workflow separately rebuilds, tests, lints, signs and verifies the downloadable APK. It has a different debug signing key than the cloud build and earlier releases; use the release's SHA256SUMS.txt for the download checksum. Export wanted logs before uninstalling an older development build if Android rejects the update.

No phone, adapter or vehicle was attached. Live OpenAI credentials were not supplied. Bluetooth hardware, Android permission/Keystore behavior, on-device UI and live model analysis remain unverified here. There are no enabled ECU writes, tuning or relearn functions.
