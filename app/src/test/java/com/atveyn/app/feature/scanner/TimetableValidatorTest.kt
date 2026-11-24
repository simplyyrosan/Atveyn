package com.atveyn.app.feature.scanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimetableValidatorTest {

    @Test
    fun testValidEntriesPassValidation() {
        val entries = listOf(
            ParsedTimetableEntry(
                dayOfWeek = 1,
                subjectName = "CT LAB",
                subjectCode = "CT LAB",
                startTime = "13:50",
                endTime = "15:50",
                room = "E-009"
            ),
            ParsedTimetableEntry(
                dayOfWeek = 1,
                subjectName = "ECAP",
                subjectCode = "ECAP",
                startTime = "15:50",
                endTime = "16:50",
                room = "E-301"
            )
        )

        val report = TimetableValidator.validateEntries(entries)
        assertTrue(report.isValid)
        assertTrue(report.errors.isEmpty())
    }

    @Test
    fun testStartTimeNotEarlierThanEndTimeFails() {
        val entries = listOf(
            ParsedTimetableEntry(
                dayOfWeek = 1,
                subjectName = "SCWP",
                startTime = "16:00",
                endTime = "14:00"
            )
        )

        val report = TimetableValidator.validateEntries(entries)
        assertFalse(report.isValid)
        assertEquals(1, report.errors.size)
        assertTrue(report.errors[0].message.contains("strictly earlier"))
    }

    @Test
    fun testBlankSubjectFails() {
        val entries = listOf(
            ParsedTimetableEntry(
                dayOfWeek = 2,
                subjectName = "   ",
                startTime = "09:00",
                endTime = "10:00"
            )
        )

        val report = TimetableValidator.validateEntries(entries)
        assertFalse(report.isValid)
        assertTrue(report.errors.any { it.message.contains("blank") })
    }

    @Test
    fun testDuplicateEntryDetection() {
        val entries = listOf(
            ParsedTimetableEntry(
                dayOfWeek = 3,
                subjectName = "Math",
                startTime = "09:00",
                endTime = "10:00"
            ),
            ParsedTimetableEntry(
                dayOfWeek = 3,
                subjectName = "Math",
                startTime = "09:00",
                endTime = "10:00"
            )
        )

        val report = TimetableValidator.validateEntries(entries)
        assertFalse(report.isValid)
        assertTrue(report.errors.any { it.message.contains("Duplicate") })
    }

    @Test
    fun testConsecutiveNoonSlotsDoNotConflict() {
        val entries = listOf(
            ParsedTimetableEntry(
                dayOfWeek = 6, // Saturday
                subjectName = "PSD",
                startTime = "11:00",
                endTime = "12:00"
            ),
            ParsedTimetableEntry(
                dayOfWeek = 6, // Saturday
                subjectName = "CT",
                startTime = "12:00",
                endTime = "13:00"
            )
        )

        val report = TimetableValidator.validateEntries(entries)
        assertTrue("Consecutive noon slots 11-12 and 12-1 must be valid and non-overlapping", report.isValid)
        assertTrue(report.errors.isEmpty())
    }

    @Test
    fun testOverlappingIntervalWarning() {
        val entries = listOf(
            ParsedTimetableEntry(
                dayOfWeek = 4,
                subjectName = "Lab A",
                startTime = "10:00",
                endTime = "12:00"
            ),
            ParsedTimetableEntry(
                dayOfWeek = 4,
                subjectName = "Lecture B",
                startTime = "11:00",
                endTime = "12:00"
            )
        )

        val report = TimetableValidator.validateEntries(entries)
        // Overlap is reported as a warning so user can inspect and resolve
        assertTrue(report.warnings.any { it.message.contains("Overlapping") })
    }
}
