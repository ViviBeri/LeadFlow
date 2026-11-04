package com.leadflow.app.data

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.io.IOException
import java.util.concurrent.Callable
import java.util.concurrent.CompletionService
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorCompletionService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.ConcurrentHashMap

data class PlaceResult(
    val name: String,
    val category: String,
    val address: String,
    val phone: String,
    val website: String,
    val latitude: Double,
    val longitude: Double
) {
    val mapUrl: String
        get() = "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"
}

object OpenPlaceSearch {
    private const val USER_AGENT = "LeadFlowAndroid/1.0"
    private val geocodeCache = ConcurrentHashMap<String, Pair<Double, Double>>()

    fun search(category: String, location: String, radiusMeters: Int = 5000): List<PlaceResult> {
        val (lat, lon) = geocode(location)
        val filters = categoryFilters(category)
        val clauses = filters.joinToString("\n") { filter ->
            """
            node(around:$radiusMeters,$lat,$lon)$filter;
            way(around:$radiusMeters,$lat,$lon)$filter;
            relation(around:$radiusMeters,$lat,$lon)$filter;
            """.trimIndent()
        }
        val query = "[out:json][timeout:15];\n(\n$clauses\n);\nout center tags;"
        val overpassResponse = try {
            requestOverpass(query)
        } catch (overpassError: Exception) {
            // Photon is an independent OSM-backed provider. Use it when every
            // Overpass mirror is unavailable, rather than failing the whole search.
            return searchWithPhoton(category, location, lat, lon, radiusMeters)
        }
        val json = JSONObject(overpassResponse)
        val elements = json.optJSONArray("elements") ?: return emptyList()
        val results = ArrayList<PlaceResult>()
        val seen = HashSet<String>()
        for (i in 0 until elements.length()) {
            val item = elements.optJSONObject(i) ?: continue
            val tags = item.optJSONObject("tags") ?: JSONObject()
            val name = tags.optString("name").trim()
            if (name.isBlank()) continue
            val address = listOf(
                tags.optString("addr:housenumber"), tags.optString("addr:street"),
                tags.optString("addr:suburb"), tags.optString("addr:city")
            ).filter { it.isNotBlank() }.distinct().joinToString(", ").ifBlank { location }
            val center = item.optJSONObject("center")
            val latitude = if (item.has("lat")) item.optDouble("lat") else center?.optDouble("lat") ?: Double.NaN
            val longitude = if (item.has("lon")) item.optDouble("lon") else center?.optDouble("lon") ?: Double.NaN
            if (!latitude.isFinite() || !longitude.isFinite()) continue
            if (!seen.add("$name|$address".lowercase())) continue
            val type = tags.optString("amenity").ifBlank {
                tags.optString("tourism").ifBlank { tags.optString("shop").ifBlank { category } }
            }
            results.add(
                PlaceResult(
                    name = name,
                    category = type.replace('_', ' '),
                    address = address,
                    phone = tags.optString("phone").ifBlank { tags.optString("contact:phone") },
                    website = tags.optString("website").ifBlank { tags.optString("contact:website") },
                    latitude = latitude,
                    longitude = longitude
                )
            )
        }
        // A valid but empty Overpass response can still miss records returned by Photon.
        // Use Photon as a fallback for empty categories as well as provider failures.
        if (results.isEmpty()) {
            return searchWithPhoton(category, location, lat, lon, radiusMeters)
        }
        return results.sortedBy { it.name.lowercase() }
    }


