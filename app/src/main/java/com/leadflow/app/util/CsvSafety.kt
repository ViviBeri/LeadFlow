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
    val startsWithFormula = firstSignificant?.let { it in setOf('=', '+', '-', '@') } == true
    val startsWithControl = value.firstOrNull()?.let { it in setOf('\t', '\r', '\n') } == true
    return if (startsWithFormula || startsWithControl) "'$value" else value
}
