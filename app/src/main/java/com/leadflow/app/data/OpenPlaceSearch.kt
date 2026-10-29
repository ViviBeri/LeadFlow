package com.leadflow.app.data

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL

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
    private const val USER_AGENT = "LeadFlowAndroid/1.0 (public place discovery)"

    fun search(category: String, location: String, radiusMeters: Int = 5000): List<PlaceResult> {
        val coordinates = geocode(location)
        val lat = coordinates.first
        val lon = coordinates.second
        val filter = categoryFilter(category)
        val query = """
            [out:json][timeout:25];
            (
              node(around:$radiusMeters,$lat,$lon)$filter;
              way(around:$radiusMeters,$lat,$lon)$filter;
              relation(around:$radiusMeters,$lat,$lon)$filter;
            );
            out center tags;
        """.trimIndent()
        val encoded = URLEncoder.encode(query, Charsets.UTF_8.name())
        val json = request("https://overpass-api.de/api/interpreter?data=$encoded")
        val elements = json.optJSONArray("elements") ?: return emptyList()
        val results = ArrayList<PlaceResult>()
        val seen = HashSet<String>()
        for (i in 0 until elements.length()) {
            val item = elements.optJSONObject(i) ?: continue
            val tags = item.optJSONObject("tags") ?: JSONObject()
            val name = tags.optString("name").trim()
            if (name.isBlank()) continue
            val address = listOf(
                tags.optString("addr:housenumber"),
                tags.optString("addr:street"),
                tags.optString("addr:suburb"),
                tags.optString("addr:city")
            ).filter { it.isNotBlank() }.distinct().joinToString(", ").ifBlank { location }
            val latValue = if (item.has("lat")) item.optDouble("lat") else item.optJSONObject("center")?.optDouble("lat") ?: Double.NaN
            val lonValue = if (item.has("lon")) item.optDouble("lon") else item.optJSONObject("center")?.optDouble("lon") ?: Double.NaN
            if (!latValue.isFinite() || !lonValue.isFinite()) continue
            val key = "$name|$address".lowercase()
            if (!seen.add(key)) continue
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
                    latitude = latValue,
                    longitude = lonValue
                )
            )
        }
        return results.sortedBy { it.name.lowercase() }
    }

    private fun geocode(location: String): Pair<Double, Double> {
        val encoded = URLEncoder.encode(location, Charsets.UTF_8.name())
        val json = request("https://nominatim.openstreetmap.org/search?format=jsonv2&limit=1&q=$encoded")
        val results = json.optJSONArray("results") ?: throw IllegalStateException("No matching location found. Try a city or town name.")
        if (results.length() == 0) throw IllegalStateException("No matching location found. Try a city or town name.")
        val first = results.getJSONObject(0)
        return first.getDouble("lat") to first.getDouble("lon")
    }

    private fun categoryFilter(category: String): String {
        val value = category.trim().lowercase()
        return when {
            "restaurant" in value || "hotel" in value || "food" in value -> """["amenity"="restaurant"]"""
            "cafe" in value || "café" in value || "coffee" in value -> """["amenity"="cafe"]"""
            "hotel" in value || "lodging" in value -> """["tourism"="hotel"]"""
            "bakery" in value -> """["shop"="bakery"]"""
            "bar" in value -> """["amenity"="bar"]"""
            "pharmacy" in value -> """["amenity"="pharmacy"]"""
            "supermarket" in value || "grocery" in value -> """["shop"="supermarket"]"""
            "salon" in value -> """["shop"="hairdresser"]"""
            "hospital" in value -> """["amenity"="hospital"]"""
            else -> """["name"~".",i]"""
        }
    }

    private fun request(address: String): JSONObject {
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
            return JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }
}