    private fun requestOverpass(query: String): String {
        val endpoints = listOf(
            "https://overpass-api.de/api/interpreter",
            "https://lz4.overpass-api.de/api/interpreter",
            "https://overpass.private.coffee/api/interpreter",
            "https://overpass.kumi.systems/api/interpreter",
            "https://maps.mail.ru/osm/tools/overpass/api/interpreter"
        )
        val failures = java.util.concurrent.atomic.AtomicInteger(0)
        val executor = Executors.newFixedThreadPool(endpoints.size)
        val completion: CompletionService<String?> = ExecutorCompletionService(executor)
        val futures = mutableListOf<Future<String?>>()

        try {
            endpoints.forEach { endpoint ->
                futures += completion.submit(Callable {
                    for (method in listOf("POST", "GET")) {
                        try {
                            val response = if (method == "POST") {
                                postOverpass(endpoint, query, 6000)
                            } else {
                                getOverpass(endpoint, query)
                            }
                            val payload = JSONObject(response)
                            if (!payload.has("elements")) {
                                throw IOException("Invalid Overpass JSON response")
                            }
                            return@Callable response
                        } catch (_: Exception) {
                            failures.incrementAndGet()
                        }
                    }
                    null
                })
            }

            // Race independent mirrors. A healthy server wins without waiting for
            // slower or unreachable mirrors; this lookup has an 8-second overall deadline.
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(8)
            repeat(endpoints.size) {
                val remaining = deadline - System.nanoTime()
                if (remaining <= 0L) return@repeat
                val completed = completion.poll(remaining, TimeUnit.NANOSECONDS) ?: return@repeat
                val response = try {
                    completed.get()
                } catch (_: ExecutionException) {
                    null
                }
                if (response != null) return response
            }
        } finally {
            futures.forEach { it.cancel(true) }
            executor.shutdownNow()
        }

        throw IOException(
            "OpenStreetMap search servers did not respond within 8 seconds or returned errors. " +
                "Check your connection and try again. (" + failures.get() + " failed requests)"
        )
    }

