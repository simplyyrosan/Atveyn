package com.atveyn.app.feature.scanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimetableParserTest {

    private fun box(text: String, left: Float, top: Float, right: Float, bottom: Float) =
        TimetableParser.TextItemWithBox(
            text = text,
            left = left,
            top = top,
            right = right,
            bottom = bottom
        )

    @Test
    fun testExtractTimeRange24Hr() {
        val range = TimetableParser.extractTimeRange("09:00 - 10:30")
        assertNotNull(range)
        assertEquals("09:00", range!!.first)
        assertEquals("10:30", range.second)

        val range2 = TimetableParser.extractTimeRange("Operating Systems 14:00 to 15:45 Room 301")
        assertNotNull(range2)
        assertEquals("14:00", range2!!.first)
        assertEquals("15:45", range2.second)
    }

    @Test
    fun testExtractTimeRange12HrAmPm() {
        val range = TimetableParser.extractTimeRange("9:00 AM - 10:00 AM")
        assertNotNull(range)
        assertEquals("09:00", range!!.first)
        assertEquals("10:00", range.second)

        val range2 = TimetableParser.extractTimeRange("Calculus 1:30 PM - 3:00 PM Hall B")
        assertNotNull(range2)
        assertEquals("13:30", range2!!.first)
        assertEquals("15:00", range2.second)
    }

    @Test
    fun testNoonTimeParsing() {
        // 11.00AM-12.00NOON should parse as 11:00 to 12:00
        val range1 = TimetableParser.extractTimeRange("11.00AM-12.00NOON")
        assertNotNull(range1)
        assertEquals("11:00", range1!!.first)
        assertEquals("12:00", range1.second)

        // 12.00 NOON - 1.00 PM should parse as 12:00 to 13:00
        val range2 = TimetableParser.extractTimeRange("12.00 NOON - 1.00 PM")
        assertNotNull(range2)
        assertEquals("12:00", range2!!.first)
        assertEquals("13:00", range2.second)

        // 12.00NOON-1.00PM without spaces
        val range3 = TimetableParser.extractTimeRange("12.00NOON-1.00PM")
        assertNotNull(range3)
        assertEquals("12:00", range3!!.first)
        assertEquals("13:00", range3.second)

        // 1.50PM-2.50PM afternoon slot
        val range4 = TimetableParser.extractTimeRange("1.50PM-2.50PM")
        assertNotNull(range4)
        assertEquals("13:50", range4!!.first)
        assertEquals("14:50", range4.second)

        // 5.50PM-6.50PM
        val range5 = TimetableParser.extractTimeRange("5.50PM-6.50PM")
        assertNotNull(range5)
        assertEquals("17:50", range5!!.first)
        assertEquals("18:50", range5.second)
    }

    @Test
    fun testCleanSubjectAndRoom() {
        val (subj1, room1) = TimetableParser.cleanSubjectAndRoom("MLW1 (E-309)", "E-224")
        assertEquals("MLW1", subj1)
        assertEquals("E-309", room1)

        val (subj2, room2) = TimetableParser.cleanSubjectAndRoom("AD 1 LAB (E-309)", "E-224")
        assertEquals("AD 1 LAB", subj2)
        assertEquals("E-309", room2)

        val (subj3, room3) = TimetableParser.cleanSubjectAndRoom("CALC B", "SC 202")
        assertEquals("CALC B", subj3)
        assertEquals("SC 202", room3)

        val (subj4, room4) = TimetableParser.cleanSubjectAndRoom("Al 1", "E-224")
        assertEquals("AI 1", subj4)
        assertEquals("E-224", room4)

        val (subj5, room5) = TimetableParser.cleanSubjectAndRoom("MSD LAB (C-230)", "E-224")
        assertEquals("MCSD LAB", subj5)
        assertEquals("C-230", room5)
    }

    @Test
    fun testGridParserMultiPeriodSessions() {
        val items = mutableListOf<TimetableParser.TextItemWithBox>()

        // Header default room
        items.add(box("ROOM NO: - E-224", 300f, 20f, 500f, 40f))

        // Day label in header
        items.add(box("DAY", 20f, 70f, 70f, 90f))

        // 10 Time slot columns (x step = 80)
        val headerTexts = listOf(
            "8.00AM-9.00AM",
            "9.00AM-10.00AM",
            "10.00AM-11.00AM",
            "11.00AM-12.00NOON",
            "12.00 NOON - 1.00 PM",
            "1.50PM-2.50PM",
            "2.50PM-3.50PM",
            "3.50PM-4.50PM",
            "4.50PM-5.50PM",
            "5.50PM-6.50PM"
        )
        for (i in headerTexts.indices) {
            val left = 100f + i * 80f
            items.add(box(headerTexts[i], left, 70f, left + 75f, 90f))
        }

        // Days: Monday (yc=130), Tuesday (yc=180), Wednesday (yc=230), Thursday (yc=280), Friday (yc=330), Saturday (yc=380)
        val days = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY")
        for (i in days.indices) {
            val top = 120f + i * 50f
            items.add(box(days[i], 20f, top, 70f, top + 20f))
        }

        // Tuesday (yc=180):
        // MLW1 (E-309) spanning 3 periods: cols 0, 1, 2 (8:00 to 11:00). Centered around col 1 (x=217.5)
        items.add(box("MLW1 (E-309)", 175f, 170f, 260f, 190f))
        // AD 1 LAB (E-309) spanning 2 periods: cols 3, 4 (11:00 to 13:00). Centered around x=417.5
        items.add(box("AD 1 LAB (E-309)", 375f, 170f, 460f, 190f))
        // Free markers for cols 5, 6, 7, 8, 9
        for (c in 5..9) {
            val left = 100f + c * 80f + 30f
            items.add(box("X", left, 170f, left + 15f, 190f))
        }

        // Friday (yc=330):
        // Free markers for cols 0, 1, 2, 3, 4
        for (c in 0..4) {
            val left = 100f + c * 80f + 30f
            items.add(box("X", left, 320f, left + 15f, 340f))
        }
        // MCSD LAB (C-230) spanning 3 periods: cols 5, 6, 7 (13:50 to 16:50). Centered around col 6 (x=617.5)
        items.add(box("MCSD LAB (C-230)", 575f, 320f, 660f, 340f))
        // MLW1 (C-230) spanning 2 periods: cols 8, 9 (16:50 to 18:50). Centered around x=817.5
        items.add(box("MLW1 (C-230)", 775f, 320f, 860f, 340f))

        // Course legend footer below Saturday (yc > 420): MUST BE DISCARDED!
        items.add(box("CSE 2793 - MACHINE LEARNING WORKSHOP 1 - CR (4) - GP (5) - SAYANTAN SAHA", 20f, 440f, 600f, 455f))
        items.add(box("MTH 2002 - PROBABILITY AND STATISTICS - BIMALESH NAYAK", 20f, 460f, 500f, 475f))

        val parsed = TimetableParser.parseFromGridItems(items)

        // Tuesday checks
        val tuesdayEntries = parsed.filter { it.dayOfWeek == 2 }
        assertEquals(2, tuesdayEntries.size)

        // MLW1 8am to 11am
        val mlw1Tue = tuesdayEntries[0]
        assertEquals("MLW1", mlw1Tue.subjectCode)
        assertEquals("MLW1", mlw1Tue.subjectName)
        assertEquals("08:00", mlw1Tue.startTime)
        assertEquals("11:00", mlw1Tue.endTime)
        assertEquals("E-309", mlw1Tue.room)

        // AD 1 LAB 11am to 1pm (spans 11.00AM-12.00NOON and 12.00 NOON-1.00 PM)
        val ad1LabTue = tuesdayEntries[1]
        assertEquals("AD 1 LAB", ad1LabTue.subjectCode)
        assertEquals("AD 1 LAB", ad1LabTue.subjectName)
        assertEquals("11:00", ad1LabTue.startTime)
        assertEquals("13:00", ad1LabTue.endTime)
        assertEquals("E-309", ad1LabTue.room)

        // Friday checks
        val fridayEntries = parsed.filter { it.dayOfWeek == 5 }
        assertEquals(2, fridayEntries.size)

        // MCSD LAB 1:50 to 4:50 (3 periods)
        val mcsdFri = fridayEntries[0]
        assertEquals("MCSD LAB", mcsdFri.subjectCode)
        assertEquals("13:50", mcsdFri.startTime)
        assertEquals("16:50", mcsdFri.endTime)
        assertEquals("C-230", mcsdFri.room)

        // MLW1 4:50 to 6:50 (2 periods)
        val mlw1Fri = fridayEntries[1]
        assertEquals("MLW1", mlw1Fri.subjectCode)
        assertEquals("16:50", mlw1Fri.startTime)
        assertEquals("18:50", mlw1Fri.endTime)
        assertEquals("C-230", mlw1Fri.room)

        // Verify footer legend entries were strictly discarded: no Sayantan Saha, no Bimlesh Nayak
        assertTrue(parsed.none { it.subjectName.contains("SAYANTAN", ignoreCase = true) })
        assertTrue(parsed.none { it.subjectName.contains("BIMALESH", ignoreCase = true) })
    }

    @Test
    fun testMultiLineCellMergingInGrid() {
        val items = mutableListOf<TimetableParser.TextItemWithBox>()
        items.add(box("ROOM NO: - E-222", 300f, 20f, 500f, 40f))
        items.add(box("DAY", 20f, 70f, 70f, 90f))

        val headerTexts = listOf(
            "8.00AM-9.00AM", "9.00AM-10.00AM", "10.00AM-11.00AM", "11.00AM-12.00NOON",
            "12.00 NOON - 1.00 PM", "1.50PM-2.50PM", "2.50PM-3.50PM", "3.50PM-4.50PM",
            "4.50PM-5.50PM", "5.50PM-6.50PM"
        )
        for (i in headerTexts.indices) {
            val left = 100f + i * 80f
            items.add(box(headerTexts[i], left, 70f, left + 75f, 90f))
        }

        val days = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY")
        for (i in days.indices) {
            val top = 120f + i * 50f
            items.add(box(days[i], 20f, top, 70f, top + 20f))
        }

        // Thursday (yc=280):
        // Col 0 & 1: SCWP 1 (E-301) (8-10)
        items.add(box("SCWP 1 (E-301)", 110f, 270f, 210f, 290f))

        // Col 2 & 3: PSD LAB on line 1, (E-301) on line 2 (10-12)
        items.add(box("PSD LAB", 270f, 265f, 350f, 278f))
        items.add(box("(E-301)", 280f, 282f, 340f, 295f))

        // Col 4: ECAP (E-301) (12-1)
        items.add(box("ECAP (E-301)", 430f, 270f, 490f, 290f))

        // Col 5..9: X
        for (c in 5..9) {
            val left = 100f + c * 80f + 30f
            items.add(box("X", left, 270f, left + 15f, 290f))
        }

        val parsed = TimetableParser.parseFromGridItems(items)
        val thursday = parsed.filter { it.dayOfWeek == 4 }
        assertEquals(3, thursday.size)

        assertEquals("SCWP 1", thursday[0].subjectCode)
        assertEquals("08:00", thursday[0].startTime)
        assertEquals("10:00", thursday[0].endTime)

        assertEquals("PSD LAB", thursday[1].subjectCode)
        assertEquals("10:00", thursday[1].startTime)
        assertEquals("12:00", thursday[1].endTime)
        assertEquals("E-301", thursday[1].room)

        assertEquals("ECAP", thursday[2].subjectCode)
        assertEquals("12:00", thursday[2].startTime)
        assertEquals("13:00", thursday[2].endTime)
        assertEquals("E-301", thursday[2].room)
    }

    @Test
    fun testParseFromLinesWithDayHeaders() {
        val timetableText = """
            MONDAY
            09:00 - 10:00 Data Structures CS201 Room 101
            10:15 - 11:15 Discrete Mathematics MATH203
            
            WEDNESDAY
            02:00 PM - 03:30 PM Computer Networks CS304 Lab 2
        """.trimIndent()

        val parsed = TimetableParser.parseFromRawString(timetableText)
        assertEquals(3, parsed.size)

        // Item 1: Monday Data Structures
        assertEquals(1, parsed[0].dayOfWeek)
        assertEquals("09:00", parsed[0].startTime)
        assertEquals("10:00", parsed[0].endTime)
        assertTrue(parsed[0].subjectName.contains("Data Structures"))

        // Item 2: Monday Discrete Mathematics
        assertEquals(1, parsed[1].dayOfWeek)
        assertEquals("10:15", parsed[1].startTime)
        assertEquals("11:15", parsed[1].endTime)
        assertTrue(parsed[1].subjectName.contains("Discrete Mathematics"))

        // Item 3: Wednesday Computer Networks
        assertEquals(3, parsed[2].dayOfWeek)
        assertEquals("14:00", parsed[2].startTime)
        assertEquals("15:30", parsed[2].endTime)
        assertTrue(parsed[2].subjectName.contains("Computer Networks"))
    }

    @Test
    fun testCompleteTimetableSection25E1U2() {
        val items = mutableListOf<TimetableParser.TextItemWithBox>()
        // Header default room
        items.add(box("ROOM NO: - E-222", 300f, 20f, 500f, 40f))
        items.add(box("DAY", 20f, 70f, 70f, 90f))

        val headerTexts = listOf(
            "8.00AM-9.00AM", "9.00AM-10.00AM", "10.00AM-11.00AM", "11.00AM-12.00NOON",
            "12.00 NOON - 1.00 PM", "1.50PM-2.50PM", "2.50PM-3.50PM", "3.50PM-4.50PM",
            "4.50PM-5.50PM", "5.50PM-6.50PM"
        )
        for (i in headerTexts.indices) {
            val left = 100f + i * 80f
            items.add(box(headerTexts[i], left, 70f, left + 75f, 90f))
        }

        val days = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY")
        for (i in days.indices) {
            val top = 120f + i * 50f
            items.add(box(days[i], 20f, top, 70f, top + 20f))
        }

        // --- MONDAY (yc = 130) ---
        // Cols 0..4 = X
        for (c in 0..4) {
            val left = 100f + c * 80f + 30f
            items.add(box("X", left, 120f, left + 15f, 140f))
        }
        // Col 5..6: CT LAB (E-009) (13:50 - 15:50)
        items.add(box("CT LAB (E-009)", 510f, 120f, 620f, 140f))
        // Col 7: ECAP (E-301) (15:50 - 16:50)
        items.add(box("ECAP (E-301)", 670f, 120f, 730f, 140f))
        // Col 8..9: SCWP 1 (E-301) (16:50 - 18:50)
        items.add(box("SCWP 1 (E-301)", 750f, 120f, 860f, 140f))

        // --- THURSDAY (yc = 280) ---
        // Col 0..1: SCWP 1 (E-301) (08:00 - 10:00)
        items.add(box("SCWP 1 (E-301)", 110f, 270f, 220f, 290f))
        // Col 2..3: PSD LAB (E-301) (10:00 - 12:00)
        items.add(box("PSD LAB (E-301)", 270f, 270f, 380f, 290f))
        // Col 4: ECAP (E-301) (12:00 - 13:00)
        items.add(box("ECAP (E-301)", 430f, 270f, 490f, 290f))
        // Cols 5..9 = X
        for (c in 5..9) {
            val left = 100f + c * 80f + 30f
            items.add(box("X", left, 270f, left + 15f, 290f))
        }

        // --- SATURDAY (yc = 380) ---
        // Col 0: CA (08:00 - 09:00)
        items.add(box("CA", 110f, 370f, 170f, 390f))
        // Col 1: PS (09:00 - 10:00)
        items.add(box("PS", 190f, 370f, 250f, 390f))
        // Col 2: IM (10:00 - 11:00)
        items.add(box("IM", 270f, 370f, 330f, 390f))
        // Col 3: PSD (11:00 - 12:00)
        items.add(box("PSD", 350f, 370f, 410f, 390f))
        // Col 4: CT (12:00 - 13:00)
        items.add(box("CT", 430f, 370f, 490f, 390f))
        // Cols 5..9 = X
        for (c in 5..9) {
            val left = 100f + c * 80f + 30f
            items.add(box("X", left, 370f, left + 15f, 390f))
        }

        // Footer legend below Saturday (yc >= 430): Must be ignored!
        items.add(box("MTH 2002 - PROBABILITY AND STATISTICS - SANJIB NASKAR", 20f, 440f, 500f, 455f))
        items.add(box("GET 2011 - ELECTRONICS CIRCUIT ANALYSIS PRACTICE - SHUBHRANSHU MOHAN PARIDA", 20f, 460f, 600f, 475f))

        val parsed = TimetableParser.parseFromGridItems(items)

        // Monday tests
        val monday = parsed.filter { it.dayOfWeek == 1 }
        assertEquals(3, monday.size)
        assertEquals("CT LAB", monday[0].subjectCode)
        assertEquals("13:50", monday[0].startTime)
        assertEquals("15:50", monday[0].endTime)
        assertEquals("E-009", monday[0].room)

        assertEquals("ECAP", monday[1].subjectCode)
        assertEquals("15:50", monday[1].startTime)
        assertEquals("16:50", monday[1].endTime)
        assertEquals("E-301", monday[1].room)

        assertEquals("SCWP 1", monday[2].subjectCode)
        assertEquals("16:50", monday[2].startTime)
        assertEquals("18:50", monday[2].endTime)
        assertEquals("E-301", monday[2].room)

        // Thursday tests (Verify noon slot boundary between 10-12 and 12-1)
        val thursday = parsed.filter { it.dayOfWeek == 4 }
        assertEquals(3, thursday.size)
        assertEquals("SCWP 1", thursday[0].subjectCode)
        assertEquals("08:00", thursday[0].startTime)
        assertEquals("10:00", thursday[0].endTime)

        assertEquals("PSD LAB", thursday[1].subjectCode)
        assertEquals("10:00", thursday[1].startTime)
        assertEquals("12:00", thursday[1].endTime)

        assertEquals("ECAP", thursday[2].subjectCode)
        assertEquals("12:00", thursday[2].startTime)
        assertEquals("13:00", thursday[2].endTime)

        // Saturday tests (Verify consecutive noon slots 11-12 and 12-1)
        val saturday = parsed.filter { it.dayOfWeek == 6 }
        assertEquals(5, saturday.size)
        assertEquals("CA", saturday[0].subjectCode)
        assertEquals("08:00", saturday[0].startTime)
        assertEquals("09:00", saturday[0].endTime)

        assertEquals("PS", saturday[1].subjectCode)
        assertEquals("09:00", saturday[1].startTime)
        assertEquals("10:00", saturday[1].endTime)

        assertEquals("IM", saturday[2].subjectCode)
        assertEquals("10:00", saturday[2].startTime)
        assertEquals("11:00", saturday[2].endTime)

        assertEquals("PSD", saturday[3].subjectCode)
        assertEquals("11:00", saturday[3].startTime)
        assertEquals("12:00", saturday[3].endTime)

        assertEquals("CT", saturday[4].subjectCode)
        assertEquals("12:00", saturday[4].startTime)
        assertEquals("13:00", saturday[4].endTime)

        // Validate that consecutive noon slots do not overlap
        val psdSat = saturday[3]
        val ctSat = saturday[4]
        assertEquals("12:00", psdSat.endTime)
        assertEquals("12:00", ctSat.startTime)
        assertTrue(TimeNormalizer.compareTimes(psdSat.startTime, psdSat.endTime) < 0)
        assertTrue(TimeNormalizer.compareTimes(ctSat.startTime, ctSat.endTime) < 0)

        // Validate full output with TimetableValidator
        val report = TimetableValidator.validateEntries(parsed)
        assertTrue("Scanned timetable must pass validation", report.isValid)
        assertTrue(report.errors.isEmpty())

        // Ensure no teacher names from legend leaked
        assertTrue(parsed.none { it.subjectName.contains("SANJIB", ignoreCase = true) })
        assertTrue(parsed.none { it.subjectName.contains("PARIDA", ignoreCase = true) })
    }
}
