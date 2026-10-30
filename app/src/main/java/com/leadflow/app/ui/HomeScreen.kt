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
    newLeads: Int,
    contactedLeads: Int,
    interestedLeads: Int,
    convertedLeads: Int,
    notInterestedLeads: Int,
    onAdd: () -> Unit,
    onView: () -> Unit,
    onExport: () -> Unit,
    onReports: () -> Unit,
    onSettings: () -> Unit,
    onDiscover: () -> Unit
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
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Sales pipeline", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("New: $newLeads   ·   Contacted: $contactedLeads")
                    Text("Interested: $interestedLeads   ·   Converted: $convertedLeads")
                    Text("Not interested: $notInterestedLeads")
                    val conversionRate = if (totalLeads == 0) 0 else (convertedLeads * 100) / totalLeads
                    Text("Conversion rate: $conversionRate%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
            Button(onClick = onDiscover, modifier = Modifier.fillMaxWidth()) { Text("Discover Places") }
            Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Text("Add Lead Manually") }
            Button(onClick = onView, modifier = Modifier.fillMaxWidth()) { Text("View All Leads") }
            Button(onClick = onExport, modifier = Modifier.fillMaxWidth()) { Text("Export Data") }
            OutlinedButton(onClick = onReports, modifier = Modifier.fillMaxWidth()) { Text("Performance Reports") }
            OutlinedButton(onClick = onSettings, modifier = Modifier.fillMaxWidth()) { Text("Settings") }
            Spacer(Modifier.weight(1f))
            Text(
                "Discover local places and manage business leads. Messages are always sent manually.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
