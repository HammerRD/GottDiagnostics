# Version 1.5 cloud validation

Validated 2026-09-30 using `./scripts/build-cloud.sh --rerun-tasks`.

- `assembleDebug`: BUILD SUCCESSFUL; all 51 actionable tasks executed.
- `testDebugUnitTest`: 30 tests passed, 0 failed, 0 skipped.
- `lintDebug`: no issues found.
- APK signature and zip alignment: verified.
- Local APK size: 8,880,356 bytes.
- Local APK SHA-256: `5d3fdaf3678e8b1449a7c60d24bfb109143be8698cb74baeb4b8934987809e83`.
- JDK 17.0.16, Gradle 8.9, AGP 8.7.3, Kotlin 2.0.21, SDK/build tools 35.

One additional local UI check used a temporary Robolectric 4.14.1 native-graphics/Compose harness on Android API 34 at 393×852 dp. It entered a custom Ford profile through the full-screen form, saved and selected it, verified the generic vehicle card after Activity recreation, checked persisted profile JSON, and switched back to the original Nissan. Six permanent garage tests cover serialization, validation, unique identities, malformed storage and selected-vehicle session matching including legacy Nissan logs. Temporary UI-test dependencies are excluded from the final APK. Earlier modal-form attempts hit host test idling timeouts; the shipped full-screen form passed the complete flow. Physical keyboard/document-provider behavior still needs phone verification. The existing screenshots show the v1.4 photo-card design; v1.5 adds the new-car flow.

The GitHub release workflow independently rebuilds and runs the 30 unit tests and lint before publishing. Release APKs have a different debug signing key than this local build and earlier releases; use the release's SHA256SUMS.txt. Export wanted logs before uninstalling an older development build.

OBD commands and key-storage code are unchanged. Custom profiles now participate in the same diagnostics; analysis and exports select the current car’s latest log. Generic guide applicability text was broadened for other engines and transmissions. No phone, adapter, vehicle or live API key was available for this task. Physical-device Bluetooth, API calls, permissions and Keystore interactions still require on-device verification. ECU write/tuning/relearn controls remain unavailable.
