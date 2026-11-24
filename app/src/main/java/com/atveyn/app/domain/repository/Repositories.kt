package com.atveyn.app.domain.repository

import com.atveyn.app.core.database.entity.AppSettingsEntity
import com.atveyn.app.core.database.entity.ClassSessionEntity
import com.atveyn.app.core.database.entity.HolidayEntity
import com.atveyn.app.core.database.entity.SubjectEntity
import com.atveyn.app.core.database.entity.TimetableEntryEntity
import com.atveyn.app.core.model.SessionStatus
import kotlinx.coroutines.flow.Flow

interface SubjectRepository {
    fun getActiveSubjects(): Flow<List<SubjectEntity>>
    fun getAllSubjects(): Flow<List<SubjectEntity>>
    fun getSubjectById(id: Long): Flow<SubjectEntity?>
    suspend fun getSubjectByIdSync(id: Long): SubjectEntity?
    suspend fun findExistingSubject(name: String, code: String): SubjectEntity?
    suspend fun saveSubject(subject: SubjectEntity): Long
    suspend fun saveSubjects(subjects: List<SubjectEntity>): List<Long>
    suspend fun updateSubject(subject: SubjectEntity)
    suspend fun setArchived(id: Long, isArchived: Boolean)
    suspend fun deleteSubject(subject: SubjectEntity)
    suspend fun getAllSubjectsSync(): List<SubjectEntity>
    suspend fun clearAll()
}

interface TimetableRepository {
    fun getAllActiveEntries(): Flow<List<TimetableEntryEntity>>
    fun getEntriesForDay(dayOfWeek: Int): Flow<List<TimetableEntryEntity>>
    suspend fun getEntriesForDaySync(dayOfWeek: Int): List<TimetableEntryEntity>
    suspend fun saveEntry(entry: TimetableEntryEntity): Long
    suspend fun saveEntries(entries: List<TimetableEntryEntity>): List<Long>
    suspend fun updateEntry(entry: TimetableEntryEntity)
    suspend fun deleteEntry(id: Long)
    suspend fun removeDuplicateEntries()
    suspend fun getAllEntriesSync(): List<TimetableEntryEntity>
    suspend fun clearAll()
}

interface AttendanceRepository {
    fun getAllSessions(): Flow<List<ClassSessionEntity>>
    fun getSessionsForDate(date: String): Flow<List<ClassSessionEntity>>
    suspend fun getSessionsForDateSync(date: String): List<ClassSessionEntity>
    fun getSessionsForSubject(subjectId: Long): Flow<List<ClassSessionEntity>>
    suspend fun getSessionsForSubjectSync(subjectId: Long): List<ClassSessionEntity>
    fun getSessionsBetweenDates(startDate: String, endDate: String): Flow<List<ClassSessionEntity>>
    suspend fun getSessionsBetweenDatesSync(startDate: String, endDate: String): List<ClassSessionEntity>
    fun getPendingSessions(): Flow<List<ClassSessionEntity>>
    suspend fun getPendingSessionsSync(): List<ClassSessionEntity>
    fun getSessionById(id: Long): Flow<ClassSessionEntity?>
    suspend fun getSessionByIdSync(id: Long): ClassSessionEntity?
    suspend fun saveSession(session: ClassSessionEntity): Long
    suspend fun saveSessions(sessions: List<ClassSessionEntity>): List<Long>
    suspend fun updateStatus(id: Long, status: SessionStatus)
    suspend fun updateSession(session: ClassSessionEntity)
    suspend fun deleteSession(id: Long)
    suspend fun findExistingScheduledSession(subjectId: Long, date: String, startTime: String): ClassSessionEntity?
    suspend fun getAllSessionsSync(): List<ClassSessionEntity>
    suspend fun getSessionByTimetableEntryAndDate(entryId: Long, date: String): ClassSessionEntity?
    suspend fun deletePendingSessionsForTimetableEntry(entryId: Long)
    suspend fun deleteOrphanedPendingSessions(date: String, activeEntryIds: List<Long>)
    suspend fun deleteAllPendingScheduledSessionsForDate(date: String)
    suspend fun removeDuplicateSessions()
    suspend fun deleteFuturePendingSessions(todayDate: String)
    suspend fun clearAll()
}

interface HolidayRepository {
    fun getAllHolidays(): Flow<List<HolidayEntity>>
    suspend fun isHoliday(date: String): Boolean
    suspend fun getHolidayForDate(date: String): HolidayEntity?
    suspend fun saveHoliday(holiday: HolidayEntity): Long
    suspend fun saveHolidays(holidays: List<HolidayEntity>): List<Long>
    suspend fun deleteHoliday(holiday: HolidayEntity)
    suspend fun deleteHolidayByDate(date: String)
    suspend fun getAllHolidaysSync(): List<HolidayEntity>
    suspend fun clearAll()
}

interface SettingsRepository {
    fun getSettings(): Flow<AppSettingsEntity>
    suspend fun getSettingsSync(): AppSettingsEntity
    suspend fun updateSettings(settings: AppSettingsEntity)
    suspend fun setDefaultThreshold(threshold: Double)
    suspend fun setNotificationsEnabled(enabled: Boolean)
    suspend fun setReminderDelayMinutes(minutes: Int)
    suspend fun setThemeMode(themeMode: String)
    suspend fun setSemesterDates(startDate: String?, endDate: String?)
    suspend fun setOnboardingCompleted(completed: Boolean)
}

