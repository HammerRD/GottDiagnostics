# Gott Diagnostics

Native Kotlin / Jetpack Compose Android app for a paired OBDLink MX+ Bluetooth Classic (SPP) adapter. Android 8.0 or later (API 26+).

## Use

1. Install the debug APK, plug in the MX+, turn ignition on, and pair the adapter in Android Bluetooth settings. Close other apps using the adapter.
2. Enter year, make, model, engine, and symptoms in **Session** before connecting. The profile is saved locally.
3. In **Connect**, grant Nearby devices permission (Android 12+) and choose the paired adapter.
4. In **Live**, start monitoring. RPM, speed, coolant, load, throttle, intake temperature, short/long fuel trim, and ECU voltage are polled sequentially. Missing/unsupported readings appear as a dash. Sampling speed depends on the vehicle protocol.
5. Stop monitoring, then use **Codes** to scan stored (03) and pending (07) codes. Scanning is exclusive with polling. Codes are never cleared.
6. Stop recording before export. **Session → Export diagnostic ZIP** shares the current or most recently saved log and an analysis prompt. Attach to ChatGPT; if ZIP uploads are unavailable, extract and attach the JSONL file. No cloud service or API key is required by the app.

Sessions are timestamped JSONL files in app-private storage. Raw replies, decoded samples, vehicle details, DTCs, and threshold alerts are retained. The system share sheet grants temporary read access to exported ZIPs. Logs remain until app data is cleared or the app is uninstalled. Review profile details before sharing. Android backup is disabled.

## Build

Use JDK 17 or 21, Android SDK platform 35 and build tools 35.0.0, and the checked-in Gradle 8.9 wrapper. Set `ANDROID_HOME` or create an untracked `local.properties` containing `sdk.dir=/path/to/android-sdk`.

```sh
./gradlew assembleDebug testDebugUnitTest lintDebug
```

In this cloud environment, SDK and Gradle caches are under `/workspace/toolchains/android-sdk` and `/workspace/.gradle`. Run `./scripts/build-cloud.sh`; it activates the installed full JDK, uses Google’s Maven Central mirror, and passes the environment's existing proxy and CA trust to Gradle while preserving TLS verification. Network-enabled execution is required for dependency downloads.

APK: `app/build/outputs/apk/debug/app-debug.apk`

Install with `adb install -r app/build/outputs/apk/debug/app-debug.apk`, or copy to a phone and allow installation from the file-opening app. The debug APK uses a development signing key, not a production release key.

## Scope and validation

Generic engine OBD-II only, with ELM/STN AT initialization and RFCOMM SPP. ABS, airbags, bidirectional controls, manufacturer-specific modules, and automated repair diagnosis are outside this version. The app reads data without clearing fault memory or issuing actuator commands. Numbered ELM multi-frame responses and CAN DTC count fields are decoded; no BLE connection is used.

Monitoring is a foreground UI workflow: keep the app open. It does not run a background service. A disconnected screen retains the last measurements, explicitly labeled as such. Reconnecting starts a new session. The live screen reports the latest sample's alerts rather than a persistent diagnosis. Coolant >110 °C, voltage <11.8 or >15.2 V, and absolute fuel trim >20% are heuristic flags; engine state and OEM specifications matter.

Unit tests cover PID conversion, unavailable/truncated replies, DTC families, CAN multi-frame decoding, and anomaly thresholds. Hardware communication, Android permission prompts, and sharing require phone/adapter validation; a cloud build does not establish vehicle compatibility. Operate while parked or have a passenger operate the app.
