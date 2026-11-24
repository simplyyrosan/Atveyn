package com.atveyn.app.core.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateTimeUtils {
    val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)
    val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
    val DISPLAY_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.US)
    val DISPLAY_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
    val FORMATTER_12H: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)

    fun format12Hour(timeStr: String): String = try {
        parseTime(timeStr).format(FORMATTER_12H)
    } catch (e: Exception) {
        timeStr
    }

    fun formatTimeRange12h(startTimeStr: String, endTimeStr: String): String {
        return "${format12Hour(startTimeStr)} - ${format12Hour(endTimeStr)}"
    }

    fun todayDate(): String = LocalDate.now().format(DATE_FORMATTER)

    fun todayLocalDate(): LocalDate = LocalDate.now()

    fun parseDate(dateStr: String): LocalDate = LocalDate.parse(dateStr, DATE_FORMATTER)

    fun formatDate(date: LocalDate): String = date.format(DATE_FORMATTER)

    fun formatDisplayDate(dateStr: String): String = try {
        parseDate(dateStr).format(DISPLAY_DATE_FORMATTER)
    } catch (e: Exception) {
        dateStr
    }

    fun parseTime(timeStr: String): LocalTime = LocalTime.parse(timeStr, TIME_FORMATTER)

    fun formatTime(time: LocalTime): String = time.format(TIME_FORMATTER)

    fun formatDisplayTime(timeStr: String): String = try {
        parseTime(timeStr).format(DISPLAY_TIME_FORMATTER)
    } catch (e: Exception) {
        timeStr
    }

    /**
     * Day of week: 1 = Monday ... 7 = Sunday
     */
    fun dayOfWeekInt(date: LocalDate): Int = date.dayOfWeek.value

    fun dayOfWeekName(dayOfWeek: Int): String = when (dayOfWeek) {
        1 -> "Monday"
        2 -> "Tuesday"
        3 -> "Wednesday"
        4 -> "Thursday"
        5 -> "Friday"
        6 -> "Saturday"
        7 -> "Sunday"
        else -> "Unknown"
    }

    fun dayOfWeekShortName(dayOfWeek: Int): String = when (dayOfWeek) {
        1 -> "Mon"
        2 -> "Tue"
        3 -> "Wed"
        4 -> "Thu"
        5 -> "Fri"
        6 -> "Sat"
        7 -> "Sun"
        else -> "???"
    }

    /**
     * Checks if a session's end time is in the past compared to current time on current date.
     */
    fun isPastTime(dateStr: String, endTimeStr: String): Boolean {
        return try {
            val sessionDate = parseDate(dateStr)
            val today = LocalDate.now()
            if (sessionDate.isBefore(today)) return true
            if (sessionDate.isAfter(today)) return false
            val sessionEndTime = parseTime(endTimeStr)
            val now = LocalTime.now()
            sessionEndTime.isBefore(now)
        } catch (e: Exception) {
            false
        }
    }
}
