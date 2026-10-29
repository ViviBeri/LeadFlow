package com.leadflow.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun DiscoveryScreen(onBack: () -> Unit) {
    var category by remember { mutableStateOf("Restaurants") }
    var location by remember { mutableStateOf("") }
    val context = LocalContext.current

    LfScaffold(title = "Discover Places", onBack = onBack) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Find places for your next visit", style = MaterialTheme.typography.headlineSmall)
            Text("Search by category and location. LeadFlow will open matching Google Maps listings so you can check current ratings, reviews and details.")
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Category") },
                placeholder = { Text("Restaurants, hotels, cafés…") },
                singleLine = true
            )
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Location") },
                placeholder = { Text("Pondicherry, India") },
                singleLine = true
            )
            Button(
                onClick = {
                    val query = listOf(category.trim(), location.trim()).filter { it.isNotBlank() }.joinToString(" ")
                    val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=$encoded"))
                    context.startActivity(intent)
                },
                enabled = category.isNotBlank() && location.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Search on Google Maps") }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Coming next", style = MaterialTheme.typography.titleMedium)
                    Text("• Results ranked by rating and review count")
                    Text("• Minimum rating and review-count filters")
                    Text("• Nearby search and distance filters")
                    Text("• Recent customer reviews and dish mentions")
                    Text("• Save selected places and export results")
                }
            }
            Text(
                "Current stage: this screen opens Google Maps for the selected search. Ratings and reviews are not yet imported, ranked or filtered inside LeadFlow.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
