package com.leadflow.app.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.leadflow.app.LeadViewModel
import com.leadflow.app.data.Prefs

@Composable
fun SettingsScreen(vm: LeadViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    var template by remember { mutableStateOf(vm.prefs.template) }
    var reminder by remember { mutableStateOf(vm.prefs.delayReminder) }
    val versionName = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull()?.takeIf { it.isNotBlank() } ?: "Version unavailable"
    }

    LfScaffold(title = "Settings", onBack = onBack) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Default WhatsApp message", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = template,
                onValueChange = { template = it },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
                supportingText = { Text("Write {Business Name} where the lead's name should appear.") }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    vm.prefs.template = template.ifBlank { Prefs.DEFAULT_TEMPLATE }
                    template = vm.prefs.template
                    Toast.makeText(context, "Template saved", Toast.LENGTH_SHORT).show()
                }) { Text("Save") }
                OutlinedButton(onClick = { template = Prefs.DEFAULT_TEMPLATE }) { Text("Reset") }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Add delay reminder before sending", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Shows a reminder to wait 30-90 sec between messages.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(checked = reminder, onCheckedChange = { reminder = it; vm.prefs.delayReminder = it })
            }

            HorizontalDivider()

            Text("About LeadFlow", style = MaterialTheme.typography.titleLarge)
            Text(
                "Discover local businesses, organize prospects and manage follow-ups in one place.",
                style = MaterialTheme.typography.bodyMedium
            )
            Text("LeadFlow · $versionName", style = MaterialTheme.typography.labelLarge)
            Text("What you can do", style = MaterialTheme.typography.titleSmall)
            Text("• Discover nearby businesses by category and location")
            Text("• Save prospects and manage pipeline status, priority and follow-up dates")
            Text("• Record notes and activity history")
            Text("• Filter, sort and export saved leads")
            Text("• Open email, website and WhatsApp actions manually")
            Text("Data and privacy", style = MaterialTheme.typography.titleSmall)
            Text(
                "Place discovery currently uses OpenStreetMap Nominatim and Overpass. Search results are public map listings; ratings, review counts and customer review text are not currently provided by this integration.",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "Saved leads and preferences are stored locally on this device. WhatsApp messages are opened for you to review and send; LeadFlow does not send messages automatically.",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "OpenStreetMap data is © OpenStreetMap contributors. Map data is available under the Open Database License (ODbL).",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
