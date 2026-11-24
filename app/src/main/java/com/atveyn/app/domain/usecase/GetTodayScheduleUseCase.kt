package com.atveyn.app.domain.usecase

import com.atveyn.app.core.model.ClassScheduleItem
import com.atveyn.app.core.util.DateTimeUtils
import com.atveyn.app.domain.repository.AttendanceRepository
import com.atveyn.app.domain.repository.SubjectRepository
import com.atveyn.app.domain.repository.TimetableRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

class GetTodayScheduleUseCase @Inject constructor(
    private val attendanceRepository: AttendanceRepository,
    private val subjectRepository: SubjectRepository,
    private val timetableRepository: TimetableRepository
) {
    operator fun invoke(date: String = DateTimeUtils.todayDate()): Flow<List<ClassScheduleItem>> {
        return combine(
            attendanceRepository.getSessionsForDate(date),
            subjectRepository.getAllSubjects(),
            timetableRepository.getAllActiveEntries()
        ) { sessions, subjects, timetableEntries ->
            val subjectsMap = subjects.associateBy { it.id }
            val timetableMap = timetableEntries.associateBy { it.id }

            val now = LocalTime.now()
            val isToday = date == DateTimeUtils.todayDate()

            sessions.map { session ->
                val subject = subjectsMap[session.subjectId]
                val timetableEntry = session.timetableEntryId?.let { timetableMap[it] }

                val sTime = DateTimeUtils.parseTime(session.startTime)
                val eTime = DateTimeUtils.parseTime(session.endTime)

                val isCurrent = isToday && !now.isBefore(sTime) && !now.isAfter(eTime)
                val isPast = if (isToday) now.isAfter(eTime) else DateTimeUtils.parseDate(date).isBefore(LocalDate.now())

                ClassScheduleItem(
                    sessionId = session.id,
                    subjectId = session.subjectId,
                    subjectName = subject?.name ?: "Unknown Subject",
                    subjectCode = subject?.code ?: "N/A",
                    colorHex = subject?.colorHex ?: "#6366F1",
                    startTime = session.startTime,
                    endTime = session.endTime,
                    room = timetableEntry?.room ?: "",
                    date = session.date,
                    status = session.status,
                    source = session.source,
                    isCurrent = isCurrent,
                    isPast = isPast
                )
            }.sortedBy { it.startTime }
        }
    }
}
