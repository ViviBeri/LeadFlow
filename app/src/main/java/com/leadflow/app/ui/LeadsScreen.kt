package com.leadflow.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.leadflow.app.LeadViewModel
import com.leadflow.app.data.Lead
import com.leadflow.app.util.WhatsApp

@Composable
fun LeadsScreen(vm: LeadViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val leads by vm.leads.collectAsState()
    val query by vm.searchText.collectAsState()
    var toDelete by remember { mutableStateOf<Lead?>(null) }
    var toSend by remember { mutableStateOf<Lead?>(null) }

    fun openWhatsApp(lead: Lead) = WhatsApp.open(context, lead, vm.prefs.template)

    LfScaffold(title = "All Leads", onBack = onBack) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = { vm.setQuery(it) },
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                singleLine = true,
                placeholder = { Text("Search by name or area") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { vm.setQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                }
            )
            if (leads.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(if (query.isBlank()) "No leads yet" else "No matches")
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(leads, key = { it.id }) { lead ->
                        LeadRow(
                            lead = lead,
                            onSend = {
                                if (vm.prefs.delayReminder) toSend = lead else openWhatsApp(lead)
                            },
                            onDelete = { toDelete = lead }
                        )
                    }
                }
            }
        }
    }

    toDelete?.let { lead ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Delete lead?") },
            text = { Text("${lead.businessName} will be removed permanently.") },
            confirmButton = {
                TextButton(onClick = { vm.deleteLead(lead); toDelete = null }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Cancel") } }
        )
    }

    toSend?.let { lead ->
        AlertDialog(
            onDismissRequest = { toSend = null },
            title = { Text("Wait between messages") },
            text = {
                Text("To keep your WhatsApp account safe, wait 30-90 seconds between messages to different businesses.")
            },
            confirmButton = {
                TextButton(onClick = { openWhatsApp(lead); toSend = null }) { Text("Open WhatsApp") }
            },
            dismissButton = { TextButton(onClick = { toSend = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun LeadRow(lead: Lead, onSend: () -> Unit, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    lead.businessName,
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(lead.phone, style = MaterialTheme.typography.bodyMedium)
            }
            if (lead.address.isNotBlank()) {
                Text(lead.address, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onSend, modifier = Modifier.weight(1f)) { Text("Send WhatsApp") }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete lead")
                }
            }
        }
    }
}
