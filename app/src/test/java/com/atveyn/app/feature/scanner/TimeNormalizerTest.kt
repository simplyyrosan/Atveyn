package com.atveyn.app.feature.scanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeNormalizerTest {

    @Test
    fun testNoonVariationsNormalizeTo12PM() {
        // Explicit NOON must normalize strictly to 12:00 (12:00 PM), never 00:00 (midnight)
        val variants = listOf(
            "12:00NOON",
            "12.00 NOON",
            "12:00 NOON",
            "12.00NOON",
            "12 NOON",
            "12.00 PM",
            "12:00PM",
            "12:00 PM",
            "12.00PM"
        )

        for (variant in variants) {
            val parsed = TimeNormalizer.parseSingleTime(variant)
            assertNotNull("Failed to parse: $variant", parsed)
            assertEquals("Expected 12:00 PM (12:00) for '$variant'", "12:00", parsed)
        }
    }

    @Test
    fun testMidnightIsNotConfusedWithNoon() {
        val midnight = TimeNormalizer.parseSingleTime("12:00 AM")
        assertEquals("00:00", midnight)

        val noon = TimeNormalizer.parseSingleTime("12:00 PM")
        assertEquals("12:00", noon)

        val noonText = TimeNormalizer.parseSingleTime("12:00 NOON")
        assertEquals("12:00", noonText)
    }

    @Test
    fun testConsecutiveNoonSlots() {
        // Slot 1: 11.00AM-12.00NOON -> 11:00 to 12:00
        val slot1 = TimeNormalizer.extractTimeRange("11.00AM-12.00NOON")
        assertNotNull(slot1)
        assertEquals("11:00", slot1!!.first)
        assertEquals("12:00", slot1.second)

        // Slot 2: 12.00 NOON - 1.00 PM -> 12:00 to 13:00
        val slot2 = TimeNormalizer.extractTimeRange("12.00 NOON - 1.00 PM")
        assertNotNull(slot2)
        assertEquals("12:00", slot2!!.first)
        assertEquals("13:00", slot2.second)

        // Ensure slot1 end time matches slot2 start time exactly with zero overlap
        assertEquals(slot1.second, slot2.first)
        assertTrue(TimeNormalizer.compareTimes(slot1.first, slot1.second) < 0)
        assertTrue(TimeNormalizer.compareTimes(slot2.first, slot2.second) < 0)
        assertTrue(TimeNormalizer.compareTimes(slot1.second, slot2.second) < 0)
    }

    @Test
    fun testDiverseTimeFormats() {
        // 01.00 PM
        val t1 = TimeNormalizer.parseSingleTime("01.00 PM")
        assertEquals("13:00", t1)

        // 1.50PM-2.50PM
        val r1 = TimeNormalizer.extractTimeRange("1.50PM-2.50PM")
        assertNotNull(r1)
        assertEquals("13:50", r1!!.first)
        assertEquals("14:50", r1.second)

        // 5.50PM-6.50PM
        val r2 = TimeNormalizer.extractTimeRange("5.50PM-6.50PM")
        assertNotNull(r2)
        assertEquals("17:50", r2!!.first)
        assertEquals("18:50", r2.second)

        // 8.00AM-9.00AM
        val r3 = TimeNormalizer.extractTimeRange("8.00AM-9.00AM")
        assertNotNull(r3)
        assertEquals("08:00", r3!!.first)
        assertEquals("09:00", r3.second)

        // Standard 24h: 14:00 - 15:45
        val r4 = TimeNormalizer.extractTimeRange("14:00 - 15:45")
        assertNotNull(r4)
        assertEquals("14:00", r4!!.first)
        assertEquals("15:45", r4.second)
    }

    @Test
    fun testContextualAmPmInference() {
        // When suffix is missing in daytime academic timetable:
        // 8:00 defaults to 08:00 (AM)
        assertEquals("08:00", TimeNormalizer.parseSingleTime("8:00"))
        // 10:30 defaults to 10:30 (AM)
        assertEquals("10:30", TimeNormalizer.parseSingleTime("10:30"))
        // 12:00 defaults to 12:00 (midday noon, never midnight)
        assertEquals("12:00", TimeNormalizer.parseSingleTime("12:00"))
        // 1:50 defaults to 13:50 (PM)
        assertEquals("13:50", TimeNormalizer.parseSingleTime("1.50"))
        // 3:00 defaults to 15:00 (PM)
        assertEquals("15:00", TimeNormalizer.parseSingleTime("3:00"))
    }

    @Test
    fun testOcrArtifactsForNoon() {
        assertTrue(TimeNormalizer.isNoon("12.00 N00N"))
        assertTrue(TimeNormalizer.isNoon("12:00 NO0N"))
        assertTrue(TimeNormalizer.isNoon("12:00 N0ON"))
        assertTrue(TimeNormalizer.isNoon("12:00 NOON"))

        val rangeWithOcrArtifact = TimeNormalizer.extractTimeRange("11.00AM-12.00N00N")
        assertNotNull(rangeWithOcrArtifact)
        assertEquals("11:00", rangeWithOcrArtifact!!.first)
        assertEquals("12:00", rangeWithOcrArtifact.second)
    }
}
