package com.leadflow.app.util

import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ResourceCleanupTest {
    @Test
    fun successfulOperationKeepsResource() {
        var cleanupCount = 0

        val result = runWithCleanupOnFailure(
            resource = "media-store-uri",
            action = { true },
            cleanup = { cleanupCount++ }
        )

        assertTrue(result)
        assertEquals(0, cleanupCount)
    }

    @Test
    fun unsuccessfulOperationCleansUpResource() {
        var cleanedResource: String? = null

        val result = runWithCleanupOnFailure(
            resource = "media-store-uri",
            action = { false },
            cleanup = { cleanedResource = it }
        )

        assertFalse(result)
        assertEquals("media-store-uri", cleanedResource)
    }

    @Test
    fun thrownOperationCleansUpAndPreservesOriginalFailure() {
        val failure = IOException("disk full")
        var cleanedResource: String? = null

        val thrown = try {
            runWithCleanupOnFailure(
                resource = "media-store-uri",
                action = { throw failure },
                cleanup = { cleanedResource = it }
            )
            null
        } catch (exception: IOException) {
            exception
        }

        assertSame(failure, thrown)
        assertEquals("media-store-uri", cleanedResource)
    }

    @Test
    fun cleanupFailureDoesNotHideOriginalFailure() {
        val failure = IOException("write failed")

        val thrown = try {
            runWithCleanupOnFailure(
                resource = "media-store-uri",
                action = { throw failure },
                cleanup = { throw IllegalStateException("delete failed") }
            )
            null
        } catch (exception: IOException) {
            exception
        }

        assertSame(failure, thrown)
    }
}
