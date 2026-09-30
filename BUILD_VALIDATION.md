# Version 1.6 cloud validation

Validated 2026-09-30 using `./scripts/build-cloud.sh --rerun-tasks`.

- `assembleDebug`: BUILD SUCCESSFUL; all 51 actionable tasks executed.
- `testDebugUnitTest`: 36 tests passed, 0 failed, 0 skipped.
- `lintDebug`: no issues found.
- APK signature and zip alignment: verified.
- Local APK size: 8,880,356 bytes.
- Local APK SHA-256: `0e603246c4d262c29da286629b60cc0eba5c5ea5178ac6f5c9c544da90e961c2`.
- JDK 17.0.16, Gradle 8.9, AGP 8.7.3, Kotlin 2.0.21, SDK/build tools 35.

One additional local UI check used a temporary Robolectric 4.14.1 native-graphics/Compose harness on Android API 34 at 393×852 dp. It selected the fifth and sixth guides, verified their distinct category labels and the displayed measurement-limit panel. Six additional permanent guide tests cover ordering/required PIDs, track movement, dyno zero/missing speed, no automatic pass/fail, missing channels and temperature aborts, and coverage excluding cooldown/long gaps. Existing garage and diagnostic tests remain included. Temporary UI-test dependencies are excluded from the final APK. Screenshots in docs show the earlier photo-card design, not the two new guides.

The GitHub release workflow independently rebuilds and runs the 36 unit tests and lint before publishing. Release APKs have a different debug signing key than this local build and earlier releases; use the release's SHA256SUMS.txt. Export wanted logs before uninstalling an older development build.

Loaded guides use a focused subset of existing read-only PIDs. Track and dyno markers are included in logs and AI summaries; generic OBD data does not establish safe fueling or tune safety. No physical track, dyno, vehicle, adapter or live API validation was available. External instrumentation is not imported or synchronized, and ECU write/tuning/relearn controls remain unavailable.
