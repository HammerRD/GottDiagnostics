# Gott Diagnostics 1.6

Native Kotlin / Jetpack Compose Android app for an OBDLink MX+ using Bluetooth Classic SPP. Android 8.0+ (API 26). Includes guided OBD recording and direct, owner-approved OpenAI analysis inside the app.

## Install

Download `gott-diagnostics-debug.apk` from the GitHub release assets and open it on your phone. Allow installation from the file-opening app. These are development builds, not production-signed releases. The GitHub runner generates a debug signing key for each build, so Android may reject an in-place update. If an earlier version is installed, export any logs you want to keep before uninstalling it and installing 1.6. Uninstalling clears app data. The API key must be entered again after reinstalling. The release's SHA256SUMS.txt identifies the downloadable APK; a local build has a different signing key/checksum.

## Motorsport interface

The garage uses large personal-photo cards. In Garage, choose Add car photo for each vehicle: white 2003 350Z, red 2006 350Z, and silver 2009 370Z. The Android file picker gives access only to the image you choose. Photo access is saved across app restarts; you can change or remove each photo. Keep the source image on your phone. Photos stay out of diagnostic exports and AI requests. No stock or generated car photos are bundled; until you choose one, the card shows a paint-color label and photo prompt. The red, white and black theme, segmented RPM display and two-column telemetry dashboard remain. All six destinations remain visible in the bottom navigation: Link, Guides, Live, Codes, Garage and AI. Scroll position resets when switching sections, and the Stop recording button stays above the content while recording.

The RPM display is a visual scale, not an ECU redline or a recommended target. Offline/missing readings show dashes and NO DATA; stopped readings are labeled LAST READING. Your selected car photo appears in both the Garage and Paddock. Read-only engine protocols and API consent controls remain in place.

Version 1.4 photo-card captures (version 1.6 also adds an ADD NEW CAR button above the garage):

| Paddock | Live cockpit | Garage |
|---|---|---|
| ![Paddock](docs/screenshots/paddock.png) | ![Live cockpit](docs/screenshots/cockpit.png) | ![Garage](docs/screenshots/garage.png) |

## Your garage

Use **Garage → ADD NEW CAR** to enter year, make, model/trim, paint color, and engine/transmission/fuel/modification/tune details. The new car is saved and selected automatically. Add its photo and symptoms just like the original cars. Disconnect before adding or switching vehicles.

Every saved car uses the same Bluetooth Classic connection, supported live PIDs, DTC scans, logging, generic guided checks, anomaly heuristics, exports, and personal-key AI analysis. Availability depends on ECU/OBD support; manufacturer-specific modules and ECU tuning writes are not added. Generic engine guides and thresholds are not factory specifications for every engine or transmission.

AI analysis and backup exports now choose the latest recording for the selected car. Switching cars clears displayed readings, codes and AI conversation so they are not attributed to the wrong vehicle. Recordings preserve the profile captured at connection time. Existing Nissan logs remain matched by their original profile header; new logs include a stable vehicle ID.


The three original profiles are manual transmission and run owner-reported 93 octane (AKI versus RON was not specified).

- White 2003 350Z VQ35DE: upgraded injectors of unknown specification, aftermarket exhaust; matching calibration unknown.
- Red 2006 350Z VQ35DE Rev-Up: Kinetix intake manifold, full exhaust, high-flow catalytic converters; tune unknown.
- Silver 2009 370Z VQ37VHR: cold-air intake, full exhaust with test pipes; UpRev tune believed to match the modifications but not confirmed.

Add symptoms or corrections under Garage. Each connection saves a profile snapshot. Analysis uses the profile in the recording, not whichever car you later select.

## Record and analyze

1. Plug in and pair the MX+ in Android Bluetooth settings. Close other OBD apps. Select the vehicle, turn ignition on, grant Nearby devices permission, and connect.
2. Under **Guides**, read the conditions, purpose and stop instructions, acknowledge readiness while parked, and start a recording. Or record freely under **Live**.
3. Stop recording, then scan stored and pending DTCs under **Codes** to include both in the same session. Nothing clears codes or sends ECU write commands.
4. Park, open **AI**, enter your own OpenAI API key and save it. API billing is separate from ChatGPT subscriptions; configure spending limits in your API account. The editable default model is `gpt-5.4-mini`; access depends on your project.
5. Enter a question, tap **Review data & analyze**, inspect the exact request body, and confirm **Send & analyze**. Read the answer and ask follow-ups inside the app. No ZIP or external prompt is required. ZIP export remains an optional backup.

Without an API key the app still records, scans, guides and exports. No account login, developer-owned backend, or embedded API key is used.

## Guided collection

Guides use conservative heuristics, not Nissan factory test procedures. They check advertised required PIDs first. Missing readings never count as zero or as a valid condition.

1. **Cold start:** cool for at least 6 hours; connect with ignition on/engine off. Start recording, wait for the engine-start instruction, then start without throttle. Initial coolant and intake temperatures must be within 10 °C; this alone does not prove a full cold soak. Capture about 120 seconds of running data; 5-minute overall cap.
2. **Warm idle:** stationary, neutral, parking brake on, outdoors, A/C off. Looks for 75–105 °C coolant and 550–1,200 RPM. Targets 60 seconds of qualifying data, with a 10-minute cap.
3. **Brief neutral RPM:** only warm and normally running. Gently hold 2,000–2,500 RPM, never redline or blip. A separate timer stops recording **15 seconds after Start**, regardless of PID coverage. Release the accelerator when it stops; never extend the hold to satisfy progress. Targets at least 10 seconds of sampled qualifying coverage and 4 samples; slow adapters may yield incomplete data.
4. **Steady normal driving:** set up while parked, secure the phone, avoid interaction while moving, and have a passenger operate it if needed. Observe legal driving without hard acceleration or lugging. Looks for coolant 75–105 °C, speed >15 km/h, RPM 1,200–3,500, reported load ≤60%, and speed changes ≤8 km/h between samples. Targets 90 seconds of qualifying coverage; 10-minute cap. Never alter driving to chase the timer.

