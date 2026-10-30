package com.leadflow.app.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.URLEncoder

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
        val encoded = URLEncoder.encode(query, Charsets.UTF_8.name())
        val response = requestText("https://overpass-api.de/api/interpreter?data=$encoded")
        val json = try {
            JSONObject(response)
        } catch (exception: org.json.JSONException) {
            throw IllegalStateException("The place search service returned an unreadable response. Please try again later.", exception)
        }
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
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                throw IllegalStateException("Place search service returned HTTP $status. Please try again later.")
            }
            if (body.isBlank()) {
                throw IllegalStateException("Place search service returned an empty response. Please try again.")
            }
            return body
        } catch (exception: SocketTimeoutException) {
            throw IllegalStateException("Place search timed out. Check your connection and try again.", exception)
        } catch (exception: IOException) {
            throw IllegalStateException("Unable to reach the place search service. Check your internet connection and try again.", exception)
        } finally {
            connection.disconnect()
        }
    }
}
