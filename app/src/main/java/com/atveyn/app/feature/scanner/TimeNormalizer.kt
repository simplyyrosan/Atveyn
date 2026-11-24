package com.atveyn.app.feature.scanner

import java.util.Locale
import java.util.regex.Pattern

/**
 * Dedicated Time Normalization Utility for OCR Timetable Parsing.
 *
 * Handles diverse OCR output formats including:
 * - 12:00NOON, 12.00 NOON, 12:00 NOON
 * - 12:00PM, 12.00 PM, 01.00 PM
 * - Dot and colon time separators (e.g. 11.00, 1.50)
 * - Explicit NOON normalization strictly to 12:00 PM (never midnight 00:00)
 * - Contextual AM/PM inference for daytime schedules when suffixes are omitted
 * - Accurate consecutive noon slot separation (11:00-12:00 and 12:00-13:00)
 */
object TimeNormalizer {

    private val TIME_RANGE_REGEX = Pattern.compile(
        "\\b(\\d{1,2})[.:](\\d{2})\\s*(AM|PM|NOON)?\\s*[-–—to]+\\s*(\\d{1,2})[.:](\\d{2})\\s*(AM|PM|NOON)?\\b",
        Pattern.CASE_INSENSITIVE
    )

    private val TIME_TOKEN_REGEX = Pattern.compile(
        "\\b(\\d{1,2})[.:](\\d{2})\\s*(AM|PM|NOON)?\\b",
        Pattern.CASE_INSENSITIVE
    )

    private val TIME_FORMAT_24H_REGEX = Pattern.compile("^([01]\\d|2[0-3]):([0-5]\\d)$")

    /**
     * Checks if a string representation of 24-hour time is valid "HH:mm" (00:00 to 23:59).
     */
    fun isValid24HourTime(time: String): Boolean {
        return TIME_FORMAT_24H_REGEX.matcher(time.trim()).matches()
    }

    /**
     * Compares two 24-hour time strings "HH:mm".
     * Returns negative if t1 < t2, zero if t1 == t2, positive if t1 > t2.
     */
    fun compareTimes(t1: String, t2: String): Int {
        val m1 = timeToMinutes(t1) ?: return 0
        val m2 = timeToMinutes(t2) ?: return 0
        return m1.compareTo(m2)
    }

