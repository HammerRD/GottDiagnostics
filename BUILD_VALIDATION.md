# Version 1.2 cloud validation

Validated 2026-09-30 using `./scripts/build-cloud.sh --rerun-tasks`.

- `assembleDebug`: BUILD SUCCESSFUL; all 51 actionable tasks executed.
- `testDebugUnitTest`: 24 tests passed, 0 failed, 0 skipped.
- `lintDebug`: no issues found.
- APK signature and zip alignment: verified.
- Local APK size: 8,847,588 bytes.
- Local APK SHA-256: `5955ed3666ea9a54d034b88f2987c0e028bdbdcac070eb48e853b23314cbab7e`.
- JDK 17.0.16, Gradle 8.9, AGP 8.7.3, Kotlin 2.0.21, SDK/build tools 35.

Two additional local UI checks used a temporary Robolectric 4.14.1 native-graphics/Compose harness on Android API 34, at 393×852 and 320×640 dp. Navigation to all six destinations was verified at standard size; the narrow check verified bottom navigation and the offline cockpit. Actual Android view drawing was captured and inspected. The AI screen's screenshot protection was retained; its heading/navigation was checked without capturing it. These host-only checks and dependencies are not part of the shipped app or standard CI. Captures are in docs/screenshots.

The GitHub release workflow independently rebuilds and runs the 24 unit tests and lint before publishing. Release APKs have a different debug signing key than this local build and earlier releases; use the release's SHA256SUMS.txt. Export wanted logs before uninstalling an older development build.

Diagnostic protocol, guidance, API and key-storage code were unchanged by the visual redesign. No phone, adapter, vehicle or live API key was available for this task. Physical-device Bluetooth, API calls, permissions and Keystore interactions still require on-device verification. ECU write/tuning/relearn controls remain unavailable.
