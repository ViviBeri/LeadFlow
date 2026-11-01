package com.leadflow.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

class CsvSafetyTest {
    @Test
    fun prefixesFormulaStartingWithEquals() {
        assertEquals("'=1+1", safeCsvCell("=1+1"))
    }

    @Test
    fun prefixesFormulaStartingWithPlusMinusAndAt() {
        assertEquals("'+SUM(A1:A2)", safeCsvCell("+SUM(A1:A2)"))
        assertEquals("'-1+2", safeCsvCell("-1+2"))
        assertEquals("'@SUM(A1:A2)", safeCsvCell("@SUM(A1:A2)"))
    }

    @Test
    fun detectsFormulaAfterLeadingWhitespaceAndControls() {
        assertEquals("'  =1+1", safeCsvCell("  =1+1"))
        assertEquals("'\t=1+1", safeCsvCell("\t=1+1"))
        assertEquals("'\r=1+1", safeCsvCell("\r=1+1"))
        assertEquals("'\n=1+1", safeCsvCell("\n=1+1"))
    }

    @Test
    fun preservesOrdinaryTextAndEmptyValues() {
        assertEquals("Puducherry HVAC Services", safeCsvCell("Puducherry HVAC Services"))
        assertEquals("", safeCsvCell(""))
        assertEquals("தமிழ் நிறுவனம்", safeCsvCell("தமிழ் நிறுவனம்"))
    }
}
