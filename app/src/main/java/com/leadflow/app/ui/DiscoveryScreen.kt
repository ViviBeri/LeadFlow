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
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import kotlin.math.roundToInt
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
    var radius by remember { mutableStateOf("5") }
    var results by remember { mutableStateOf<List<PlaceResult>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var searched by remember { mutableStateOf(false) }
    var resultFilter by remember { mutableStateOf("") }
    var sortMode by remember { mutableStateOf("Name") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val visibleResults = results
        .filter {
            resultFilter.isBlank() ||
                it.name.contains(resultFilter, ignoreCase = true) ||
                it.address.contains(resultFilter, ignoreCase = true) ||
                it.category.contains(resultFilter, ignoreCase = true)
        }
        .let { list ->
            when (sortMode) {
                "Category" -> list.sortedWith(compareBy<PlaceResult> { it.category }.thenBy { it.name })
                "Distance" -> list.sortedBy { if (it.distanceMeters < 0) Int.MAX_VALUE else it.distanceMeters }
                else -> list.sortedBy { it.name }
            }
        }

    LfScaffold(title = "Discover Places", onBack = onBack) { padding ->
        // Keep the search form and all results in one scroll container. A nested
        // LazyColumn constrained by the remaining form height made the result
        // cards scroll inside a small viewport on phones.
        LazyColumn(
            state = listState,
            modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text("Find businesses by category and location", style = MaterialTheme.typography.titleLarge)
            }
            item {
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Business category") },
                    placeholder = { Text("Restaurant, hotel, bakery…") },
                    singleLine = true
                )
            }
            item {
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("City or location") },
                    placeholder = { Text("Pondicherry, India") },
                    singleLine = true
                )
            }
            item {
                OutlinedTextField(
                    value = radius,
                    onValueChange = { value ->
                        val cleaned = value.filter { it.isDigit() || it == '.' }
                        radius = if (cleaned.count { it == '.' } <= 1) cleaned.take(5) else radius
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Search radius in kilometres (0.5–25)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }
            item {
                Button(
                    onClick = {
                        loading = true
                        error = null
                        searched = true
                        results = emptyList()
                        val selectedCategory = category.trim()
                        val selectedLocation = location.trim()
                        val selectedRadius = ((radius.toDoubleOrNull() ?: 5.0).coerceIn(0.5, 25.0) * 1000.0).roundToInt()
                        scope.launch {
                            try {
                                results = withContext(Dispatchers.IO) {
                                    OpenPlaceSearch.search(selectedCategory, selectedLocation, selectedRadius)
                                }
                                if (results.isEmpty()) error = "No named places found. Try a broader category or nearby city."
                            } catch (e: Exception) {
                                error = if (e is IllegalStateException &&
                                    (e.message?.startsWith("No matching location found") == true ||
                                        e.message?.startsWith("Location lookup returned incomplete coordinates") == true)
                                ) {
                                    e.message
                                } else {
                                    "OpenStreetMap is temporarily unavailable. Try again later or use Google Maps for this search."
                                }
                            } finally {
                                loading = false
                            }
                        }
                    },
                    enabled = !loading && category.isNotBlank() && location.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (loading) "Searching…" else "Find Places") }
            }
            item {
                Text(
                    "Place names and locations come from OpenStreetMap. Google ratings and review text are not available from this data source.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                Text(
                    "© OpenStreetMap contributors · Data available under the Open Database License (ODbL).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
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
            }
            if (loading) {
                item {
                    CircularProgressIndicator()
                    Text("Looking up the location and nearby listings…")
                }
            }
            error?.let { message ->
                item {
                    Text(message, color = MaterialTheme.colorScheme.error)
                    OutlinedButton(
                        onClick = {
                            val query = Uri.encode("$category in $location")
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://www.google.com/maps/search/?api=1&query=$query")
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Search in Google Maps") }
                }
            }
            if (searched && !loading && results.isNotEmpty()) {
                item {
                    Text("${visibleResults.size} of ${results.size} places", style = MaterialTheme.typography.titleMedium)
                }
                item {
                    OutlinedTextField(
                        value = resultFilter,
                        onValueChange = { resultFilter = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Filter results by name, address or type") },
                        singleLine = true
                    )
                }
                item {
                    TextButton(onClick = {
                        sortMode = when (sortMode) { "Name" -> "Category"; "Category" -> "Distance"; else -> "Name" }
                    }) { Text("Sort: $sortMode (tap to change)") }
                }
                if (results.size >= OpenPlaceSearch.MAX_RESULTS) {
                    item {
                        Text(
                            "Showing the first ${OpenPlaceSearch.MAX_RESULTS} places. Reduce the radius or use a more specific category to see others.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (visibleResults.isEmpty()) {
                    item { Text("No matching places. Try a different filter.") }
                }
                items(visibleResults, key = { "${it.name}|${it.latitude}|${it.longitude}" }) { place ->
                    PlaceCard(place = place, onOpen = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(place.mapUrl)))
                    }, onSave = { onSave(place) })
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
            if (place.distanceMeters >= 0) {
                val d = place.distanceMeters
                Text(if (d < 1000) "$d m from search centre" else "%.1f km from search centre".format(d / 1000.0))
            }
            if (place.phone.isNotBlank()) Text("Phone: ${place.phone}")
            if (place.website.isNotBlank()) Text("Website: ${place.website}")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onOpen) { Text("Open in Maps") }
                Button(onClick = onSave) { Text("Save lead") }
            }
        }
    }
}
