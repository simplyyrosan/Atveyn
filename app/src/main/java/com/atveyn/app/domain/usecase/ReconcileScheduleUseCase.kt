package com.atveyn.app.domain.usecase

import com.atveyn.app.core.database.entity.ClassSessionEntity
import com.atveyn.app.core.model.SessionSource
import com.atveyn.app.core.model.SessionStatus
import com.atveyn.app.core.scheduler.AlarmScheduler
import com.atveyn.app.core.util.DateTimeUtils
import com.atveyn.app.domain.repository.AttendanceRepository
import com.atveyn.app.domain.repository.HolidayRepository
import com.atveyn.app.domain.repository.SubjectRepository
import com.atveyn.app.domain.repository.TimetableRepository
import java.time.LocalDate
import javax.inject.Inject

class ReconcileScheduleUseCase @Inject constructor(
    private val timetableRepository: TimetableRepository,
    private val attendanceRepository: AttendanceRepository,
    private val holidayRepository: HolidayRepository,
    private val subjectRepository: SubjectRepository,
    private val alarmScheduler: AlarmScheduler
) {
    /**
     * Reconciles attendance sessions and alarms.
     * Reconciles past days up to today (i <= 0).
     * Future classes (date > today) are NEVER pre-created in the database as PENDING.
     */
    suspend operator fun invoke(daysPast: Int = 0): Int {
        val today = LocalDate.now()
        val todayStr = DateTimeUtils.formatDate(today)

        // 1. Purge any stray future pending sessions and remove duplicates
        timetableRepository.removeDuplicateEntries()
        attendanceRepository.deleteFuturePendingSessions(todayStr)
        attendanceRepository.removeDuplicateSessions()

        val subjects = subjectRepository.getAllSubjectsSync().associateBy { it.id }
        var reconciledCount = 0

        // 2. Reconcile past days and today
        for (i in -daysPast..0) {
            val date = today.plusDays(i.toLong())
            val dateStr = DateTimeUtils.formatDate(date)
            val dayOfWeek = DateTimeUtils.dayOfWeekInt(date)
            val isHoliday = holidayRepository.isHoliday(dateStr)

            val timetableEntries = timetableRepository.getEntriesForDaySync(dayOfWeek)

            // Remove any pending scheduled sessions that no longer belong to an active timetable entry
            val activeEntryIds = timetableEntries.map { it.id }
            if (activeEntryIds.isNotEmpty()) {
                attendanceRepository.deleteOrphanedPendingSessions(dateStr, activeEntryIds)
            } else {
                attendanceRepository.deleteAllPendingScheduledSessionsForDate(dateStr)
            }

            for (entry in timetableEntries) {
                // Find existing session by entry ID first, fallback to subject + time
                val existing = attendanceRepository.getSessionByTimetableEntryAndDate(entry.id, dateStr)
                    ?: attendanceRepository.findExistingScheduledSession(
                        subjectId = entry.subjectId,
                        date = dateStr,
                        startTime = entry.startTime
                    )

                if (isHoliday) {
                    if (existing == null) {
                        attendanceRepository.saveSession(
                            ClassSessionEntity(
                                subjectId = entry.subjectId,
                                timetableEntryId = entry.id,
                                date = dateStr,
                                startTime = entry.startTime,
                                endTime = entry.endTime,
                                status = SessionStatus.CANCELLED,
                                source = SessionSource.SCHEDULED,
                                notes = "Holiday"
                            )
                        )
                        reconciledCount++
                    } else if (existing.status == SessionStatus.PENDING) {
                        attendanceRepository.updateStatus(existing.id, SessionStatus.CANCELLED)
                        alarmScheduler.cancelAlarm(existing.id)
                    }
                } else {
                    if (existing == null) {
                        val newSessionId = attendanceRepository.saveSession(
                            ClassSessionEntity(
                                subjectId = entry.subjectId,
                                timetableEntryId = entry.id,
                                date = dateStr,
                                startTime = entry.startTime,
                                endTime = entry.endTime,
                                status = SessionStatus.PENDING,
                                source = SessionSource.SCHEDULED
                            )
                        )
                        reconciledCount++

                        // If today, schedule class-end alarm
                        if (i == 0) {
                            val createdSession = attendanceRepository.getSessionByIdSync(newSessionId)
                            val subject = subjects[entry.subjectId]
                            if (createdSession != null && subject != null) {
                                alarmScheduler.scheduleClassEndAlarm(
                                    session = createdSession,
                                    subjectName = subject.name,
                                    subjectCode = subject.code
                                )
                            }
                        }
                    } else if (existing.status == SessionStatus.PENDING) {
                        // Entry was updated or time changed: update the session in place so no duplicate is created!
                        val needsUpdate = existing.startTime != entry.startTime ||
                                existing.endTime != entry.endTime ||
                                existing.subjectId != entry.subjectId ||
                                existing.timetableEntryId != entry.id

                        val currentSession = if (needsUpdate) {
                            val updated = existing.copy(
                                subjectId = entry.subjectId,
                                timetableEntryId = entry.id,
                                startTime = entry.startTime,
                                endTime = entry.endTime,
                                updatedAt = System.currentTimeMillis()
                            )
                            attendanceRepository.updateSession(updated)
                            updated
                        } else {
                            existing
                        }

                        // Re-register alarm in case of time change or reboot for today
                        if (i == 0) {
                            val subject = subjects[entry.subjectId]
                            if (subject != null) {
                                alarmScheduler.scheduleClassEndAlarm(
                                    session = currentSession,
                                    subjectName = subject.name,
                                    subjectCode = subject.code
                                )
                            }
                        }
                    }
                }
            }
        }

        return reconciledCount
    }
}
