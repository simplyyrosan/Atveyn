package com.atveyn.app.domain.usecase

import com.atveyn.app.core.database.entity.ClassSessionEntity
import com.atveyn.app.core.database.entity.SubjectEntity
import com.atveyn.app.core.model.AttendanceStatusType
import com.atveyn.app.core.model.OverallAttendanceSummary
import com.atveyn.app.core.model.SessionStatus
import com.atveyn.app.core.model.SubjectAttendanceSummary
import com.atveyn.app.core.util.AttendanceCalculator
import com.atveyn.app.core.util.DateTimeUtils
import javax.inject.Inject

class CalculateAttendanceUseCase @Inject constructor() {

    fun calculateForSubject(
        subject: SubjectEntity,
        sessions: List<ClassSessionEntity>
    ): SubjectAttendanceSummary {
        val subjectSessions = sessions.filter { it.subjectId == subject.id }
        var present = 0
        var absent = 0
        var pending = 0
        var cancelled = 0

        for (session in subjectSessions) {
            when (session.status) {
                SessionStatus.PRESENT -> present++
                SessionStatus.ABSENT -> absent++
                SessionStatus.PENDING -> {
                    // Only count as pending if the class has actually occurred / concluded
                    if (DateTimeUtils.isPastTime(session.date, session.endTime)) {
                        pending++
                    }
                }
                SessionStatus.CANCELLED -> cancelled++
            }
        }

        val conducted = AttendanceCalculator.calculateConducted(present, absent)
        val percentage = AttendanceCalculator.calculatePercentage(present, absent)
        val target = subject.targetAttendancePercentage
        val needed = AttendanceCalculator.calculateClassesNeeded(target, present, conducted)
        val margin = AttendanceCalculator.calculateMarginClasses(target, present, conducted)
        val status = AttendanceCalculator.calculateStatusType(percentage, target)

        return SubjectAttendanceSummary(
            subjectId = subject.id,
            subjectName = subject.name,
            subjectCode = subject.code,
            colorHex = subject.colorHex,
            targetThreshold = target,
            presentCount = present,
            absentCount = absent,
            conductedCount = conducted,
            pendingCount = pending,
            cancelledCount = cancelled,
            attendancePercentage = percentage,
            classesNeeded = needed,
            marginClasses = margin,
            statusType = status
        )
    }

    fun calculateOverall(
        subjects: List<SubjectEntity>,
        sessions: List<ClassSessionEntity>,
        overallTarget: Double = 75.0
    ): OverallAttendanceSummary {
        var totalPresent = 0
        var totalAbsent = 0
        var totalPending = 0
        var totalCancelled = 0

        val activeSubjectIds = subjects.filter { !it.isArchived }.map { it.id }.toSet()
        val relevantSessions = sessions.filter { it.subjectId in activeSubjectIds }

        for (session in relevantSessions) {
            when (session.status) {
                SessionStatus.PRESENT -> totalPresent++
                SessionStatus.ABSENT -> totalAbsent++
                SessionStatus.PENDING -> {
                    // Only count as pending if the class has actually occurred / concluded
                    if (DateTimeUtils.isPastTime(session.date, session.endTime)) {
                        totalPending++
                    }
                }
                SessionStatus.CANCELLED -> totalCancelled++
            }
        }

        val totalConducted = AttendanceCalculator.calculateConducted(totalPresent, totalAbsent)
        val overallPercentage = AttendanceCalculator.calculatePercentage(totalPresent, totalAbsent)
        val needed = AttendanceCalculator.calculateClassesNeeded(overallTarget, totalPresent, totalConducted)
        val status = AttendanceCalculator.calculateStatusType(overallPercentage, overallTarget)

        return OverallAttendanceSummary(
            totalConducted = totalConducted,
            totalPresent = totalPresent,
            totalAbsent = totalAbsent,
            totalPending = totalPending,
            totalCancelled = totalCancelled,
            overallPercentage = overallPercentage,
            overallTarget = overallTarget,
            classesNeededOverall = needed,
            statusType = status
        )
    }
}
