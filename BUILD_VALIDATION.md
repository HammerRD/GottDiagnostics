# Cloud build validation

Validated 2026-09-30 in this cloud environment.

- Final command: `./scripts/build-cloud.sh --rerun-tasks`
- Result: **BUILD SUCCESSFUL**; all 51 actionable tasks executed.
- `assembleDebug`: passed.
- `testDebugUnitTest`: 5 tests, 0 failures, 0 errors, 0 skipped.
- `lintDebug`: no warnings or errors.
- `apksigner verify --verbose`: verified with APK Signature Scheme v2.
- `zipalign -c 4`: passed.
- Reusable setup: `./scripts/setup-cloud.sh` passed using the installed toolchains and caches.
- Toolchain: Temurin JDK 17.0.16, Gradle 8.9, AGP 8.7.3, Kotlin 2.0.21, Android SDK 35/build tools 35.0.0.
- APK: `app/build/outputs/apk/debug/app-debug.apk` (8,749,228 bytes).
- SHA-256: `1452fd33ce6590dd12ff448d770f5972b774b06555bf61d2805b893b7998b38f`.

The build packages Compose's `libandroidx.graphics.path.so` without stripping debug symbols; this is a nonblocking packaging notice. The APK is signed with a development key. No phone, emulator, vehicle, or OBDLink adapter was attached, so Bluetooth hardware behavior, on-device UI, and sharing have not been validated here.