    /**
     * Converts "HH:mm" string to minutes from midnight.
     */
    fun timeToMinutes(time: String): Int? {
        val parts = time.trim().split(":")
        if (parts.size != 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        if (h !in 0..23 || m !in 0..59) return null
        return h * 60 + m
    }

    /**
     * Formats hours and minutes into "HH:mm".
     */
    fun format24Hour(hour: Int, minute: Int): String {
        return String.format(Locale.US, "%02d:%02d", hour.coerceIn(0, 23), minute.coerceIn(0, 59))
    }

    /**
     * Normalizes raw OCR time text:
     * - Fixes common OCR misreads of "NOON" (e.g. N00N, NO0N, N0ON, N O O N)
     * - Normalizes standalone "12 NOON" to "12:00 PM"
     * - Replaces explicit "NOON" with "PM" so standard 12-hour parsing recognizes it
     * - Normalizes dot separators between numbers (e.g. "11.00" -> "11:00", "1.50" -> "1:50")
     */
    fun normalizeRawText(text: String): String {
        var norm = text.uppercase(Locale.US)
        
        // Fix OCR misreads for NOON
        norm = norm.replace(Regex("N[0O]{2}N"), "NOON")
        norm = norm.replace(Regex("N\\s+[0O]\\s+[0O]\\s+N"), "NOON")
        norm = norm.replace(Regex("\\b12\\s*N[0O]{2}N\\b"), "12:00 PM")
        norm = norm.replace(Regex("\\b12\\s*NOON\\b"), "12:00 PM")

        // Explicit NOON is strictly PM (12:00 PM), never AM or midnight!
        norm = norm.replace("NOON", "PM")

        // Convert dot time separator (e.g. 11.00 -> 11:00, 1.50 -> 1:50)
        norm = norm.replace(Regex("(\\d{1,2})\\.(\\d{2})"), "$1:$2")

        return norm
    }

    /**
     * Parses a single time string like "12:00NOON", "12.00 NOON", "11:00 AM", "01.00 PM", "8:00"
     * into standard 24-hour "HH:mm".
     *
     * @param raw Raw input time string
     * @param defaultAmPm Contextual AM/PM hint if suffix is absent (defaults to daytime school heuristic)
     */
    fun parseSingleTime(raw: String, defaultAmPm: String? = null): String? {
        val norm = normalizeRawText(raw)
        val m = TIME_TOKEN_REGEX.matcher(norm)
        if (m.find()) {
            var h = m.group(1)!!.toInt()
            val min = m.group(2)!!.toInt()
            val ampm = m.group(3)?.uppercase(Locale.US) ?: defaultAmPm?.uppercase(Locale.US)

            if (ampm != null) {
                if (ampm == "PM") {
                    if (h < 12) h += 12
                } else if (ampm == "AM") {
                    if (h == 12) h = 0
                }
            } else {
                // Contextual daytime inference:
                // Hours 8..11 -> AM (08:00 - 11:00)
                // Hour 12 -> 12:00 PM (midday noon, never midnight)
                // Hours 1..7 -> PM (13:00 - 19:00)
                if (h in 1..7) {
                    h += 12
                }
            }

            return format24Hour(h, min)
        }
        return null
    }

    /**
     * Extracts a time range pair Pair(startTime, endTime) in 24-hour "HH:mm" from raw text.
     * Correctly handles:
     * - "11.00AM-12.00NOON" -> Pair("11:00", "12:00")
     * - "12.00 NOON - 1.00 PM" -> Pair("12:00", "13:00")
     * - "12.00NOON-1.00PM" -> Pair("12:00", "13:00")
     * - "1.50PM-2.50PM" -> Pair("13:50", "14:50")
     * - "8.00AM-9.00AM" -> Pair("08:00", "09:00")
     * - "09:00 - 10:30" -> Pair("09:00", "10:30")
     */
    fun extractTimeRange(text: String): Pair<String, String>? {
        val norm = normalizeRawText(text)
        val m = TIME_RANGE_REGEX.matcher(norm)
        if (m.find()) {
            var h1 = m.group(1)!!.toInt()
            val min1 = m.group(2)!!.toInt()
            val ampm1 = m.group(3)?.uppercase(Locale.US)

            var h2 = m.group(4)!!.toInt()
            val min2 = m.group(5)!!.toInt()
            val ampm2 = m.group(6)?.uppercase(Locale.US)

            // Propagate suffix context across range: e.g. "8.00 - 9.00AM" or "11.00AM - 12.00"
            val effAmPm2 = ampm2 ?: if (ampm1 == "PM" && h2 < 12) "PM" else null
            val effAmPm1 = ampm1 ?: if (ampm2 == "AM" && h1 in 1..11) "AM" else null

            // Determine hour 1
            if (effAmPm1 != null) {
                if (effAmPm1 == "PM" && h1 < 12) h1 += 12
                else if (effAmPm1 == "AM" && h1 == 12) h1 = 0
            } else {
                if (h1 in 1..7) h1 += 12
                // Note: h1 == 12 without suffix in daytime defaults to 12 (12:00 PM)
            }

            // Determine hour 2
            if (effAmPm2 != null) {
                if (effAmPm2 == "PM" && h2 < 12) h2 += 12
                else if (effAmPm2 == "AM" && h2 == 12) h2 = 0
            } else {
                if (h2 in 1..7) h2 += 12
            }

            // Safety rule: if h2 < h1 (e.g. 11:00 - 1:00 or 12:00 - 1:00), h2 crosses into afternoon
            if (h2 < h1 && h2 < 12) {
                h2 += 12
            }

            // Safety rule: start == 11:00, end == 12:00 (11.00AM - 12.00NOON)
            // Ensure end hour is 12:00 PM (12:00), never 00:00
            if (h1 == 11 && h2 == 0) {
                h2 = 12
            }

            val startStr = format24Hour(h1, min1)
            val endStr = format24Hour(h2, min2)

            return Pair(startStr, endStr)
        }
        return null
    }

    /**
     * Extracts individual time tokens in order from text.
     */
    fun extractTimeTokens(text: String): List<String> {
        val result = mutableListOf<String>()
        val norm = normalizeRawText(text)
        val m = TIME_TOKEN_REGEX.matcher(norm)
        while (m.find()) {
            var h = m.group(1)!!.toInt()
            val min = m.group(2)!!.toInt()
            val ampm = m.group(3)?.uppercase(Locale.US)

            if (ampm != null) {
                if (ampm == "PM" && h < 12) h += 12
                if (ampm == "AM" && h == 12) h = 0
            } else {
                if (h in 1..7) h += 12
            }

            result.add(format24Hour(h, min))
        }
        return result
    }

    /**
     * Checks if the given text explicitly denotes midday NOON.
     */
    fun isNoon(text: String): Boolean {
        val u = text.uppercase(Locale.US)
        return u.contains("NOON") || u.contains("N00N") || u.contains("NO0N") || u.contains("N0ON")
    }
}