    private fun getOverpass(endpoint: String, query: String): String {
        val encoded = URLEncoder.encode(query, Charsets.UTF_8.name())
        val connection = (URL("$endpoint?data=$encoded").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 3500
            readTimeout = 6000
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "application/json")
        }
        try {
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) throw IOException("HTTP $status")
            return response
        } finally {
            connection.disconnect()
        }
    }

    private fun postOverpass(endpoint: String, query: String, timeoutMs: Int): String {
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = timeoutMs.coerceAtMost(3500)
            readTimeout = timeoutMs.coerceAtMost(6000)
            doOutput = true
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
        }
        try {
            val body = "data=" + URLEncoder.encode(query, Charsets.UTF_8.name())
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) throw IOException("HTTP $status")
            return response
        } finally {
            connection.disconnect()
        }
    }

    private fun searchWithPhoton(
        category: String,
        location: String,
        latitude: Double,
        longitude: Double,
        radiusMeters: Int
    ): List<PlaceResult> {
        val osmTag = when {
            category.contains("restaurant", true) || category.contains("food", true) -> "amenity:restaurant"
            category.contains("hotel", true) || category.contains("lodging", true) || category.contains("hostel", true) || category.contains("motel", true) || category.contains("guest house", true) || category.contains("guesthouse", true) -> "tourism:hotel"
            category.contains("cafe", true) || category.contains("café", true) || category.contains("coffee", true) -> "amenity:cafe"
            category.contains("bakery", true) -> "shop:bakery"
            category.contains("bar", true) -> "amenity:bar"
            category.contains("pharmacy", true) -> "amenity:pharmacy"
            category.contains("supermarket", true) || category.contains("grocery", true) -> "shop:supermarket"
            category.contains("salon", true) -> "shop:hairdresser"
            category.contains("hospital", true) -> "amenity:hospital"
            else -> null
        }
        // Forward search discovers businesses; reverse geocoding mostly returns nearby address features.
        val queryText = category.trim().ifBlank { "business" }
        val params = buildString {
            append("q=").append(URLEncoder.encode(queryText, Charsets.UTF_8.name()))
            append("&lat=").append(latitude)
            append("&lon=").append(longitude)
            append("&limit=100")
            append("&lang=en")
            if (osmTag != null) append("&osm_tag=").append(URLEncoder.encode(osmTag, Charsets.UTF_8.name()))
        }
        val response = requestText("https://photon.komoot.io/api/?$params")
        val features = JSONObject(response).optJSONArray("features") ?: return emptyList()
        val results = ArrayList<PlaceResult>()
        val seen = HashSet<String>()
        for (i in 0 until features.length()) {
            val feature = features.optJSONObject(i) ?: continue
            val properties = feature.optJSONObject("properties") ?: continue
            val name = properties.optString("name").trim()
            if (name.isBlank()) continue
            val geometry = feature.optJSONObject("geometry") ?: continue
            val coordinates = geometry.optJSONArray("coordinates") ?: continue
            if (coordinates.length() < 2) continue
            val placeLon = coordinates.optDouble(0, Double.NaN)
            val placeLat = coordinates.optDouble(1, Double.NaN)
            if (!placeLat.isFinite() || !placeLon.isFinite()) continue
            if (distanceMeters(latitude, longitude, placeLat, placeLon) > radiusMeters) continue
            val address = listOf(
                properties.optString("housenumber"),
                properties.optString("street"),
                properties.optString("district"),
                properties.optString("city"),
                properties.optString("state")
            ).filter { it.isNotBlank() }.distinct().joinToString(", ").ifBlank { location }
            if (!seen.add("$name|$address".lowercase())) continue
            val extra = properties.optJSONObject("extra") ?: JSONObject()
            val type = properties.optString("osm_value").ifBlank { category }
            results.add(
                PlaceResult(
                    name = name,
                    category = type.replace('_', ' '),
                    address = address,
                    phone = extra.optString("phone").ifBlank { extra.optString("contact:phone") },
                    website = extra.optString("website").ifBlank { extra.optString("contact:website") },
                    latitude = placeLat,
                    longitude = placeLon
                )
            )
        }
        return results.sortedBy { it.name.lowercase() }
    }

    private fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
            kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
            kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
        return 2 * earthRadius * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
    }

    private fun categoryFilters(category: String): List<String> {
        // Accept comma-, semicolon-, or pipe-separated categories, e.g.
        // "hotel, resort, guest house". Keep the Overpass request to one query.
        val terms = category.split(',', ';', '|')
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .distinct()
        val requestedTerms = terms.ifEmpty { listOf(category.trim().lowercase()) }

        return requestedTerms.flatMap { value ->
            when {
                "hotel" in value || "lodging" in value || "resort" in value ||
                    "hostel" in value || "motel" in value || "guest house" in value ||
                    "guesthouse" in value -> listOf(
                        """["tourism"="hotel"]""",
                        """["tourism"="guest_house"]""",
                        """["tourism"="hostel"]""",
                        """["tourism"="motel"]""",
                        """["tourism"="apartment"]""",
                        """["tourism"="chalet"]"""
                    )
                "restaurant" in value || "food" in value -> listOf("""["amenity"="restaurant"]""")
                "cafe" in value || "café" in value || "coffee" in value -> listOf("""["amenity"="cafe"]""")
                "bakery" in value || "bake shop" in value -> listOf("""["shop"="bakery"]""")
                "bar" in value || "pub" in value -> listOf("""["amenity"="bar"]""")
                "pharmacy" in value || "chemist" in value -> listOf("""["amenity"="pharmacy"]""")
                "supermarket" in value || "grocery" in value -> listOf(
                    """["shop"="supermarket"]""",
                    """["shop"="convenience"]"""
                )
                "salon" in value || "hairdresser" in value -> listOf("""["shop"="hairdresser"]""")
                "hospital" in value || "clinic" in value -> listOf(
                    """["amenity"="hospital"]""",
                    """["amenity"="clinic"]"""
                )
                else -> listOf("""["name"~".",i]""")
            }
        }.distinct()
    }

    private fun geocode(location: String): Pair<Double, Double> {
        val cacheKey = location.trim().lowercase()
        geocodeCache[cacheKey]?.let { return it }
        val encoded = URLEncoder.encode(location, Charsets.UTF_8.name())
        val results = JSONArray(requestText("https://nominatim.openstreetmap.org/search?format=jsonv2&limit=1&q=$encoded"))
        if (results.length() == 0) throw IllegalStateException("No matching location found. Try a city or town name.")
        val first = results.getJSONObject(0)
        val coordinates = first.getDouble("lat") to first.getDouble("lon")
        geocodeCache[cacheKey] = coordinates
        return coordinates
    }

    private fun requestText(address: String): String {
        val connection = (URL(address).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 4000
            readTimeout = 7000
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "application/json")
        }
        try {
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            if (status !in 200..299) throw IllegalStateException("Place search service returned HTTP $status. Please try again later.")
            return body
        } finally {
            connection.disconnect()
        }
    }
}
