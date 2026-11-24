package com.atveyn.app.core.model

enum class SessionStatus {
    PENDING,
    PRESENT,
    ABSENT,
    CANCELLED
}

enum class SessionSource {
    SCHEDULED,
    EXTRA_CLASS,
    MANUAL
}

enum class AttendanceStatusType {
    ON_TRACK,  // >= target
    WARNING,   // between (target - 5%) and target
    SHORTAGE,  // < (target - 5%)
    NO_DATA    // 0 conducted classes
}

data class SubjectAttendanceSummary(
    val subjectId: Long,
    val subjectName: String,
    val subjectCode: String,
    val colorHex: String,
    val targetThreshold: Double,
    val presentCount: Int,
    val absentCount: Int,
    val conductedCount: Int,
    val pendingCount: Int,
    val cancelledCount: Int,
    val attendancePercentage: Double?,
    val classesNeeded: Int,
    val marginClasses: Int,
    val statusType: AttendanceStatusType
)

data class OverallAttendanceSummary(
    val totalConducted: Int,
    val totalPresent: Int,
    val totalAbsent: Int,
    val totalPending: Int,
    val totalCancelled: Int,
    val overallPercentage: Double?,
    val overallTarget: Double,
    val classesNeededOverall: Int,
    val statusType: AttendanceStatusType
)
