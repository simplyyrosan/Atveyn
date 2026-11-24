package com.atveyn.app.core.model

data class ClassScheduleItem(
    val sessionId: Long,
    val subjectId: Long,
    val subjectName: String,
    val subjectCode: String,
    val colorHex: String,
    val startTime: String,
    val endTime: String,
    val room: String,
    val date: String,
    val status: SessionStatus,
    val source: SessionSource,
    val isCurrent: Boolean = false,
    val isPast: Boolean = false
)

data class DayTimetableItem(
    val entryId: Long,
    val subjectId: Long,
    val subjectName: String,
    val subjectCode: String,
    val colorHex: String,
    val dayOfWeek: Int,
    val startTime: String,
    val endTime: String,
    val room: String,
    val isActive: Boolean = true
)
