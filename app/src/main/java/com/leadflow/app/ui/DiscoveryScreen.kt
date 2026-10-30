package com.leadflow.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.leadflow.app.data.OpenPlaceSearch
import com.leadflow.app.data.PlaceResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DiscoveryScreen(onBack: () -> Unit, onSave: (PlaceResult) -> Unit) {
    var category by remember { mutableStateOf("Restaurant") }
    var location by remember { mutableStateOf("") }
    var radius by remember { mutableStateOf("5000") }
    var results by remember { mutableStateOf<List<PlaceResult>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var searched by remember { mutableStateOf(false) }
    var resultFilter by remember { mutableStateOf("") }
    var sortByCategory by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LfScaffold(title = "Discover Places", onBack = onBack) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Find businesses by category and location", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Business category") },
                placeholder = { Text("Restaurant, hotel, bakery…") },
                singleLine = true
            )
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("City or location") },
                placeholder = { Text("Pondicherry, India") },
                singleLine = true
            )
            OutlinedTextField(
                value = radius,
                onValueChange = { radius = it.filter(Char::isDigit).take(5) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search radius in metres (500–25000)") },
                singleLine = true
            )
            Button(
                onClick = {
                    loading = true
                    error = null
                    searched = true
                    results = emptyList()
                    val selectedCategory = category.trim()
                    val selectedLocation = location.trim()
                    val selectedRadius = radius.toIntOrNull()?.coerceIn(500, 25000) ?: 5000
                    scope.launch {
                        try {
                            results = withContext(Dispatchers.IO) {
                                OpenPlaceSearch.search(selectedCategory, selectedLocation, selectedRadius)
                            }
                            if (results.isEmpty()) error = "No named places found. Try a broader category or nearby city."
                        } catch (e: Exception) {
                            error = e.message ?: "Search failed. Check your internet connection and try again."
                        } finally {
                            loading = false
                        }
                    }
                },
                enabled = !loading && category.isNotBlank() && location.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (loading) "Searching…" else "Find Places") }

            Text(
                "Place names and locations come from OpenStreetMap. Google ratings and review text are not available from this data source.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                "© OpenStreetMap contributors · Data available under the Open Database License (ODbL).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://www.openstreetmap.org/copyright")
                        )
                    )
                }
            ) { Text("View data attribution") }

            if (loading) {
                CircularProgressIndicator()
                Text("Looking up the location and nearby listings…")
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (searched && !loading && results.isNotEmpty()) {
                val visibleResults = results
                    .filter {
                        resultFilter.isBlank() ||
                            it.name.contains(resultFilter, ignoreCase = true) ||
                            it.address.contains(resultFilter, ignoreCase = true) ||
                            it.category.contains(resultFilter, ignoreCase = true)
                    }
                    .let { list ->
                        if (sortByCategory) list.sortedWith(compareBy<PlaceResult> { it.category }.thenBy { it.name })
                        else list.sortedBy { it.name }
                    }
                Text("${visibleResults.size} of ${results.size} places", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = resultFilter,
                    onValueChange = { resultFilter = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Filter results by name, address or type") },
                    singleLine = true
                )
                TextButton(onClick = { sortByCategory = !sortByCategory }) {
                    Text(if (sortByCategory) "Sort: Name" else "Sort: Category")
                }
                if (visibleResults.isEmpty()) {
                    Text("No matching places. Try a different filter.")
                }
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(visibleResults, key = { "${it.name}|${it.latitude}|${it.longitude}" }) { place ->
                        PlaceCard(place = place, onOpen = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(place.mapUrl)))
                        }, onSave = { onSave(place) })
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceCard(place: PlaceResult, onOpen: () -> Unit, onSave: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(place.name, style = MaterialTheme.typography.titleMedium)
            Text(place.category.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelMedium)
            Text(place.address, style = MaterialTheme.typography.bodyMedium)
            if (place.phone.isNotBlank()) Text("Phone: ${place.phone}")
            if (place.website.isNotBlank()) Text("Website: ${place.website}")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onOpen) { Text("Open in Maps") }
                Button(onClick = onSave) { Text("Save lead") }
            }
        }
    }
}
