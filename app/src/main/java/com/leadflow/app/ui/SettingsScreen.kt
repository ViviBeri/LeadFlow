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
        }
    }
}
