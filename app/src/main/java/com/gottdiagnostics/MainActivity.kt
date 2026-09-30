package com.gottdiagnostics

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
        val scope = rememberCoroutineScope()
        val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if(it) model.refresh() else message = "Allow Nearby devices permission in Android app settings." }
        Column(Modifier.safeDrawingPadding().padding(16.dp)) {
            Text("GOTT", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
            Text("DIAGNOSTICS", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(12.dp))
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
                Text(state.status, style = MaterialTheme.typography.titleMedium)
                if(state.session != null) Text("${state.samples} samples • saved on this phone", style = MaterialTheme.typography.bodySmall)
            } }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("Connect", "Live", "Codes", "Session").forEachIndexed { i, title -> TextButton(onClick = { tab = i }) { Text(title, color = if(tab == i) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) } }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when(tab) {
                    0 -> {
                        Text("OBDLink MX+", style = MaterialTheme.typography.headlineSmall)
                        Text("Plug the adapter into the vehicle, turn ignition on, and pair OBDLink MX+ in Android Bluetooth settings. Close other OBD apps before connecting.")
                        OutlinedButton(onClick = { startActivity(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)) }) { Text("Bluetooth settings") }
                        Button(onClick = { if(Build.VERSION.SDK_INT >= 31) permission.launch(Manifest.permission.BLUETOOTH_CONNECT) else model.refresh() }, enabled = !state.busy && !state.connected) { Text("Grant permission / refresh devices") }
                        state.devices.forEach { device ->
                            OutlinedButton(onClick = { model.connect(device.address) }, enabled = !state.busy && !state.connected, modifier = Modifier.fillMaxWidth()) { Text("${device.name}\n${device.address}") }
                        }
                        if(state.connected || state.busy) Button(onClick = model::disconnect) { Text("Disconnect / cancel") }
                        Text("Use while parked or have a passenger operate the app. Generic engine OBD-II only; module coverage depends on the vehicle.", style = MaterialTheme.typography.bodySmall)
                    }
                    1 -> {
                        Text("Live engine data", style = MaterialTheme.typography.headlineSmall)
                        Button(onClick = model::monitor, enabled = state.connected && !state.busy) { Text("Start monitoring") }
                        if(state.monitoring) OutlinedButton(onClick = model::stopMonitoring) { Text("Stop monitoring") }
                        Text("Keep the app open during recording. Unsupported PIDs show —. Last values remain visible after disconnect.", style = MaterialTheme.typography.bodySmall)
                        Obd.pids.forEach { pid -> Card(Modifier.fillMaxWidth()) { Row(Modifier.padding(14.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(pid.name); Text(state.values[pid.code]?.let { "%.1f %s".format(it, pid.unit) } ?: "—")
                        } } }
                        Text("Anomaly checks", style = MaterialTheme.typography.titleLarge)
                        Text("Heuristic thresholds; consider engine state and vehicle specifications.")
                        if(state.alerts.isEmpty()) Text(if(state.samples == 0) "Start monitoring to evaluate readings." else "No threshold alerts in the latest sample.")
                        state.alerts.forEach { Text(it, color = MaterialTheme.colorScheme.error) }
                    }
                    2 -> {
                        Text("Trouble codes", style = MaterialTheme.typography.headlineSmall)
                        Text("Read stored and pending powertrain codes. Stop monitoring before scanning. Codes are recorded in your session.")
                        Button(onClick = model::scan, enabled = state.connected && !state.busy) { Text("Scan DTCs") }
                        state.codes.forEach { code -> Card(Modifier.fillMaxWidth()) { Text(code, Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge) } }
                        Text("A DTC identifies a detected condition, not necessarily a failed part. This app does not clear codes.", style = MaterialTheme.typography.bodySmall)
                    }
                    3 -> {
                        Text("Vehicle & session", style = MaterialTheme.typography.headlineSmall)
                        OutlinedTextField(value = state.profile, onValueChange = model::profile, label = { Text("Year, make, model, engine, symptoms") }, modifier = Modifier.fillMaxWidth(), minLines = 3, enabled = !state.connected && !state.busy)
                        Text("Save the profile before connecting. Each connection creates a timestamped local JSONL log with raw responses, PID samples, DTCs, and alerts.")
                        Text("Export your current session, or the most recent saved session after restarting the app. The ZIP includes a ChatGPT analysis prompt. Review vehicle details before sharing.")
                        Button(enabled = !state.busy, onClick = {
                            scope.launch {
                                try {
                                    val file = model.export()
                                    val uri = FileProvider.getUriForFile(this@MainActivity, "$packageName.files", file)
                                    val intent = Intent(Intent.ACTION_SEND).apply { type = "application/zip"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); clipData = android.content.ClipData.newRawUri("Diagnostic session", uri) }
                                    startActivity(Intent.createChooser(intent, "Share diagnostics for analysis"))
                                } catch(e: Exception) { message = e.message ?: "Export failed" }
                            }
                        }) { Text("Export diagnostic ZIP") }
                        Text("Attach the ZIP in ChatGPT and use analysis-request.txt as your prompt. No account or API key is needed in this app.")
                    }
                }
                if(message.isNotEmpty()) Text(message, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
