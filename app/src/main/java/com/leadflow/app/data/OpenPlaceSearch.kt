package com.leadflow.app.data

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.URLEncoder
import java.io.IOException
import java.util.LinkedHashMap

data class PlaceResult(
    val name: String,
    val category: String,
    val address: String,
    val phone: String,
    val website: String,
    val latitude: Double,
    val longitude: Double,
    val distanceMeters: Int = -1
) {
    val mapUrl: String
        get() = "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"
}

object OpenPlaceSearch {
    private const val USER_AGENT = "LeadFlowAndroid/1.0"
    private const val GEOCODE_CACHE_TTL_MS = 24 * 60 * 60 * 1000L
    private const val GEOCODE_CACHE_MAX_ENTRIES = 100
    private const val NOMINATIM_MIN_INTERVAL_MS = 1100L
    private val geocodeLock = Any()
    // Monotonic time avoids wall-clock changes affecting request pacing.
    private var lastGeocodeRequestAt = 0L
    private val geocodeCache = LinkedHashMap<String, Pair<Long, Pair<Double, Double>>>(16, 0.75f, true)

    /** Hard cap so a wide radius cannot flood the public Overpass servers or the phone. */
    const val MAX_RESULTS = 300

    fun search(category: String, location: String, radiusMeters: Int = 5000): List<PlaceResult> {
        val (lat, lon) = geocode(location)
        val query = buildQuery(category, lat, lon, radiusMeters)
        return parseResults(requestOverpass(query), category, location, lat, lon)
    }

    internal fun buildQuery(category: String, lat: Double, lon: Double, radiusMeters: Int): String {
        val clauses = categoryFilters(category).joinToString("\n") { filter ->
            "node(around:$radiusMeters,$lat,$lon)$filter;\n" +
                "way(around:$radiusMeters,$lat,$lon)$filter;\n" +
                "relation(around:$radiusMeters,$lat,$lon)$filter;"
        }
        return "[out:json][timeout:25];\n(\n$clauses\n);\nout center tags $MAX_RESULTS;"
    }

