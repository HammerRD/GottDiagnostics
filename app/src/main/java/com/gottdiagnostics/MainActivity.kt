package com.gottdiagnostics

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

class MainActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RaceTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { DiagnosticsScreen() }
            }
        }
    }
    @Composable private fun DiagnosticsScreen(model: DiagnosticsModel = viewModel()) {
        val state by model.state.collectAsState()
        var tab by rememberSaveable { mutableIntStateOf(0) }
        var message by remember { mutableStateOf("") }
        var guide by rememberSaveable { mutableStateOf(Guide.IDLE) }
        var ready by remember(guide, state.vehicleId) { mutableStateOf(false) }
        var apiKey by remember { mutableStateOf("") }
        val scope = rememberCoroutineScope()
        val photoPrefs = remember { getSharedPreferences("vehicle_photos", MODE_PRIVATE) }
        var photos by remember { mutableStateOf(Vehicles.all.associate { it.id to photoPrefs.getString(it.id, null) }) }
        var photoVehicle by rememberSaveable { mutableStateOf<String?>(null) }
        val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            val id = photoVehicle
            photoVehicle = null
            if(uri != null && id != null) {
                try {
                    contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    val old = photos[id]
                    photoPrefs.edit().putString(id, uri.toString()).apply()
                    photos = photos + (id to uri.toString())
                    if(old != null && old != uri.toString() && old !in photos.values) {
                        runCatching { contentResolver.releasePersistableUriPermission(android.net.Uri.parse(old), Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                    }
                    message = ""
                } catch(e: Exception) { message = "Could not save access to this photo. Choose an image stored on your phone." }
            }
        }
        val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if(it) model.refresh() else message = "Allow Nearby devices permission in Android app settings." }
        DisposableEffect(state.monitoring, tab) {
            if(state.monitoring) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            if(tab == 5) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            onDispose { window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or WindowManager.LayoutParams.FLAG_SECURE) }
        }
        val editable = !state.connected && !state.busy && !state.aiBusy
        Column(Modifier.safeDrawingPadding().imePadding().padding(horizontal = 16.dp)) {
            RaceHeader(state)
            Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(state.status, style = MaterialTheme.typography.bodySmall, color = RaceMuted)
                    if(state.session != null) RaceEyebrow("${state.samples} SAMPLES / LOCAL SESSION", RaceWhite)
                }
                if(state.monitoring) FilledTonalButton(onClick = model::stopMonitoring, colors = ButtonDefaults.filledTonalButtonColors(containerColor = RaceRed, contentColor = Color(0xFF1B0B0B))) { Text("STOP") }
            }
            Box(Modifier.weight(1f)) {
              key(tab) {
               Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                when(tab) {
                    0 -> {
                        RaceHeading("01 / PADDOCK", "Your pit wall.", "Connect your machine. Find the story in the data.")
                        VehicleHero(Vehicles.get(state.vehicleId), photoUri = photos[state.vehicleId])
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(onClick = { tab = 4 }, modifier = Modifier.weight(1f)) { Text("OPEN GARAGE") }
                            OutlinedButton(onClick = { tab = 2 }, modifier = Modifier.weight(1f)) { Text("LIVE COCKPIT") }
                        }
                        RacePanel("ADAPTER / OBDLINK MX+", "Plug in the adapter, turn ignition on, and pair it in Android Bluetooth settings. Close other OBD apps before connecting.")
                        OutlinedButton(onClick = { startActivity(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)) }) { Text("Bluetooth settings") }
                        Button(onClick = { if(Build.VERSION.SDK_INT >= 31) permission.launch(Manifest.permission.BLUETOOTH_CONNECT) else model.refresh() }, enabled = editable, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("FIND PAIRED ADAPTERS") }
                        state.devices.forEach { device ->
                            OutlinedButton(onClick = { model.connect(device.address) }, enabled = editable, modifier = Modifier.fillMaxWidth()) { Text("${device.name}\n${device.address}") }
                        }
                        if(state.connected || state.busy) Button(onClick = model::disconnect) { Text("Disconnect / cancel connection") }
                        if(state.supported != null) Text("${Obd.pids.count { it.code in state.supported!! }} of ${Obd.pids.size} app PIDs advertised by this ECU. Other readings remain unavailable.")
                        Text("For a cold-start guide, connect with ignition on and engine off after cooling. For other guides, follow the guide's warm-up steps. Use while parked or let a passenger operate the app.")
                    }
                    1 -> {
                        RaceHeading("02 / GUIDED SESSIONS", "Run a better check.", "The right conditions. More useful data.")
                        Text("Neutral RPM is not a substitute for road load. These are conservative collection guides, not Nissan factory test procedures.", color = RaceMuted)
                        Guide.entries.forEach { item -> GuideOption(item, guide == item, !state.busy) { guide = item } }
                        Text(guide.title, style = MaterialTheme.typography.titleLarge)
                        RacePanel("01 / SET UP", guide.instructions, RaceAccent)
                        RacePanel("02 / WHAT YOU WILL LEARN", guide.purpose)
                        RacePanel("STOP CONDITIONS", guide.abort, RaceRed)
                        Text("Required channels: " + guide.required.joinToString { code -> Obd.pids.first { it.code == code }.name }, style = MaterialTheme.typography.bodySmall, color = RaceMuted)
                        Row { Checkbox(checked = ready, onCheckedChange = { ready = it }, enabled = !state.busy); Text("I am parked, have read the steps and stop conditions, and will not handle the phone while driving.", Modifier.padding(top = 8.dp)) }
                        Button(onClick = { model.monitor(guide); ready = false }, enabled = ready && state.connected && !state.busy && !state.aiBusy, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("START GUIDED RECORDING") }
                        state.guidance?.let { progress ->
                            RacePanel(if(progress.complete) "CAPTURE COMPLETE" else "SESSION PROGRESS", "${state.guide?.title ?: "Recording"}\n${progress.text}\nApproximate qualifying coverage: ${progress.seconds} s • ${progress.samples} readings", if(progress.complete) RaceAccent else RaceWhite)
                        }
                        if(state.vehicleId == "350z-2003") Text("2003 DE: unknown injectors and calibration. Identify them and verify matching calibration before high-load fueling evaluation.", color = MaterialTheme.colorScheme.error)
                        Text("High-load / dyno testing", style = MaterialTheme.typography.titleMedium)
                        Text("Arrange a qualified tuner and controlled dyno with appropriate instrumentation. No full-throttle road guide is offered. Generic OBD data cannot establish safe fueling under load.")
                    }
                    2 -> {
                        RaceHeading("03 / TELEMETRY", "Live cockpit.", if(state.connected) Vehicles.get(state.vehicleId).title else if(state.values.isEmpty()) "Connect to see your engine data." else "Last recorded values • reconnect to update.")
                        Button(onClick = { model.monitor() }, enabled = state.connected && !state.busy && !state.aiBusy, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("START RECORDING") }
                        Tachometer(state)
                        val headlinePids = listOf("0D", "05", "04", "42", "10", "11")
                        headlinePids.chunked(2).forEach { pair ->
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                pair.forEach { code -> MetricTile(Obd.pids.first { it.code == code }, state.values[code], Modifier.weight(1f)) }
                            }
                        }
                        Text("Keep the app open. Values are sequential samples. — means unavailable; after stopping, the display retains the last reading.", style = MaterialTheme.typography.bodySmall, color = RaceMuted)
                        RaceEyebrow("ENGINE CHANNELS", RaceWhite)
                        Surface(shape = RoundedCornerShape(14.dp), color = RacePanel, border = BorderStroke(1.dp, RaceLine)) {
                            Column(Modifier.padding(horizontal = 14.dp)) {
                                Obd.pids.filter { it.code !in headlinePids && it.code != "0C" }.forEach { pid ->
                                    Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text(pid.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = RaceMuted)
                                        Text(state.values[pid.code]?.let { "%.2f %s".format(it, pid.unit) } ?: "—", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyMedium)
                                    }
                                    HorizontalDivider(color = RaceLine)
                                }
                            }
                        }
                        Text("Fuel-system status: 1 cold/open loop, 2 closed loop, 4 load/decel open loop, 8 fault open loop, 16 closed loop with fault. Commanded equivalence ratio is not measured AFR.", style = MaterialTheme.typography.bodySmall, color = RaceMuted)
                        RaceEyebrow("THRESHOLD WATCH", RaceSilver)
                        Text("Heuristic thresholds; consider engine state, closed-loop status and vehicle specifications.")
                        if(state.alerts.isEmpty()) Text(if(state.samples == 0) "Start recording to evaluate readings." else "No threshold alerts in the latest sample.")
                        state.alerts.forEach { RacePanel("CHECK THIS READING", it, RaceRed) }
                    }
                    3 -> {
                        RaceHeading("04 / FAULT MEMORY", "Decode the warning.", "Stored and pending powertrain trouble codes.")
                        RacePanel("BEFORE YOU SCAN", "Stop recording first. Trouble codes and recorded readings stay in the same session for AI analysis.")
                        Button(onClick = model::scan, enabled = state.connected && !state.busy && !state.aiBusy, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("SCAN TROUBLE CODES") }
                        state.codes.forEach { code -> RacePanel("ECU REPORTED", code, RaceSilver) }
                        if(state.codes.isEmpty()) RacePanel("NO CODES DISPLAYED", "Use Scan to read the ECU. An empty list before a scan does not establish a healthy vehicle.", RaceMuted)
                        Text("A DTC identifies a detected condition, not necessarily a failed part. No codes are cleared.")
                        Text("Service, relearns & performance tuning", style = MaterialTheme.typography.titleLarge)
                        Text("Not enabled: verified ECU-specific procedures and compatible write access have not been established for these cars. This app sends only adapter setup and OBD read requests. Use the supported UpRev workflow for the 370Z's calibration. AI cannot execute vehicle commands.")
                    }
                    4 -> {
                        RaceHeading("05 / THE LINEUP", "Built to be driven.", "Three machines. Three different stories.")
                        Text("Your car photos stay on this phone and are not sent with diagnostics or AI requests.", color = RaceMuted)
                        Vehicles.all.forEach { vehicle ->
                            VehicleHero(vehicle, state.vehicleId == vehicle.id, compact = true, enabled = editable,
                                onClick = { model.selectVehicle(vehicle.id) }, photoUri = photos[vehicle.id])
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(enabled = !state.monitoring, onClick = {
                                    photoVehicle = vehicle.id
                                    photoPicker.launch(arrayOf("image/*"))
                                }) { Text(if(photos[vehicle.id] == null) "Add car photo" else "Change photo") }
                                if(photos[vehicle.id] != null) TextButton(enabled = !state.monitoring, onClick = {
                                    val old = photos[vehicle.id]
                                    photoPrefs.edit().remove(vehicle.id).apply()
                                    photos = photos + (vehicle.id to null)
                                    if(old != null && old !in photos.values) runCatching {
                                        contentResolver.releasePersistableUriPermission(android.net.Uri.parse(old), Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                }) { Text("Remove") }
                            }
                        }
                        RacePanel("CURRENT SETUP", Vehicles.get(state.vehicleId).details, vehicleAccent(state.vehicleId))
                        OutlinedTextField(value = state.notes, onValueChange = model::profile, label = { Text("Symptoms / additional vehicle details") }, modifier = Modifier.fillMaxWidth(), minLines = 3, enabled = editable)
                        Text("Disconnect to change vehicle details. Each connection records a snapshot of this profile. AI always uses the profile saved with the recording, even if you later select another car.")
                        Text("Optional backup", style = MaterialTheme.typography.titleLarge)
                        Text("Use AI analysis for direct in-app help. ZIP export remains available as a local backup; it is not required for AI.")
                        Button(enabled = !state.busy && !state.aiBusy, onClick = {
                            scope.launch {
                                try {
                                    val file = model.export()
                                    val uri = FileProvider.getUriForFile(this@MainActivity, "$packageName.files", file)
                                    val intent = Intent(Intent.ACTION_SEND).apply { type = "application/zip"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); clipData = android.content.ClipData.newRawUri("Diagnostic session", uri) }
                                    startActivity(Intent.createChooser(intent, "Share diagnostic backup"))
                                } catch(e: Exception) { message = e.message ?: "Export failed" }
                            }
                        }) { Text("Export backup ZIP") }
                    }
                    5 -> {
                        RaceHeading("06 / AI ANALYSIS", "Your second opinion.", "Turn a session into your next diagnostic step.")
                        RaceBadge(if(state.hasApiKey) "PERSONAL API / KEY SAVED" else "PERSONAL API / SETUP NEEDED", RaceWhite)
                        RacePanel("YOU STAY IN CONTROL", "Park and stop recording before analysis. Data goes directly to OpenAI only after your confirmation. API charges are separate from ChatGPT subscriptions. AI advice cannot change ECU settings.")
                        OutlinedTextField(value = state.question, onValueChange = model::question, label = { Text("Question or follow-up (optional)") }, minLines = 2, modifier = Modifier.fillMaxWidth(), enabled = !state.aiBusy)
                        Button(onClick = model::prepareAnalysis, enabled = !state.busy && !state.aiBusy && state.hasApiKey, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("REVIEW DATA & ANALYZE") }
                        if(state.aiBusy) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("Preparing / requesting analysis…"); OutlinedButton(onClick = model::cancelAnalysis) { Text("Cancel") } }
                        if(state.aiError.isNotEmpty()) Text(state.aiError)
                        if(state.analysisSession.isNotEmpty()) Text("Analysis of: ${state.analysisSession}", style = MaterialTheme.typography.bodySmall)
                        state.conversation.forEachIndexed { index, (question, answer) -> Surface(Modifier.fillMaxWidth(), color = RacePanel, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, RaceWhite.copy(alpha = 0.35f))) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            RaceEyebrow("ANALYSIS / ${index + 1}", RaceWhite)
                            Text(if(question.isBlank()) "Diagnostic assessment" else question, style = MaterialTheme.typography.titleMedium)
                            SelectionContainer { Text(answer) }
                        } } }
                        HorizontalDivider(color = RaceLine)
                        RaceEyebrow("PERSONAL API / SETTINGS", RaceWhite)
                        Text(if(state.hasApiKey) "A key is saved, encrypted using Android Keystore. It is excluded from logs, exports and backups." else "Enter your OpenAI API key on this phone. Do not send it in chat. Enable API billing and set a spending limit in your OpenAI account.")
                        OutlinedTextField(value = apiKey, onValueChange = { apiKey = it }, label = { Text("OpenAI API key") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth(), enabled = !state.aiBusy)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { model.saveApiKey(apiKey); apiKey = "" }, enabled = apiKey.isNotBlank() && !state.aiBusy) { Text("Save key") }
                            OutlinedButton(onClick = { model.removeApiKey(); apiKey = "" }, enabled = state.hasApiKey && !state.aiBusy) { Text("Remove key") }
                        }
                        OutlinedTextField(value = state.aiModel, onValueChange = model::aiModel, label = { Text("OpenAI model") }, modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !state.aiBusy)
                        Text("Default: ${AnalysisProtocol.defaultModel}. Model access depends on your API project. Requests disable response storage with store=false; OpenAI's API data policies still apply. No automatic retries. Follow-ups include the latest 3 exchanges; conversations remain in memory for this app session.", style = MaterialTheme.typography.bodySmall)
                    }
                }
                if(message.isNotEmpty()) Text(message, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(16.dp))
               }
              }
            }
            Box(Modifier.padding(vertical = 8.dp)) { RaceNavigation(tab) { tab = it } }
        }
        state.aiPreview?.let { preview -> AlertDialog(
            onDismissRequest = model::dismissPreview,
            title = { Text("Send this data to OpenAI?") },
            text = { Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                Text("This sends the recorded vehicle profile, diagnostic summary, guide results, your question and recent conversation. It excludes your API key from the request body and diagnostic logs. API usage is billed to your account. The full request body is below.")
                SelectionContainer { Text(preview, style = MaterialTheme.typography.bodySmall) }
            } },
            confirmButton = { TextButton(onClick = model::sendAnalysis) { Text("Send & analyze") } },
            dismissButton = { TextButton(onClick = model::dismissPreview) { Text("Cancel") } }
        ) }
    }
}
