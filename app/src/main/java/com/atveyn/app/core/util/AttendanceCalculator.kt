package com.atveyn.app.core.util

import com.atveyn.app.core.model.AttendanceStatusType
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

object AttendanceCalculator {

    /**
     * Conducted classes = Present + Absent.
     * Pending and cancelled classes are excluded.
     */
    fun calculateConducted(present: Int, absent: Int): Int {
        return present + absent
    }

    /**
     * Attendance percentage = Present / (Present + Absent) * 100.
     * Returns null if conducted == 0 (to display N/A).
     */
    fun calculatePercentage(present: Int, absent: Int): Double? {
        val conducted = calculateConducted(present, absent)
        return if (conducted == 0) {
            null
        } else {
            (present.toDouble() / conducted) * 100.0
        }
    }

    /**
     * For target threshold T as a decimal (e.g. 0.75 for 75%),
     * classes required when currently below target = max(0, ceil((T * conducted - present)/(1-T))).
     * Returns 0 if already at or above target or if no classes needed.
     */
    fun calculateClassesNeeded(targetPercentage: Double, present: Int, conducted: Int): Int {
        if (conducted == 0) return 0
        val t = targetPercentage / 100.0
        val currentFraction = present.toDouble() / conducted
        if (currentFraction >= t) {
            return 0
        }
        if (t >= 0.9999) {
            // Cannot mathematically recover to 100% if even 1 absence exists
            return if (present < conducted) Int.MAX_VALUE else 0
        }
        val raw = (t * conducted - present) / (1.0 - t)
        val needed = ceil(raw - 1e-9).toInt()
        return max(0, needed)
    }

    /**
     * Number of upcoming classes a student can safely miss while remaining at or above target threshold.
     * y = max(0, floor((present - T * conducted) / T))
     */
    fun calculateMarginClasses(targetPercentage: Double, present: Int, conducted: Int): Int {
        if (conducted == 0) return 0
        val t = targetPercentage / 100.0
        if (t <= 0.0) return Int.MAX_VALUE
        val currentFraction = present.toDouble() / conducted
        if (currentFraction < t) {
            return 0
        }
        val raw = (present - t * conducted) / t
        val margin = floor(raw + 1e-9).toInt()
        return max(0, margin)
    }

    /**
     * Status alert:
     * - Red shortage alert below threshold
     * - Amber near threshold (target <= % < target + 5%)
     * - Green above threshold (% >= target + 5%)
     * - NO_DATA if conducted == 0
     */
    fun calculateStatusType(percentage: Double?, targetPercentage: Double): AttendanceStatusType {
        if (percentage == null) return AttendanceStatusType.NO_DATA
        return when {
            percentage < targetPercentage -> AttendanceStatusType.SHORTAGE
            percentage < (targetPercentage + 5.0) -> AttendanceStatusType.WARNING
            else -> AttendanceStatusType.ON_TRACK
        }
    }
}