    internal fun parseResults(
        response: String,
        category: String,
        location: String,
        originLat: Double,
        originLon: Double
    ): List<PlaceResult> {
        val elements = JSONObject(response).optJSONArray("elements") ?: return emptyList()
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
                tags.optString("tourism").ifBlank {
                    tags.optString("shop").ifBlank { tags.optString("office").ifBlank { category } }
                }
            }
            results.add(
                PlaceResult(
                    name = name,
                    category = type.replace('_', ' '),
                    address = address,
                    phone = tags.optString("phone").ifBlank { tags.optString("contact:phone") },
                    website = tags.optString("website").ifBlank { tags.optString("contact:website") },
                    latitude = latitude,
                    longitude = longitude,
                    distanceMeters = distanceMeters(originLat, originLon, latitude, longitude)
                )
            )
        }
        return results.sortedBy { it.name.lowercase() }
    }

    /** Haversine distance in whole metres. */
    internal fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
        val r = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
        return (2 * r * Math.asin(Math.sqrt(a))).toInt()
    }


    private fun requestOverpass(query: String): String {
        // Public instances are best-effort. Try POST, then GET on each global mirror.
        val endpoints = listOf(
            "https://overpass-api.de/api/interpreter",
            "https://overpass.private.coffee/api/interpreter",
            "https://maps.mail.ru/osm/tools/overpass/api/interpreter"
        )
        val failures = mutableListOf<String>()
        for (endpoint in endpoints) {
            val host = endpoint.substringAfter("https://").substringBefore("/")
            for (method in listOf("POST", "GET")) {
                try {
                    val response = if (method == "POST") {
                        postOverpass(endpoint, query)
                    } else {
                        getOverpass(endpoint, query)
                    }
                    val payload = JSONObject(response)
                    if (!payload.has("elements")) {
                        throw IOException("Invalid Overpass JSON response")
                    }
                    return response
                } catch (e: Exception) {
                    val detail = e.message?.replace(Regex("\\s+"), " ")?.take(100)
                        ?: e.javaClass.simpleName
                    failures += "$host $method: $detail"
                }
            }
        }
        throw IOException(
            "OpenStreetMap search failed. Tried POST and GET on multiple Overpass servers. " +
                failures.joinToString(" | ")
        )
    }

    private fun getOverpass(endpoint: String, query: String): String {
        val encoded = URLEncoder.encode(query, Charsets.UTF_8.name())
        val connection = (URL("$endpoint?data=$encoded").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12000
            readTimeout = 35000
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

    private fun postOverpass(endpoint: String, query: String): String {
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 12000
            readTimeout = 35000
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

    internal fun escapeOverpassRegex(text: String): String =
        text.filter { it.isLetterOrDigit() || it == ' ' || it == '-' }.replace(Regex("\\s+"), " ").trim()

    internal fun categoryFilters(category: String): List<String> {
        val value = category.trim().lowercase()
        return when {
            "hotel" in value || "lodging" in value -> listOf("""["tourism"="hotel"]""", """["tourism"="guest_house"]""")
            "restaurant" in value || "food" in value -> listOf("""["amenity"="restaurant"]""")
            "cafe" in value || "café" in value || "coffee" in value -> listOf("""["amenity"="cafe"]""")
            "bakery" in value -> listOf("""["shop"="bakery"]""")
            "bar" in value -> listOf("""["amenity"="bar"]""")
            "pharmacy" in value -> listOf("""["amenity"="pharmacy"]""")
            "supermarket" in value || "grocery" in value -> listOf("""["shop"="supermarket"]""")
            "salon" in value -> listOf("""["shop"="hairdresser"]""")
            "hospital" in value -> listOf("""["amenity"="hospital"]""")
            else -> {
                val text = escapeOverpassRegex(value)
                if (text.isBlank()) emptyList()
                else listOf("name", "shop", "amenity", "office", "craft").map { key -> """["$key"~"$text",i]""" }
            }
        }
    }

    /**
     * Resolve a location while respecting Nominatim's public request rate limit.
     * Successful coordinates are cached for 24 hours in a bounded LRU cache.
     */
    private fun geocode(location: String): Pair<Double, Double> {
        val normalized = location.trim().replace(Regex("\\s+"), " ").lowercase()
        synchronized(geocodeLock) {
            val now = System.currentTimeMillis()
            val cached = geocodeCache[normalized]
            if (cached != null && now - cached.first < GEOCODE_CACHE_TTL_MS) {
                return cached.second
            }
            if (cached != null) geocodeCache.remove(normalized)

            val monotonicNow = System.nanoTime() / 1_000_000L
            val waitMs = (NOMINATIM_MIN_INTERVAL_MS - (monotonicNow - lastGeocodeRequestAt))
                .coerceIn(0L, NOMINATIM_MIN_INTERVAL_MS)
            if (waitMs > 0) Thread.sleep(waitMs)

            // Record the request start even when the provider fails, so retries stay paced.
            lastGeocodeRequestAt = System.nanoTime() / 1_000_000L
            val encoded = URLEncoder.encode(location.trim(), Charsets.UTF_8.name())
            val results = JSONArray(requestText("https://nominatim.openstreetmap.org/search?format=jsonv2&limit=1&q=$encoded"))
            val first = results.optJSONObject(0)
                ?: throw IllegalStateException("No matching location found. Try a city or town name.")
            if (!first.has("lat") || !first.has("lon")) {
                throw IllegalStateException("Location lookup returned incomplete coordinates. Please try another location.")
            }

            val coordinates = first.getDouble("lat") to first.getDouble("lon")
            geocodeCache[normalized] = System.currentTimeMillis() to coordinates
            while (geocodeCache.size > GEOCODE_CACHE_MAX_ENTRIES) {
                val eldestKey = geocodeCache.keys.iterator().next()
                geocodeCache.remove(eldestKey)
            }
            return coordinates
        }
    }

    private fun requestText(address: String): String {
        val connection = (URL(address).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12000
            readTimeout = 30000
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "application/json")
        }
        try {
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            return validatePlaceSearchResponse(status, body)
        } catch (exception: SocketTimeoutException) {
            throw placeSearchNetworkFailure(exception)
        } catch (exception: IOException) {
            throw placeSearchNetworkFailure(exception)
        } finally {
            connection.disconnect()
        }
    }
}


/** Validates an HTTP response without depending on Android framework classes. */
internal fun validatePlaceSearchResponse(statusCode: Int, body: String): String {
    if (statusCode !in 200..299) {
        throw IllegalStateException("Place search service returned HTTP $statusCode. Please try again later.")
    }
    if (body.isBlank()) {
        throw IllegalStateException("Place search service returned an empty response. Please try again.")
    }
    return body
}

/** Converts transport failures into actionable messages suitable for the UI. */
internal fun placeSearchNetworkFailure(exception: IOException): IllegalStateException {
    val message = if (exception is SocketTimeoutException) {
        "Place search timed out. Check your connection and try again."
    } else {
        "Unable to reach the place search service. Check your internet connection and try again."
    }
    return IllegalStateException(message, exception)
}
