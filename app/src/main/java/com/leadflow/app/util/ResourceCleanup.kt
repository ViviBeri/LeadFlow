package com.leadflow.app.util

/**
 * Runs an operation for a newly-created resource and removes that resource when the
 * operation fails or reports that it could not complete.
 *
 * Cleanup failures are deliberately suppressed so they do not hide the original
 * operation failure.
 */
internal fun <T> runWithCleanupOnFailure(
    resource: T,
    action: () -> Boolean,
    cleanup: (T) -> Unit
): Boolean {
    try {
        if (action()) return true
    } catch (failure: Exception) {
        runCatching { cleanup(resource) }
        throw failure
    }

    runCatching { cleanup(resource) }
    return false
}
