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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
            MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFF64DFC4), background = Color(0xFF101820), surface = Color(0xFF192630))) {
                Surface(Modifier.fillMaxSize()) { DiagnosticsScreen() }
            }
        }
    }
    @Composable private fun DiagnosticsScreen(model: DiagnosticsModel = viewModel()) {
        val state by model.state.collectAsState()
        var tab by remember { mutableIntStateOf(0) }
        var message by remember { mutableStateOf("") }
        var guide by remember { mutableStateOf(Guide.IDLE) }
        var ready by remember(guide, state.vehicleId) { mutableStateOf(false) }
        var apiKey by remember { mutableStateOf("") }
        val scope = rememberCoroutineScope()
        val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if(it) model.refresh() else message = "Allow Nearby devices permission in Android app settings." }
        DisposableEffect(state.monitoring, tab) {
            if(state.monitoring) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            if(tab == 5) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            onDispose { window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or WindowManager.LayoutParams.FLAG_SECURE) }
        }
        val editable = !state.connected && !state.busy && !state.aiBusy
        Column(Modifier.safeDrawingPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text("GOTT DIAGNOSTICS", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            Text(Vehicles.get(state.vehicleId).title + " • manual • 93 octane", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) {
                Text(state.status, style = MaterialTheme.typography.titleSmall)
                if(state.session != null) Text("${state.samples} samples • saved on this phone", style = MaterialTheme.typography.bodySmall)
                if(state.monitoring) TextButton(onClick = model::stopMonitoring) { Text("Stop recording") }
            } }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                listOf("Connect", "Guides", "Live", "Codes", "Vehicle", "AI analysis").forEachIndexed { i, title -> TextButton(onClick = { tab = i }) { Text(title, color = if(tab == i) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) } }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when(tab) {
                    0 -> {
                        Text("OBDLink MX+", style = MaterialTheme.typography.headlineSmall)
                        Text("Select your car under Vehicle before connecting. Plug in the adapter, turn ignition on, and pair OBDLink MX+ in Android Bluetooth settings. Close other OBD apps.")
                        OutlinedButton(onClick = { startActivity(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)) }) { Text("Bluetooth settings") }
                        Button(onClick = { if(Build.VERSION.SDK_INT >= 31) permission.launch(Manifest.permission.BLUETOOTH_CONNECT) else model.refresh() }, enabled = editable) { Text("Grant permission / refresh devices") }
                        state.devices.forEach { device ->
                            OutlinedButton(onClick = { model.connect(device.address) }, enabled = editable, modifier = Modifier.fillMaxWidth()) { Text("${device.name}\n${device.address}") }
                        }
                        if(state.connected || state.busy) Button(onClick = model::disconnect) { Text("Disconnect / cancel connection") }
                        if(state.supported != null) Text("${Obd.pids.count { it.code in state.supported!! }} of ${Obd.pids.size} app PIDs advertised by this ECU. Other readings remain unavailable.")
                        Text("For a cold-start guide, connect with ignition on and engine off after cooling. For other guides, follow the guide's warm-up steps. Use while parked or let a passenger operate the app.")
                    }
                    1 -> {
                        Text("What should I do with the car?", style = MaterialTheme.typography.headlineSmall)
                        Text("Choose the question's operating condition. Neutral RPM is not a substitute for road load. These are conservative collection guides, not Nissan factory test procedures.")
                        Guide.entries.forEach { item -> OutlinedButton(onClick = { guide = item }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text((if(guide == item) "✓ " else "") + item.title) } }
                        Text(guide.title, style = MaterialTheme.typography.titleLarge)
                        Text(guide.instructions)
                        Text("Why: ${guide.purpose}")
                        Text("Stop conditions: ${guide.abort}", color = MaterialTheme.colorScheme.error)
                        Text("Required: " + guide.required.joinToString { code -> Obd.pids.first { it.code == code }.name })
                        Row { Checkbox(checked = ready, onCheckedChange = { ready = it }, enabled = !state.busy); Text("I am parked, have read the steps and stop conditions, and will not handle the phone while driving.", Modifier.padding(top = 8.dp)) }
                        Button(onClick = { model.monitor(guide); ready = false }, enabled = ready && state.connected && !state.busy && !state.aiBusy) { Text("Start guided recording") }
                        state.guidance?.let { progress ->
                            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
                                Text(state.guide?.title ?: "Recording", style = MaterialTheme.typography.titleMedium)
                                Text(progress.text)
                                Text("Approximate qualifying coverage: ${progress.seconds} s • ${progress.samples} readings", style = MaterialTheme.typography.bodySmall)
                            } }
                        }
                        if(state.vehicleId == "350z-2003") Text("2003 DE: unknown injectors and calibration. Identify them and verify matching calibration before high-load fueling evaluation.", color = MaterialTheme.colorScheme.error)
                        Text("High-load / dyno testing", style = MaterialTheme.typography.titleMedium)
                        Text("Arrange a qualified tuner and controlled dyno with appropriate instrumentation. No full-throttle road guide is offered. Generic OBD data cannot establish safe fueling under load.")
                    }
                    2 -> {
                        Text("Live engine data", style = MaterialTheme.typography.headlineSmall)
                        Button(onClick = { model.monitor() }, enabled = state.connected && !state.busy && !state.aiBusy) { Text("Record without a guide") }
                        Text("Keep the app open. PIDs are polled sequentially. — means unavailable; last values remain visible after stopping. Fuel-system status is a bitmask: 1 cold/open loop, 2 closed loop, 4 load/decel open loop, 8 fault open loop, 16 closed loop with fault. Commanded equivalence ratio is not measured AFR.", style = MaterialTheme.typography.bodySmall)
                        Obd.pids.forEach { pid -> Card(Modifier.fillMaxWidth()) { Row(Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(pid.name, Modifier.weight(1f)); Text(state.values[pid.code]?.let { "%.1f %s".format(it, pid.unit) } ?: "—")
                        } } }
                        Text("Anomaly checks", style = MaterialTheme.typography.titleLarge)
                        Text("Heuristic thresholds; consider engine state, closed-loop status and vehicle specifications.")
                        if(state.alerts.isEmpty()) Text(if(state.samples == 0) "Start recording to evaluate readings." else "No threshold alerts in the latest sample.")
                        state.alerts.forEach { Text(it, color = MaterialTheme.colorScheme.error) }
                    }
                    3 -> {
                        Text("Trouble codes", style = MaterialTheme.typography.headlineSmall)
                        Text("Read stored and pending powertrain codes. Stop recording before scanning. Codes and readings stay in the same session for AI analysis.")
                        Button(onClick = model::scan, enabled = state.connected && !state.busy && !state.aiBusy) { Text("Scan DTCs") }
                        state.codes.forEach { code -> Card(Modifier.fillMaxWidth()) { Text(code, Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge) } }
                        Text("A DTC identifies a detected condition, not necessarily a failed part. No codes are cleared.")
                        Text("Service, relearns & performance tuning", style = MaterialTheme.typography.titleLarge)
                        Text("Not enabled: verified ECU-specific procedures and compatible write access have not been established for these cars. This app sends only adapter setup and OBD read requests. Use the supported UpRev workflow for the 370Z's calibration. AI cannot execute vehicle commands.")
                    }
                    4 -> {
                        Text("Your garage", style = MaterialTheme.typography.headlineSmall)
                        Vehicles.all.forEach { vehicle -> OutlinedButton(onClick = { model.selectVehicle(vehicle.id) }, enabled = editable, modifier = Modifier.fillMaxWidth()) { Text((if(state.vehicleId == vehicle.id) "✓ " else "") + vehicle.title) } }
                        Text(Vehicles.get(state.vehicleId).details)
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
                        Text("Ask about your diagnostics", style = MaterialTheme.typography.headlineSmall)
                        Text("Stop recording and park before analysis. Data goes directly to OpenAI using your API account; API charges are separate from ChatGPT subscriptions. Nothing is sent until you review and confirm. Answers are advisory and cannot change ECU settings.")
                        OutlinedTextField(value = state.question, onValueChange = model::question, label = { Text("Question or follow-up (optional)") }, minLines = 2, modifier = Modifier.fillMaxWidth(), enabled = !state.aiBusy)
                        Button(onClick = model::prepareAnalysis, enabled = !state.busy && !state.aiBusy && state.hasApiKey) { Text("Review data & analyze") }
                        if(state.aiBusy) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("Preparing / requesting analysis…"); OutlinedButton(onClick = model::cancelAnalysis) { Text("Cancel") } }
                        if(state.aiError.isNotEmpty()) Text(state.aiError)
                        if(state.analysisSession.isNotEmpty()) Text("Analysis of: ${state.analysisSession}", style = MaterialTheme.typography.bodySmall)
                        state.conversation.forEach { (question, answer) -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(if(question.isBlank()) "Diagnostic assessment" else question, style = MaterialTheme.typography.titleMedium)
                            SelectionContainer { Text(answer) }
                        } } }
                        Text("Personal API settings", style = MaterialTheme.typography.titleLarge)
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
