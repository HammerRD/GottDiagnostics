package com.gottdiagnostics

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable internal fun AddVehicleForm(onDismiss: () -> Unit, onSave: (String, String, String, String, String) -> String?) {
    var year by rememberSaveable { mutableStateOf("") }
    var make by rememberSaveable { mutableStateOf("") }
    var model by rememberSaveable { mutableStateOf("") }
    var paint by rememberSaveable { mutableStateOf("") }
    var details by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    BackHandler(onBack = onDismiss)
    Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Add a car", style = MaterialTheme.typography.headlineLarge)
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Save a profile, then add its photo in Garage. OBD readings depend on the vehicle's supported PIDs.")
                OutlinedTextField(year, { year = it.filter(Char::isDigit).take(4) }, label = { Text("Year *") }, singleLine = true)
                OutlinedTextField(make, { make = it.take(60) }, label = { Text("Make *") }, singleLine = true)
                OutlinedTextField(model, { model = it.take(100) }, label = { Text("Model / trim *") }, singleLine = true)
                OutlinedTextField(paint, { paint = it.take(40) }, label = { Text("Paint color") }, singleLine = true)
                OutlinedTextField(details, { details = it.take(4000) }, label = { Text("Engine, transmission, fuel, modifications / tune") }, minLines = 3)
                Text("Leave unknown details unspecified. You can add corrections and symptoms to the profile afterward.")
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onDismiss) { Text("Cancel") }
            Button(onClick = {
            error = onSave(year, make, model, paint, details)
            if(error == null) onDismiss()
        }, enabled = year.isNotBlank() && make.isNotBlank() && model.isNotBlank()) { Text("Save car") }
        }
    }
}
