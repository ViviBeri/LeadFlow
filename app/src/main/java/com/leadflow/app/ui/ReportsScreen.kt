package com.leadflow.app.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.leadflow.app.LeadViewModel
import com.leadflow.app.data.Lead
import java.util.Calendar

@Composable
fun ReportsScreen(vm: LeadViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    var allLeads by remember { mutableStateOf<List<Lead>>(emptyList()) }
    var range by remember { mutableStateOf("30 days") }
    LaunchedEffect(Unit) { allLeads = vm.allLeads() }

    val now = Calendar.getInstance()
    val start = Calendar.getInstance().apply {
        when (range) {
            "Today" -> Unit
            "7 days" -> add(Calendar.DAY_OF_YEAR, -6)
            "30 days" -> add(Calendar.DAY_OF_YEAR, -29)
            else -> timeInMillis = 0L
        }
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val filtered = if (range == "All time") allLeads else allLeads.filter { it.createdAt in start..now.timeInMillis }
    val total = filtered.size
    val converted = filtered.count { it.status == "Converted" }
    val active = total - filtered.count { it.status == "Not Interested" }
    val rate = if (active > 0) converted * 100 / active else 0
    val csv = buildString {
        append("Metric,Count\r\nRange,").append(range).append("\r\n")
        append("Total leads,").append(total).append("\r\n")
        listOf("New", "Contacted", "Interested", "Converted", "Not Interested").forEach { s ->
            append(s).append(",").append(filtered.count { it.status == s }).append("\r\n")
        }
        append("High priority,").append(filtered.count { it.priority == "High" }).append("\r\n")
        append("Conversion rate (%),").append(rate).append("\r\n")
    }

    LfScaffold(title = "Lead Performance Report", onBack = onBack) { padding ->
        Column(Modifier.padding(padding).padding(20.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Lead creation period", style = MaterialTheme.typography.titleMedium)
            listOf("Today", "7 days", "30 days", "All time").chunked(2).forEach { options ->
                androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.forEach { option ->
                        if (range == option) Button(onClick = { range = option }, modifier = Modifier.weight(1f)) { Text(option) }
                        else OutlinedButton(onClick = { range = option }, modifier = Modifier.weight(1f)) { Text(option) }
                    }
                }
            }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("$total", style = MaterialTheme.typography.headlineLarge)
                    Text("Leads created in selected period")
                    listOf("New", "Contacted", "Interested", "Converted", "Not Interested").forEach { s ->
                        Text("$s: ${filtered.count { it.status == s }}")
                    }
                    Text("High priority: ${filtered.count { it.priority == "High" }}")
                    Text("Conversion rate: $rate%", style = MaterialTheme.typography.titleLarge)
                    Text("Converted ÷ leads excluding Not Interested", style = MaterialTheme.typography.bodySmall)
                }
            }
            Button(onClick = {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_SUBJECT, "LeadFlow performance report - $range")
                    putExtra(Intent.EXTRA_TEXT, csv)
                }
                context.startActivity(Intent.createChooser(send, "Share CSV summary"))
            }, enabled = filtered.isNotEmpty(), modifier = Modifier.fillMaxWidth()) { Text("Share CSV Summary") }
            Text("Based on saved leads and their current pipeline status.", style = MaterialTheme.typography.bodySmall)
        }
    }
}
