package com.islami.Aha.util

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DateUtilsTest {

    @Test
    fun `getHijriDateFormatted returns non-empty string`() {
        val result = DateUtils.getHijriDateFormatted()
        assertNotNull(result)
        assertTrue(result.isNotBlank())
    }

    @Test
    fun `getHijriDateFormatted contains H suffix`() {
        val result = DateUtils.getHijriDateFormatted()
        assertTrue("Expected Hijri date to end with 'H', got: $result", result.endsWith("H"))
    }

    @Test
    fun `getHijriDateFormatted contains valid month name`() {
        val validMonths = listOf(
            "Muharram", "Safar", "Rabiul Awal", "Rabiul Akhir",
            "Jumadil Awal", "Jumadil Akhir", "Rajab", "Sya'ban",
            "Ramadhan", "Syawal", "Dzulqa'dah", "Dzulhijjah"
        )
        val result = DateUtils.getHijriDateFormatted()
        assertTrue(
            "Expected Hijri date to contain a valid month name, got: $result",
            validMonths.any { it in result }
        )
    }

    @Test
    fun `getHijriDateFormatted has day month year format`() {
        val result = DateUtils.getHijriDateFormatted()
        // Format: "day monthName year H"
        val parts = result.split(" ")
        assertTrue("Expected at least 3 parts in '$result'", parts.size >= 3)
        // First part should be a number (day)
        assertTrue("Expected first part to be a number, got: ${parts[0]}", parts[0].toIntOrNull() != null)
    }
}
