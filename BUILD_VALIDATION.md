# Version 1.4 cloud validation

Validated 2026-09-30 using `./scripts/build-cloud.sh --rerun-tasks`.

- `assembleDebug`: BUILD SUCCESSFUL; all 51 actionable tasks executed.
- `testDebugUnitTest`: 24 tests passed, 0 failed, 0 skipped.
- `lintDebug`: no issues found.
- APK signature and zip alignment: verified.
- Local APK size: 8,863,968 bytes.
- Local APK SHA-256: `6e0fa8c2db0567e1481ce3231849ed601e6ac86f42f8d512b3c9ed4265d40614`.
- JDK 17.0.16, Gradle 8.9, AGP 8.7.3, Kotlin 2.0.21, SDK/build tools 35.

Two additional local UI checks used a temporary Robolectric 4.14.1 native-graphics/Compose harness on Android API 34 at 393×852 dp. They verified the three paint labels, Android document-picker launch and cancellation, a saved image loading after Activity recreation, and photo removal clearing the stored preference. A 1600×900 local image fixture exercised bounded decoding; this was not a photo of the owner's vehicle. Paddock and Garage captures were inspected and updated in docs/screenshots. Temporary test dependencies are excluded from the final APK. Actual phone document-provider persistence and camera EXIF orientation were not device-tested; API 28+ uses Android ImageDecoder for image orientation, while API 26–27 uses BitmapFactory.

The GitHub release workflow independently rebuilds and runs the 24 unit tests and lint before publishing. Release APKs have a different debug signing key than this local build and earlier releases; use the release's SHA256SUMS.txt. Export wanted logs before uninstalling an older development build.

Diagnostic protocol, guidance, API and key-storage code were unchanged by the visual redesign. No phone, adapter, vehicle or live API key was available for this task. Physical-device Bluetooth, API calls, permissions and Keystore interactions still require on-device verification. ECU write/tuning/relearn controls remain unavailable.
