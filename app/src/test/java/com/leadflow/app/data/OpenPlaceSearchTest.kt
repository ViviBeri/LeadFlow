package com.leadflow.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenPlaceSearchTest {
    private val search = OpenPlaceSearch

    @Test
    fun hotelSearchIncludesCommonLodgingTags() {
        val filters = search.categoryFilters("hotel")
        assertTrue(filters.contains("""["tourism"="hotel"]"""))
        assertTrue(filters.contains("""["tourism"="guest_house"]"""))
        assertTrue(filters.contains("""["tourism"="hostel"]"""))
        assertTrue(filters.contains("""["tourism"="motel"]"""))
    }

    @Test
    fun multipleCategoriesAreCombinedAndDeduplicated() {
        val filters = search.categoryFilters("hotel, guest house;hotel")
        assertTrue(filters.contains("""["tourism"="hotel"]"""))
        assertTrue(filters.contains("""["tourism"="guest_house"]"""))
        assertEquals(filters.size, filters.distinct().size)
    }

    @Test
    fun bakerySearchIncludesBakeryTag() {
        assertEquals(
            listOf("""["shop"="bakery"]"""),
            search.categoryFilters("bakery")
        )
    }

    @Test
    fun grocerySearchIncludesSupermarketAndConvenienceTags() {
        val filters = search.categoryFilters("grocery")
        assertTrue(filters.contains("""["shop"="supermarket"]"""))
        assertTrue(filters.contains("""["shop"="convenience"]"""))
    }

    @Test
    fun clinicSearchIncludesClinicAndHospitalTags() {
        val filters = search.categoryFilters("clinic")
        assertTrue(filters.contains("""["amenity"="clinic"]"""))
        assertTrue(filters.contains("""["amenity"="hospital"]"""))
    }
}
