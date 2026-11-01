package com.leadflow.app.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.leadflow.app.LeadViewModel
import com.leadflow.app.util.Validators

@Composable
fun AddLeadScreen(vm: LeadViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var website by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }

    val nameError = submitted && name.isBlank()
    val showPhoneError = submitted || phone.length == 10 || (phone.isNotEmpty() && phone[0] !in '6'..'9')
    val phoneError = if (showPhoneError) Validators.phoneError(phone) else null

    LfScaffold(title = "Add Lead", onBack = onBack) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Business Name *") },
                isError = nameError,
                supportingText = { if (nameError) Text("Required") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = address, onValueChange = { address = it },
                label = { Text("Address / Area") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = phone,
                onValueChange = { new -> phone = new.filter { it in '0'..'9' }.take(10) },
                label = { Text("Phone Number *") },
                prefix = { Text("+91 ") },
                isError = phoneError != null,
                supportingText = { Text(phoneError ?: "10-digit mobile starting with 6, 7, 8 or 9") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it.trim() },
                label = { Text("Email (optional)") },
                isError = email.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches(),
                supportingText = { if (email.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) Text("Enter a valid email address") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = website,
                onValueChange = { website = it.trim() },
                label = { Text("Website (optional)") },
                supportingText = { Text("Include https:// or http://") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = notes, onValueChange = { notes = it },
                label = { Text("Notes") },
                minLines = 3, modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    submitted = true
                    if (name.isNotBlank() && Validators.phoneError(phone) == null && (email.isBlank() || android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches())) {
                        vm.addLead(name.trim(), address.trim(), phone, notes.trim(), email.trim(), website.trim()) { added ->
                            if (added) {
                                Toast.makeText(context, "Lead saved", Toast.LENGTH_SHORT).show()
                                onBack()
                            } else {
                                Toast.makeText(context, "This business is already saved with the same address", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save Lead") }
        }
    }
}
