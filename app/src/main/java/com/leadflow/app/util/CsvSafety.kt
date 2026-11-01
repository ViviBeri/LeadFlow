package com.leadflow.app.util

/**
 * Prevents spreadsheet applications from interpreting untrusted CSV cells as formulas.
 *
 * A leading apostrophe forces spreadsheet programs to treat the value as text. Check
 * after leading whitespace/control characters because some spreadsheet importers
 * ignore them before detecting a formula.
 */
internal fun safeCsvCell(value: String): String {
    val firstSignificant = value.firstOrNull { !it.isWhitespace() && !it.isISOControl() }
    return if (firstSignificant in setOf('=', '+', '-', '@') ||
        value.firstOrNull() in setOf('\t', '\r', '\n')
    ) {
        "'$value"
    } else {
        value
    }
}
