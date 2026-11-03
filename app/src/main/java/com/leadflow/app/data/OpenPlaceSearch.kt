package com.leadflow.app.data

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.io.IOException

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
        val query = "[out:json][timeout:25];\n(\n$clauses\n);\nout center tags;"
        val json = JSONObject(requestOverpass(query))
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
        return results.sortedBy { it.name.lowercase() }
    }


    private fun requestOverpass(query: String): String {
        // Public Overpass instances are independent best-effort services.
        // Try distinct global mirrors once each before reporting a useful diagnostic.
        val endpoints = listOf(
            "https://maps.mail.ru/osm/tools/overpass/api/interpreter",
            "https://lz4.overpass-api.de/api/interpreter",
            "https://z.overpass-api.de/api/interpreter",
            "https://overpass-api.de/api/interpreter",
            "https://overpass.private.coffee/api/interpreter"
        )
        val failures = mutableListOf<String>()
        for (endpoint in endpoints) {
            val host = endpoint.substringAfter("https://").substringBefore("/")
            try {
                val response = postOverpass(endpoint, query)
                // Validate the payload before accepting an endpoint. Some proxies return
                // HTML error pages with HTTP 200, which must not block the next mirror.
                JSONObject(response)
                return response
            } catch (e: Exception) {
                val detail = e.message?.replace(Regex("\\s+"), " ")?.take(110)
                    ?: e.javaClass.simpleName
                failures += "$host: $detail"
            }
        }
        throw IOException(
            "OpenStreetMap search failed on all public Overpass servers. " +
                failures.joinToString(" | ")
        )
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
            if (status !in 200..299) throw IOException("HTTP " + status)
            return response
        } finally { connection.disconnect() }
    }

    private fun categoryFilters(category: String): List<String> {
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
            else -> listOf("""["name"~".",i]""")
        }
    }

    private fun geocode(location: String): Pair<Double, Double> {
        val encoded = URLEncoder.encode(location, Charsets.UTF_8.name())
        val results = JSONArray(requestText("https://nominatim.openstreetmap.org/search?format=jsonv2&limit=1&q=$encoded"))
        if (results.length() == 0) throw IllegalStateException("No matching location found. Try a city or town name.")
        val first = results.getJSONObject(0)
        return first.getDouble("lat") to first.getDouble("lon")
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
            val body = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            if (status !in 200..299) throw IllegalStateException("Place search service returned HTTP $status. Please try again later.")
            return body
        } finally {
            connection.disconnect()
        }
    }
}
