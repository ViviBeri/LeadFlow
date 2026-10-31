package com.leadflow.app.data

import java.io.IOException
import java.net.SocketTimeoutException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class PlaceSearchErrorHandlingTest {

    @Test
    fun successfulHttpResponseReturnsBodyUnchanged() {
        val body = """{"elements":[]}"""
        assertEquals(body, validatePlaceSearchResponse(200, body))
    }

    @Test
    fun nonSuccessHttpResponseIncludesStatusCode() {
        val error = assertThrows(IllegalStateException::class.java) {
            validatePlaceSearchResponse(503, "upstream unavailable")
        }
        assertEquals(
            "Place search service returned HTTP 503. Please try again later.",
            error.message
        )
    }

    @Test
    fun emptyHttpResponseIsRejected() {
        val error = assertThrows(IllegalStateException::class.java) {
            validatePlaceSearchResponse(200, "  \n")
        }
        assertEquals(
            "Place search service returned an empty response. Please try again.",
            error.message
        )
    }

    @Test
    fun timeoutGetsSpecificActionableMessageAndRetainsCause() {
        val cause = SocketTimeoutException("read timed out")
        val error = placeSearchNetworkFailure(cause)
        assertEquals(
            "Place search timed out. Check your connection and try again.",
            error.message
        )
        assertSame(cause, error.cause)
    }

    @Test
    fun otherIoFailuresGetConnectionMessageAndRetainCause() {
        val cause = IOException("connection reset")
        val error = placeSearchNetworkFailure(cause)
        assertEquals(
            "Unable to reach the place search service. Check your internet connection and try again.",
            error.message
        )
        assertSame(cause, error.cause)
    }
}
