package com.leadflow.app.ui

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.leadflow.app.LeadViewModel
import com.leadflow.app.data.Lead
import com.leadflow.app.util.WhatsApp
import com.leadflow.app.util.Validators

@Composable
fun LeadsScreen(vm: LeadViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val leads by vm.leads.collectAsState()
    val query by vm.searchText.collectAsState()
    var toDelete by remember { mutableStateOf<Lead?>(null) }
    var toSend by remember { mutableStateOf<Lead?>(null) }
    var toEditNotes by remember { mutableStateOf<Lead?>(null) }
    var toEditContact by remember { mutableStateOf<Lead?>(null) }
    var editedName by remember { mutableStateOf("") }
    var editedAddress by remember { mutableStateOf("") }
    var editedPhone by remember { mutableStateOf("") }
    var editedEmail by remember { mutableStateOf("") }
    var editedWebsite by remember { mutableStateOf("") }
    var editedNotes by remember { mutableStateOf("") }
    var toEditTags by remember { mutableStateOf<Lead?>(null) }
    var editedTags by remember { mutableStateOf("") }
    var toAddActivity by remember { mutableStateOf<Lead?>(null) }
    var toViewActivity by remember { mutableStateOf<Lead?>(null) }
    var activityType by remember { mutableStateOf("Call") }
    var activityDetails by remember { mutableStateOf("") }
    var followUpFilter by remember { mutableStateOf("All") }
    var priorityFilter by remember { mutableStateOf("All") }
    var priorityFilterExpanded by remember { mutableStateOf(false) }
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    val activeLeads = leads.filterNot { it.status == "Converted" || it.status == "Not Interested" }
    val overdueCount = activeLeads.count { it.followUpDate.isNotBlank() && it.followUpDate < today }
    val dueTodayCount = activeLeads.count { it.followUpDate == today }
    val upcomingCount = activeLeads.count { it.followUpDate.isNotBlank() && it.followUpDate > today }
    val followUpLeads = when (followUpFilter) {
        "Overdue" -> leads.filter { it.status != "Converted" && it.status != "Not Interested" && it.followUpDate.isNotBlank() && it.followUpDate < today }
        "Due today" -> leads.filter { it.status != "Converted" && it.status != "Not Interested" && it.followUpDate == today }
        "Upcoming" -> leads.filter { it.status != "Converted" && it.status != "Not Interested" && it.followUpDate.isNotBlank() && it.followUpDate > today }
        else -> leads
    }
    val visibleLeads = if (priorityFilter == "All") followUpLeads
        else followUpLeads.filter { it.priority == priorityFilter }

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
            if (leads.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FollowUpSummaryCard("Overdue", overdueCount, followUpFilter == "Overdue", Modifier.weight(1f)) { followUpFilter = if (followUpFilter == "Overdue") "All" else "Overdue" }
                    FollowUpSummaryCard("Due today", dueTodayCount, followUpFilter == "Due today", Modifier.weight(1f)) { followUpFilter = if (followUpFilter == "Due today") "All" else "Due today" }
                    FollowUpSummaryCard("Upcoming", upcomingCount, followUpFilter == "Upcoming", Modifier.weight(1f)) { followUpFilter = if (followUpFilter == "Upcoming") "All" else "Upcoming" }
                }
            }
            if (followUpFilter != "All") {
                TextButton(onClick = { followUpFilter = "All" }, modifier = Modifier.padding(start = 8.dp)) {
                    Text("Showing $followUpFilter · Clear filter")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Priority:", style = MaterialTheme.typography.labelLarge)
                Box {
                    TextButton(onClick = { priorityFilterExpanded = true }) { Text(priorityFilter) }
                    DropdownMenu(
                        expanded = priorityFilterExpanded,
                        onDismissRequest = { priorityFilterExpanded = false }
                    ) {
                        listOf("All", "High", "Medium", "Low").forEach { priority ->
                            DropdownMenuItem(
                                text = { Text(priority) },
                                onClick = {
                                    priorityFilter = priority
                                    priorityFilterExpanded = false
                                }
                            )
                        }
                    }
                }
                if (priorityFilter != "All") {
                    TextButton(onClick = { priorityFilter = "All" }) { Text("Clear") }
                }
            }
            if (leads.isEmpty() || visibleLeads.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(when {
                        leads.isEmpty() -> if (query.isBlank()) "No leads yet" else "No matches"
                        followUpFilter != "All" -> "No $followUpFilter follow-ups"
                        else -> "No $priorityFilter priority leads"
                    })
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(visibleLeads, key = { it.id }) { lead ->
                        LeadRow(
                            lead = lead,
                            onSend = {
                                if (vm.prefs.delayReminder) toSend = lead else openWhatsApp(lead)
                            },
                            onDelete = { toDelete = lead },
                            onEditContact = { editedName = lead.businessName; editedAddress = lead.address; editedPhone = lead.phone; editedEmail = lead.email; editedWebsite = lead.website; toEditContact = lead },
                            onEmail = { email ->
                                runCatching { context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email"))) }
                                    .onFailure { Toast.makeText(context, "No email app available", Toast.LENGTH_SHORT).show() }
                            },
                            onWebsite = { website ->
                                val url = if (website.startsWith("http://") || website.startsWith("https://")) website else "https://$website"
                                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                                    .onFailure { Toast.makeText(context, "Unable to open website", Toast.LENGTH_SHORT).show() }
                            },
                            onEditNotes = { editedNotes = lead.notes; toEditNotes = lead },
                            onEditTags = { editedTags = lead.tags; toEditTags = lead },
                            onAddActivity = { activityType = "Call"; activityDetails = ""; toAddActivity = lead },
                            onViewActivity = { toViewActivity = lead },
                            onStatusChange = { status -> vm.updateLeadStatus(lead, status) },
                            onPriorityChange = { priority -> vm.updateLeadPriority(lead, priority) },
                            onFollowUp = { date -> vm.updateLeadFollowUpDate(lead, date) },
                            onPickFollowUp = {
                                val calendar = Calendar.getInstance()
                                DatePickerDialog(
                                    context,
                                    { _, year, month, day ->
                                        val date = String.format(java.util.Locale.US, "%04d-%02d-%02d", year, month + 1, day)
                                        vm.updateLeadFollowUpDate(lead, date)
                                    },
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }
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


    toEditContact?.let { lead ->
        val phoneError = Validators.phoneError(editedPhone)
        val emailError = editedEmail.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(editedEmail).matches()
        AlertDialog(
            onDismissRequest = { toEditContact = null },
            title = { Text("Edit contact details") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editedName,
                        onValueChange = { editedName = it },
                        label = { Text("Business name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editedAddress,
                        onValueChange = { editedAddress = it },
                        label = { Text("Address / Area") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editedEmail,
                        onValueChange = { editedEmail = it.trim() },
                        label = { Text("Email (optional)") },
                        isError = emailError,
                        supportingText = { if (emailError) Text("Enter a valid email address") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editedWebsite,
                        onValueChange = { editedWebsite = it.trim() },
                        label = { Text("Website (optional)") },
                        supportingText = { Text("Include https:// or http://") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editedPhone,
                        onValueChange = { editedPhone = it.filter { ch -> ch in '0'..'9' }.take(10) },
                        label = { Text("Phone number *") },
                        prefix = { Text("+91 ") },
                        isError = phoneError != null,
                        supportingText = { Text(phoneError ?: "10-digit mobile number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.updateLeadContactDetails(lead, editedName.trim(), editedAddress.trim(), editedPhone, editedEmail.trim(), editedWebsite.trim())
                        toEditContact = null
                    },
                    enabled = editedName.isNotBlank() && phoneError == null && !emailError
                ) { Text("Save changes") }
            },
            dismissButton = { TextButton(onClick = { toEditContact = null }) { Text("Cancel") } }
        )
    }

    toEditNotes?.let { lead ->
        AlertDialog(
            onDismissRequest = { toEditNotes = null },
            title = { Text("Edit notes") },
            text = {
                OutlinedTextField(
                    value = editedNotes,
                    onValueChange = { editedNotes = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    placeholder = { Text("Add call notes, requirements or next steps") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.updateLeadNotes(lead, editedNotes.trim())
                    toEditNotes = null
                }) { Text("Save notes") }
            },
            dismissButton = { TextButton(onClick = { toEditNotes = null }) { Text("Cancel") } }
        )
    }


    toEditTags?.let { lead ->
        AlertDialog(
            onDismissRequest = { toEditTags = null },
            title = { Text("Tags · ${lead.businessName}") },
            text = {
                OutlinedTextField(
                    value = editedTags,
                    onValueChange = { editedTags = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    placeholder = { Text("e.g. GST, Restaurant, High Priority") },
                    supportingText = { Text("Separate tags with commas") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val normalized = editedTags.split(",")
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .distinctBy { it.lowercase(Locale.ROOT) }
                        .joinToString(", ")
                    vm.updateLeadTags(lead, normalized)
                    toEditTags = null
                }) { Text("Save tags") }
            },
            dismissButton = { TextButton(onClick = { toEditTags = null }) { Text("Cancel") } }
        )
    }

    toViewActivity?.let { lead ->
        val entries = lead.activityLog.lines().filter { it.isNotBlank() }.asReversed()
        AlertDialog(
            onDismissRequest = { toViewActivity = null },
            title = { Text("Activity history · ${lead.businessName}") },
            text = {
                if (entries.isEmpty()) {
                    Text("No activity has been logged for this lead yet.")
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("${entries.size} entr${if (entries.size == 1) "y" else "ies"} · newest first",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary)
                        entries.forEachIndexed { index, entry ->
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(entry, style = MaterialTheme.typography.bodyMedium)
                                if (index != entries.lastIndex) {
                                    androidx.compose.material3.HorizontalDivider()
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { toViewActivity = null }) { Text("Close") }
            }
        )
    }

    toAddActivity?.let { lead ->
        AlertDialog(
            onDismissRequest = { toAddActivity = null },
            title = { Text("Log activity · ${lead.businessName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Activity type")
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("Call", "WhatsApp", "Meeting", "Email", "Other").forEach { type ->
                            TextButton(onClick = { activityType = type }) {
                                Text(if (activityType == type) "✓ $type" else type)
                            }
                        }
                    }
                    OutlinedTextField(
                        value = activityDetails,
                        onValueChange = { activityDetails = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        placeholder = { Text("What happened? Add outcome or next step") }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                    val details = activityDetails.trim()
                    val entry = "[$timestamp] $activityType" + if (details.isBlank()) "" else ": $details"
                    vm.addLeadActivity(lead, entry)
                    toAddActivity = null
                }, enabled = activityDetails.isNotBlank()) { Text("Save activity") }
            },
            dismissButton = { TextButton(onClick = { toAddActivity = null }) { Text("Cancel") } }
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
private fun LeadRow(lead: Lead, onSend: () -> Unit, onDelete: () -> Unit, onEditContact: () -> Unit, onEmail: (String) -> Unit, onWebsite: (String) -> Unit, onEditNotes: () -> Unit, onEditTags: () -> Unit, onAddActivity: () -> Unit, onViewActivity: () -> Unit, onStatusChange: (String) -> Unit, onPriorityChange: (String) -> Unit, onFollowUp: (String) -> Unit, onPickFollowUp: () -> Unit) {
    var statusMenuExpanded by remember { mutableStateOf(false) }
    var priorityMenuExpanded by remember { mutableStateOf(false) }
    val statuses = listOf("New", "Contacted", "Interested", "Converted", "Not Interested")
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
            if (lead.email.isNotBlank()) TextButton(onClick = { onEmail(lead.email) }) { Text("Email · ${lead.email}", maxLines = 1, overflow = TextOverflow.Ellipsis) }
            if (lead.website.isNotBlank()) TextButton(onClick = { onWebsite(lead.website) }) { Text("Open website · ${lead.website}", maxLines = 1, overflow = TextOverflow.Ellipsis) }
            TextButton(onClick = onEditContact) { Text("Edit contact details") }
            if (lead.address.isNotBlank()) {
                Text(lead.address, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (lead.notes.isNotBlank()) {
                Text(lead.notes, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            TextButton(onClick = onEditNotes) {
                Text(if (lead.notes.isBlank()) "Add notes" else "Edit notes")
            }
            if (lead.tags.isNotBlank()) {
                Text("Tags: ${lead.tags}", style = MaterialTheme.typography.labelSmall)
            }
            TextButton(onClick = onEditTags) {
                Text(if (lead.tags.isBlank()) "Add tags" else "Edit tags")
            }
            if (lead.activityLog.isNotBlank()) {
                Text("Recent activity:", style = MaterialTheme.typography.labelMedium)
                Text(lead.activityLog.lines().filter { it.isNotBlank() }.takeLast(2).asReversed().joinToString("\n"), style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = onViewActivity) {
                    Text("View full history (${lead.activityLog.lines().count { it.isNotBlank() }})")
                }
            }
            TextButton(onClick = onAddActivity) { Text("Add activity") }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Priority: " + lead.priority, style = MaterialTheme.typography.labelMedium)
                Box {
                    TextButton(onClick = { priorityMenuExpanded = true }) { Text("Change") }
                    DropdownMenu(
                        expanded = priorityMenuExpanded,
                        onDismissRequest = { priorityMenuExpanded = false }
                    ) {
                        listOf("High", "Medium", "Low").forEach { priority ->
                            DropdownMenuItem(
                                text = { Text(priority) },
                                onClick = {
                                    onPriorityChange(priority)
                                    priorityMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Status: " + lead.status, style = MaterialTheme.typography.labelMedium)
                Box {
                    TextButton(onClick = { statusMenuExpanded = true }) { Text("Change") }
                    DropdownMenu(
                        expanded = statusMenuExpanded,
                        onDismissRequest = { statusMenuExpanded = false }
                    ) {
                        statuses.forEach { status ->
                            DropdownMenuItem(
                                text = { Text(status) },
                                onClick = {
                                    onStatusChange(status)
                                    statusMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (lead.followUpDate.isBlank()) "Follow-up: Not set" else "Follow-up: ${lead.followUpDate}",
                    style = MaterialTheme.typography.bodySmall
                )
                TextButton(onClick = onPickFollowUp) {
                    Text(if (lead.followUpDate.isBlank()) "Set date" else "Change date")
                }
                if (lead.followUpDate.isNotBlank()) {
                    TextButton(onClick = { onFollowUp("") }) { Text("Clear") }
                }
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


@Composable
private fun FollowUpSummaryCard(label: String, count: Int, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(modifier.clickable(onClick = onClick), border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(count.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}
