package com.leadflow.app

import com.leadflow.app.data.OpenPlaceSearch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenPlaceSearchTest {
    private val sample = """
        {"elements":[
          {"type":"node","lat":11.93,"lon":79.83,"tags":{"name":"Cafe One","amenity":"cafe","phone":"+91 98765 43210"}},
          {"type":"way","center":{"lat":11.94,"lon":79.84},"tags":{"name":"Cafe Two","amenity":"cafe"}},
          {"type":"node","lat":11.93,"lon":79.83,"tags":{"amenity":"cafe"}},
          {"type":"node","lat":11.93,"lon":79.83,"tags":{"name":"Cafe One","amenity":"cafe"}}
        ]}
    """.trimIndent()

    @Test fun parsesNamedPlacesAndDropsUnnamedAndDuplicates() {
        val r = OpenPlaceSearch.parseResults(sample, "Cafe", "Pondicherry", 11.93, 79.83)
        assertEquals(listOf("Cafe One", "Cafe Two"), r.map { it.name })
        assertEquals("+91 98765 43210", r[0].phone)
    }

    @Test fun distanceIsComputedFromSearchCentre() {
        val r = OpenPlaceSearch.parseResults(sample, "Cafe", "Pondicherry", 11.93, 79.83)
        assertEquals(0, r[0].distanceMeters)
        assertTrue(r[1].distanceMeters in 1_000..2_500)
    }

    @Test fun queryIsBounded() {
        val q = OpenPlaceSearch.buildQuery("restaurant", 11.93, 79.83, 5000)
        assertTrue(q.trimEnd().endsWith("out center tags ${OpenPlaceSearch.MAX_RESULTS};"))
    }

    @Test fun unknownCategoryDoesNotMatchEverything() {
        val filters = OpenPlaceSearch.categoryFilters("tuition centre")
        assertTrue(filters.isNotEmpty())
        assertTrue(filters.none { it == "[\"name\"~\".\",i]" })
    }

    @Test fun regexInputIsSanitised() =
        assertEquals("a b", OpenPlaceSearch.escapeOverpassRegex("a\" .* b"))
}