5. **Closed-course high load:** agree the run plan and vehicle-specific limits with your instructor/tuner, verify the car and calibration, start recording while parked, run only on an appropriate closed course, then cool down and review after parking. RPM, speed, coolant and calculated load are required.
6. **Dyno testing:** a qualified operator handles restraints, airflow, extraction, gear, ramp/load and limits. Start recording before the operator's test and stop after unloading/cooldown. RPM, coolant and calculated load are required; speed is optional.

Both loaded guides record for up to ten minutes or until stopped; that cap is a logging limit, not permission to sustain high load. There is no duration target or automatic pass/fail. Calculated load ≥70% with the engine running marks samples for review (track also requires movement). This heuristic is not proof of wide-open throttle, a safe tune, or a driving target. Focused polling includes supported throttle, intake temperature, MAP, ignition advance, MAF, commanded equivalence ratio and fuel-system status. Generic OBD is sequential and may miss brief events. Use independent wideband lambda/AFR and appropriate fuel/oil pressure, temperature and knock instrumentation. These external channels are not imported or synchronized by this release. The app does not control a dyno or write ECU settings.

High coolant temperature (>110 °C) stops a guide. Movement stops stationary guides; RPM above 3,000 stops the neutral guide. Stop for an oil-pressure warning, flashing MIL, overheating, knocking or severe rough running; the app cannot detect all unsafe conditions. For driving sessions, pull over safely before interacting with the phone. Keep the app open; it keeps the screen awake while recording but does not run a background service.

Neutral revving does not reproduce road load and cannot validate a performance tune. For high-load work use a qualified instructor/tuner, a suitable closed course or controlled dyno, and appropriate instrumentation. Identify the 2003's injectors and confirm matching calibration first. High-load capture is offered only for closed-course use or a professional dyno, not public-road testing.

## Data and privacy

- API keys are encrypted with AES-GCM using Android Keystore. Only ciphertext and its IV are stored in private preferences. Keys are excluded from diagnostic files and request bodies, and sent only in HTTPS authorization to `api.openai.com`. Redirects are disabled. Backup/device-transfer exclusions apply; the AI screen blocks screenshots.
- Each request requires confirmation. It sends the recorded vehicle profile, PID units and availability, all-session min/max/mean/count, qualifying statistics separated by guide type, the latest 30 samples, last 30 guide events, last 10 DTC scans, your question, and up to 3 prior exchanges. Long sessions are summarized; not every transient is sent. The preview shows the full body.
- Responses API requests use `store=false`; OpenAI's API data policies still apply. Requests are never automatically retried. Canceling or timing out does not guarantee cancellation of provider processing or charges.
- Conversations are kept in memory (up to 6 exchanges), not persisted across app process restarts. Session JSONL logs remain in private storage; the current or newest session can be analyzed after restart. Keys can be removed in AI settings. Clearing app storage or uninstalling removes local sessions and settings.
- There is no automatic vehicle control. AI receives no tools and cannot execute adapter commands. Its explanations are advisory and do not establish mechanical safety.

## Diagnostic coverage

Generic engine OBD-II, with advertised-PID discovery across 00/20/40. Where supported: RPM, speed, coolant, intake air, engine load, throttle, both-bank short/long fuel trims, fuel-system-1 status, MAF, manifold absolute pressure, ignition advance, engine runtime, ECU voltage and commanded equivalence ratio. Commanded equivalence ratio is not measured AFR, and narrowband O2 is not a wideband measurement. Live data uses the first responding ECU; supported-PID discovery combines responding ECU support. PID samples are sequential rather than simultaneous.

Stored (03) and pending (07) DTCs support legacy and formatted CAN replies, including numbered multi-frame output. Raw replies remain in local session logs. Threshold alerts are heuristics; fuel-system mode and vehicle-specific limits matter. ABS, airbags, individual-cylinder enhanced data, readiness/freeze-frame UI, module coding, relearns, bidirectional tests and ECU flashing are not implemented. Continue using the supported UpRev workflow for 370Z calibration changes.

## Build and validation

Use JDK 17 or 21, SDK 35, build tools 35.0.0 and the Gradle 8.9 wrapper. Set `ANDROID_HOME` or an ignored `local.properties` with `sdk.dir`.

```sh
./gradlew assembleDebug testDebugUnitTest lintDebug
```

Cloud: `./scripts/setup-cloud.sh` installs prerequisites if absent; `./scripts/build-cloud.sh` activates the retained JDK, proxy/CA settings and Maven Central mirror. Use the existing isolated checkout, without creating a worktree.

APK: `app/build/outputs/apk/debug/app-debug.apk`.

Tests exercise PID conversion/discovery, DTC decoding, unavailable data, guide completion/abort/deadline logic, bounded summaries, guide-specific statistics, API request privacy and response parsing, and mocked HTTP success/failure. No real API key, phone or vehicle is available in the cloud, so live OpenAI calls, Android Keystore/permission UI and OBD hardware behavior still require on-device validation. See BUILD_VALIDATION.md for results.
