package com.atveyn.app.domain.usecase

import com.atveyn.app.core.database.entity.ClassSessionEntity
import com.atveyn.app.core.database.entity.SubjectEntity
import com.atveyn.app.core.model.AttendanceStatusType
import com.atveyn.app.core.model.SessionSource
import com.atveyn.app.core.model.SessionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class CalculateAttendanceUseCaseTest {

    private lateinit var useCase: CalculateAttendanceUseCase

    @Before
    fun setup() {
        useCase = CalculateAttendanceUseCase()
    }

    @Test
    fun testCalculateForSubject_ExcludesPendingAndCancelled() {
        val subject = SubjectEntity(
            id = 1L,
            name = "Mathematics",
            code = "MATH101",
            targetAttendancePercentage = 75.0
        )

        val sessions = listOf(
            ClassSessionEntity(id = 1, subjectId = 1, date = "2026-11-01", startTime = "09:00", endTime = "10:00", status = SessionStatus.PRESENT),
            ClassSessionEntity(id = 2, subjectId = 1, date = "2026-11-02", startTime = "09:00", endTime = "10:00", status = SessionStatus.PRESENT),
            ClassSessionEntity(id = 3, subjectId = 1, date = "2026-11-03", startTime = "09:00", endTime = "10:00", status = SessionStatus.PRESENT),
            ClassSessionEntity(id = 4, subjectId = 1, date = "2026-11-04", startTime = "09:00", endTime = "10:00", status = SessionStatus.ABSENT),
            ClassSessionEntity(id = 5, subjectId = 1, date = "2026-11-05", startTime = "09:00", endTime = "10:00", status = SessionStatus.PENDING),
            ClassSessionEntity(id = 6, subjectId = 1, date = "2026-11-06", startTime = "09:00", endTime = "10:00", status = SessionStatus.CANCELLED)
        )

        val summary = useCase.calculateForSubject(subject, sessions)

        // Conducted should be 3 Present + 1 Absent = 4.
        assertEquals(4, summary.conductedCount)
        assertEquals(3, summary.presentCount)
        assertEquals(1, summary.absentCount)
        assertEquals(1, summary.pendingCount)
        assertEquals(1, summary.cancelledCount)

        // Percentage: 3 / 4 * 100 = 75.0%
        assertEquals(75.0, summary.attendancePercentage!!, 0.001)

        // Since it's exactly at target (75.0% <= % < 80.0%), status is WARNING (near threshold)
        assertEquals(AttendanceStatusType.WARNING, summary.statusType)
        assertEquals(0, summary.classesNeeded)
        assertEquals(0, summary.marginClasses)
    }

    @Test
    fun testCalculateForSubject_ZeroConducted() {
        val subject = SubjectEntity(
            id = 2L,
            name = "Physics",
            code = "PHY101",
            targetAttendancePercentage = 75.0
        )

        val sessions = listOf(
            ClassSessionEntity(id = 10, subjectId = 2, date = "2026-11-10", startTime = "11:00", endTime = "12:00", status = SessionStatus.PENDING),
            ClassSessionEntity(id = 11, subjectId = 2, date = "2026-11-11", startTime = "11:00", endTime = "12:00", status = SessionStatus.CANCELLED)
        )

        val summary = useCase.calculateForSubject(subject, sessions)

        assertEquals(0, summary.conductedCount)
        assertEquals(0, summary.presentCount)
        assertEquals(0, summary.absentCount)
        assertEquals(1, summary.pendingCount)
        assertEquals(1, summary.cancelledCount)
        assertNull(summary.attendancePercentage)
        assertEquals(AttendanceStatusType.NO_DATA, summary.statusType)
    }

    @Test
    fun testCalculateOverall_MultipleSubjects() {
        val sub1 = SubjectEntity(id = 1L, name = "Data Structures", code = "CS201", targetAttendancePercentage = 75.0)
        val sub2 = SubjectEntity(id = 2L, name = "Algorithms", code = "CS202", targetAttendancePercentage = 75.0)
        val sub3Archived = SubjectEntity(id = 3L, name = "Old Subject", code = "OLD", isArchived = true)

        val subjects = listOf(sub1, sub2, sub3Archived)

        val sessions = listOf(
            // sub1: 4 present, 1 absent -> 4/5 (80%)
            ClassSessionEntity(id = 1, subjectId = 1, date = "2026-11-01", startTime = "09:00", endTime = "10:00", status = SessionStatus.PRESENT),
            ClassSessionEntity(id = 2, subjectId = 1, date = "2026-11-02", startTime = "09:00", endTime = "10:00", status = SessionStatus.PRESENT),
            ClassSessionEntity(id = 3, subjectId = 1, date = "2026-11-03", startTime = "09:00", endTime = "10:00", status = SessionStatus.PRESENT),
            ClassSessionEntity(id = 4, subjectId = 1, date = "2026-11-04", startTime = "09:00", endTime = "10:00", status = SessionStatus.PRESENT),
            ClassSessionEntity(id = 5, subjectId = 1, date = "2026-11-05", startTime = "09:00", endTime = "10:00", status = SessionStatus.ABSENT),

            // sub2: 2 present, 3 absent -> 2/5 (40%)
            ClassSessionEntity(id = 6, subjectId = 2, date = "2026-11-01", startTime = "10:00", endTime = "11:00", status = SessionStatus.PRESENT),
            ClassSessionEntity(id = 7, subjectId = 2, date = "2026-11-02", startTime = "10:00", endTime = "11:00", status = SessionStatus.PRESENT),
            ClassSessionEntity(id = 8, subjectId = 2, date = "2026-11-03", startTime = "10:00", endTime = "11:00", status = SessionStatus.ABSENT),
            ClassSessionEntity(id = 9, subjectId = 2, date = "2026-11-04", startTime = "10:00", endTime = "11:00", status = SessionStatus.ABSENT),
            ClassSessionEntity(id = 10, subjectId = 2, date = "2026-11-05", startTime = "10:00", endTime = "11:00", status = SessionStatus.ABSENT),

            // sub3 (Archived): should be excluded from active overall calculation
            ClassSessionEntity(id = 11, subjectId = 3, date = "2026-11-01", startTime = "14:00", endTime = "15:00", status = SessionStatus.ABSENT)
        )

        val overall = useCase.calculateOverall(subjects, sessions, overallTarget = 75.0)

        // Total conducted = 5 + 5 = 10. Present = 4 + 2 = 6. Absent = 1 + 3 = 4.
        assertEquals(10, overall.totalConducted)
        assertEquals(6, overall.totalPresent)
        assertEquals(4, overall.totalAbsent)

        // Overall % = 6/10 = 60.0%
        assertEquals(60.0, overall.overallPercentage!!, 0.001)

        // Below 75% -> SHORTAGE
        assertEquals(AttendanceStatusType.SHORTAGE, overall.statusType)

        // Classes needed: ceil((0.75 * 10 - 6) / 0.25) = ceil(1.5 / 0.25) = 6
        assertEquals(6, overall.classesNeededOverall)
    }
}
