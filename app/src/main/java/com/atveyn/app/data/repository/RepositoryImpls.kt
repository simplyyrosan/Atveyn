package com.atveyn.app.data.repository

import com.atveyn.app.core.database.dao.AppSettingsDao
import com.atveyn.app.core.database.dao.ClassSessionDao
import com.atveyn.app.core.database.dao.HolidayDao
import com.atveyn.app.core.database.dao.SubjectDao
import com.atveyn.app.core.database.dao.TimetableDao
import com.atveyn.app.core.database.entity.AppSettingsEntity
import com.atveyn.app.core.database.entity.ClassSessionEntity
import com.atveyn.app.core.database.entity.HolidayEntity
import com.atveyn.app.core.database.entity.SubjectEntity
import com.atveyn.app.core.database.entity.TimetableEntryEntity
import com.atveyn.app.core.model.SessionStatus
import com.atveyn.app.domain.repository.AttendanceRepository
import com.atveyn.app.domain.repository.HolidayRepository
import com.atveyn.app.domain.repository.SettingsRepository
import com.atveyn.app.domain.repository.SubjectRepository
import com.atveyn.app.domain.repository.TimetableRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubjectRepositoryImpl @Inject constructor(
    private val subjectDao: SubjectDao
) : SubjectRepository {
    override fun getActiveSubjects(): Flow<List<SubjectEntity>> = subjectDao.getActiveSubjectsFlow()
    override fun getAllSubjects(): Flow<List<SubjectEntity>> = subjectDao.getAllSubjectsFlow()
    override fun getSubjectById(id: Long): Flow<SubjectEntity?> = subjectDao.getSubjectByIdFlow(id)
    override suspend fun getSubjectByIdSync(id: Long): SubjectEntity? = subjectDao.getSubjectById(id)
    override suspend fun findExistingSubject(name: String, code: String): SubjectEntity? = subjectDao.findExistingSubject(name, code)
    override suspend fun saveSubject(subject: SubjectEntity): Long = subjectDao.insert(subject)
    override suspend fun saveSubjects(subjects: List<SubjectEntity>): List<Long> = subjectDao.insertAll(subjects)
    override suspend fun updateSubject(subject: SubjectEntity) = subjectDao.update(subject)
    override suspend fun setArchived(id: Long, isArchived: Boolean) = subjectDao.setArchived(id, isArchived)
    override suspend fun deleteSubject(subject: SubjectEntity) = subjectDao.delete(subject)
    override suspend fun getAllSubjectsSync(): List<SubjectEntity> = subjectDao.getAllSubjects()
    override suspend fun clearAll() = subjectDao.clearAll()
}

@Singleton
class TimetableRepositoryImpl @Inject constructor(
    private val timetableDao: TimetableDao
) : TimetableRepository {
    override fun getAllActiveEntries(): Flow<List<TimetableEntryEntity>> = timetableDao.getAllActiveEntriesFlow()
    override fun getEntriesForDay(dayOfWeek: Int): Flow<List<TimetableEntryEntity>> = timetableDao.getEntriesForDayFlow(dayOfWeek)
    override suspend fun getEntriesForDaySync(dayOfWeek: Int): List<TimetableEntryEntity> = timetableDao.getEntriesForDay(dayOfWeek)
    override suspend fun saveEntry(entry: TimetableEntryEntity): Long = timetableDao.insert(entry)
    override suspend fun saveEntries(entries: List<TimetableEntryEntity>): List<Long> = timetableDao.insertAll(entries)
    override suspend fun updateEntry(entry: TimetableEntryEntity) = timetableDao.update(entry)
    override suspend fun deleteEntry(id: Long) = timetableDao.deleteById(id)
    override suspend fun removeDuplicateEntries() = timetableDao.removeDuplicateEntries()
    override suspend fun getAllEntriesSync(): List<TimetableEntryEntity> = timetableDao.getAllEntries()
    override suspend fun clearAll() = timetableDao.clearAll()
}

