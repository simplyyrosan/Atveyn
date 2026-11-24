package com.atveyn.app.feature.scanner

import com.atveyn.app.core.util.DateTimeUtils

/**
 * Validator for scanned timetable entries before saving to database.
 *
 * Enforces:
 * 1. Time parseability in valid 24-hour HH:mm.
 * 2. Start time must be strictly earlier than end time.
 * 3. Chronological validity and overlap detection per day.
 * 4. Duplicate entry detection.
 * 5. Distinction between consecutive noon slots (11:00-12:00 and 12:00-13:00).
 * 6. Detailed warnings/errors without silently discarding uncertain entries.
 */
object TimetableValidator {

    data class ValidationIssue(
        val entryId: String,
        val dayOfWeek: Int,
        val message: String,
        val isError: Boolean // true = hard error preventing save, false = warning
    )

    data class ValidationReport(
        val isValid: Boolean,
        val issues: List<ValidationIssue>
    ) {
        val errors: List<ValidationIssue> get() = issues.filter { it.isError }
        val warnings: List<ValidationIssue> get() = issues.filter { !it.isError }
    }

    /**
     * Validates a full list of parsed timetable entries.
     */
    fun validateEntries(entries: List<ParsedTimetableEntry>): ValidationReport {
        val issues = mutableListOf<ValidationIssue>()

        if (entries.isEmpty()) {
            issues.add(
                ValidationIssue(
                    entryId = "",
                    dayOfWeek = 0,
                    message = "No timetable entries to validate.",
                    isError = true
                )
            )
            return ValidationReport(isValid = false, issues = issues)
        }

        // 1. Per-entry individual checks
        for (entry in entries) {
            val dayName = DateTimeUtils.dayOfWeekName(entry.dayOfWeek)

            if (entry.subjectName.isBlank()) {
                issues.add(
                    ValidationIssue(
                        entryId = entry.id,
                        dayOfWeek = entry.dayOfWeek,
                        message = "[$dayName] Subject name is blank.",
                        isError = true
                    )
                )
            }

            if (!TimeNormalizer.isValid24HourTime(entry.startTime)) {
                issues.add(
                    ValidationIssue(
                        entryId = entry.id,
                        dayOfWeek = entry.dayOfWeek,
                        message = "[$dayName] Invalid start time: '${entry.startTime}'",
                        isError = true
                    )
                )
            }

            if (!TimeNormalizer.isValid24HourTime(entry.endTime)) {
                issues.add(
                    ValidationIssue(
                        entryId = entry.id,
                        dayOfWeek = entry.dayOfWeek,
                        message = "[$dayName] Invalid end time: '${entry.endTime}'",
                        isError = true
                    )
                )
            }

            val startMin = TimeNormalizer.timeToMinutes(entry.startTime)
            val endMin = TimeNormalizer.timeToMinutes(entry.endTime)

            if (startMin != null && endMin != null) {
                if (startMin >= endMin) {
                    issues.add(
                        ValidationIssue(
                            entryId = entry.id,
                            dayOfWeek = entry.dayOfWeek,
                            message = "[$dayName] Start time (${entry.startTime}) must be strictly earlier than end time (${entry.endTime}).",
                            isError = true
                        )
                    )
                }

                // Check for accidental midnight conversion (00:00 when 12:00 was intended)
                if (entry.startTime.startsWith("00:") || entry.endTime.startsWith("00:")) {
                    issues.add(
                        ValidationIssue(
                            entryId = entry.id,
                            dayOfWeek = entry.dayOfWeek,
                            message = "[$dayName] Midnight detected in slot (${entry.startTime} - ${entry.endTime}). Noon should be 12:00 PM.",
                            isError = false // Warning, user can review/edit
                        )
                    )
                }
            }
        }

        // 2. Cross-entry checks per day (Duplicates & Overlaps)
        val entriesByDay = entries.groupBy { it.dayOfWeek }

        for ((day, dayEntries) in entriesByDay) {
            val dayName = DateTimeUtils.dayOfWeekName(day)

            for (i in dayEntries.indices) {
                val e1 = dayEntries[i]
                val s1 = TimeNormalizer.timeToMinutes(e1.startTime) ?: continue
                val end1 = TimeNormalizer.timeToMinutes(e1.endTime) ?: continue

                for (j in (i + 1) until dayEntries.size) {
                    val e2 = dayEntries[j]
                    val s2 = TimeNormalizer.timeToMinutes(e2.startTime) ?: continue
                    val end2 = TimeNormalizer.timeToMinutes(e2.endTime) ?: continue

                    // Duplicate detection
                    if (e1.startTime == e2.startTime && e1.endTime == e2.endTime &&
                        e1.subjectName.equals(e2.subjectName, ignoreCase = true)) {
                        issues.add(
                            ValidationIssue(
                                entryId = e2.id,
                                dayOfWeek = day,
                                message = "[$dayName] Duplicate class detected for '${e1.subjectName}' at ${e1.startTime} - ${e1.endTime}.",
                                isError = true
                            )
                        )
                    } else {
                        // Interval Overlap: s1 < end2 and s2 < end1
                        val overlaps = s1 < end2 && s2 < end1
                        if (overlaps) {
                            issues.add(
                                ValidationIssue(
                                    entryId = e2.id,
                                    dayOfWeek = day,
                                    message = "[$dayName] Overlapping classes: '${e1.subjectName}' (${e1.startTime}-${e1.endTime}) and '${e2.subjectName}' (${e2.startTime}-${e2.endTime}).",
                                    isError = false // Warning: allow user to resolve or accept
                                )
                            )
                        }
                    }
                }
            }
        }

        val hasHardErrors = issues.any { it.isError }
        return ValidationReport(isValid = !hasHardErrors, issues = issues)
    }
}
