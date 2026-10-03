package com.leadflow.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    totalLeads: Int,
    onAdd: () -> Unit,
    onView: () -> Unit,
    onExport: () -> Unit,
    onSettings: () -> Unit
) {
    LfScaffold(title = "LeadFlow") { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(24.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$totalLeads", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
                    Text("leads saved")
                }
            }
            Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Text("Add Lead Manually") }
            Button(onClick = onView, modifier = Modifier.fillMaxWidth()) { Text("View All Leads") }
            Button(onClick = onExport, modifier = Modifier.fillMaxWidth()) { Text("Export Data") }
            OutlinedButton(onClick = onSettings, modifier = Modifier.fillMaxWidth()) { Text("Settings") }
            Spacer(Modifier.weight(1f))
            Text(
                "You send every message yourself. LeadFlow never auto-sends.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