@Singleton
class AttendanceRepositoryImpl @Inject constructor(
    private val classSessionDao: ClassSessionDao
) : AttendanceRepository {
    override fun getAllSessions(): Flow<List<ClassSessionEntity>> = classSessionDao.getAllSessionsFlow()
    override fun getSessionsForDate(date: String): Flow<List<ClassSessionEntity>> = classSessionDao.getSessionsForDateFlow(date)
    override suspend fun getSessionsForDateSync(date: String): List<ClassSessionEntity> = classSessionDao.getSessionsForDate(date)
    override fun getSessionsForSubject(subjectId: Long): Flow<List<ClassSessionEntity>> = classSessionDao.getSessionsForSubjectFlow(subjectId)
    override suspend fun getSessionsForSubjectSync(subjectId: Long): List<ClassSessionEntity> = classSessionDao.getSessionsForSubject(subjectId)
    override fun getSessionsBetweenDates(startDate: String, endDate: String): Flow<List<ClassSessionEntity>> =
        classSessionDao.getSessionsBetweenDatesFlow(startDate, endDate)
    override suspend fun getSessionsBetweenDatesSync(startDate: String, endDate: String): List<ClassSessionEntity> =
        classSessionDao.getSessionsBetweenDates(startDate, endDate)
    override fun getPendingSessions(): Flow<List<ClassSessionEntity>> = classSessionDao.getPendingSessionsFlow()
    override suspend fun getPendingSessionsSync(): List<ClassSessionEntity> = classSessionDao.getPendingSessions()
    override fun getSessionById(id: Long): Flow<ClassSessionEntity?> = classSessionDao.getSessionByIdFlow(id)
    override suspend fun getSessionByIdSync(id: Long): ClassSessionEntity? = classSessionDao.getSessionById(id)
    override suspend fun saveSession(session: ClassSessionEntity): Long = classSessionDao.insert(session)
    override suspend fun saveSessions(sessions: List<ClassSessionEntity>): List<Long> = classSessionDao.insertAll(sessions)
    override suspend fun updateStatus(id: Long, status: SessionStatus) = classSessionDao.updateStatus(id, status)
    override suspend fun updateSession(session: ClassSessionEntity) = classSessionDao.update(session)
    override suspend fun deleteSession(id: Long) = classSessionDao.deleteById(id)
    override suspend fun findExistingScheduledSession(subjectId: Long, date: String, startTime: String): ClassSessionEntity? =
        classSessionDao.findExistingScheduledSession(subjectId, date, startTime)
    override suspend fun getAllSessionsSync(): List<ClassSessionEntity> = classSessionDao.getAllSessions()
    override suspend fun getSessionByTimetableEntryAndDate(entryId: Long, date: String): ClassSessionEntity? =
        classSessionDao.getSessionByTimetableEntryAndDate(entryId, date)
    override suspend fun deletePendingSessionsForTimetableEntry(entryId: Long) =
        classSessionDao.deletePendingSessionsForTimetableEntry(entryId)
    override suspend fun deleteOrphanedPendingSessions(date: String, activeEntryIds: List<Long>) =
        classSessionDao.deleteOrphanedPendingSessions(date, activeEntryIds)
    override suspend fun deleteAllPendingScheduledSessionsForDate(date: String) =
        classSessionDao.deleteAllPendingScheduledSessionsForDate(date)
    override suspend fun removeDuplicateSessions() =
        classSessionDao.removeDuplicateSessions()
    override suspend fun deleteFuturePendingSessions(todayDate: String) = classSessionDao.deleteFuturePendingSessions(todayDate)

    override suspend fun clearAll() = classSessionDao.clearAll()
}

@Singleton
class HolidayRepositoryImpl @Inject constructor(
    private val holidayDao: HolidayDao
) : HolidayRepository {
    override fun getAllHolidays(): Flow<List<HolidayEntity>> = holidayDao.getAllHolidaysFlow()
    override suspend fun isHoliday(date: String): Boolean = holidayDao.getHolidayForDate(date) != null
    override suspend fun getHolidayForDate(date: String): HolidayEntity? = holidayDao.getHolidayForDate(date)
    override suspend fun saveHoliday(holiday: HolidayEntity): Long = holidayDao.insert(holiday)
    override suspend fun saveHolidays(holidays: List<HolidayEntity>): List<Long> = holidayDao.insertAll(holidays)
    override suspend fun deleteHoliday(holiday: HolidayEntity) = holidayDao.delete(holiday)
    override suspend fun deleteHolidayByDate(date: String) = holidayDao.deleteByDate(date)
    override suspend fun getAllHolidaysSync(): List<HolidayEntity> = holidayDao.getAllHolidays()
    override suspend fun clearAll() = holidayDao.clearAll()
}

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val appSettingsDao: AppSettingsDao
) : SettingsRepository {
    private val defaultSettings = AppSettingsEntity(
        id = 1,
        defaultThreshold = 75.0,
        notificationsEnabled = true,
        reminderDelayMinutes = 15,
        themeMode = "SYSTEM",
        hasCompletedOnboarding = false
    )

    override fun getSettings(): Flow<AppSettingsEntity> {
        return appSettingsDao.getSettingsFlow().map { it ?: defaultSettings }
    }

    override suspend fun getSettingsSync(): AppSettingsEntity {
        return appSettingsDao.getSettings() ?: defaultSettings
    }

    override suspend fun updateSettings(settings: AppSettingsEntity) {
        appSettingsDao.insertOrUpdate(settings)
    }

    override suspend fun setDefaultThreshold(threshold: Double) {
        val current = getSettingsSync()
        updateSettings(current.copy(defaultThreshold = threshold))
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        val current = getSettingsSync()
        updateSettings(current.copy(notificationsEnabled = enabled))
    }

    override suspend fun setReminderDelayMinutes(minutes: Int) {
        val current = getSettingsSync()
        updateSettings(current.copy(reminderDelayMinutes = minutes))
    }

    override suspend fun setThemeMode(themeMode: String) {
        val current = getSettingsSync()
        updateSettings(current.copy(themeMode = themeMode))
    }

    override suspend fun setSemesterDates(startDate: String?, endDate: String?) {
        val current = getSettingsSync()
        updateSettings(current.copy(semesterStartDate = startDate, semesterEndDate = endDate))
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        val current = getSettingsSync()
        updateSettings(current.copy(hasCompletedOnboarding = completed))
    }
}

