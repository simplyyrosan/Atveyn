package com.atveyn.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.atveyn.app.core.database.entity.AppSettingsEntity
import com.atveyn.app.core.database.entity.ClassSessionEntity
import com.atveyn.app.core.database.entity.HolidayEntity
import com.atveyn.app.core.database.entity.SubjectEntity
import com.atveyn.app.core.database.entity.TimetableEntryEntity
import com.atveyn.app.core.model.SessionStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects WHERE isArchived = 0 ORDER BY name ASC")
    fun getActiveSubjectsFlow(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects ORDER BY isArchived ASC, name ASC")
    fun getAllSubjectsFlow(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE id = :id")
    fun getSubjectByIdFlow(id: Long): Flow<SubjectEntity?>

    @Query("SELECT * FROM subjects WHERE id = :id")
    suspend fun getSubjectById(id: Long): SubjectEntity?

    @Query("SELECT * FROM subjects WHERE LOWER(TRIM(name)) = LOWER(TRIM(:name)) OR (code != '' AND LOWER(TRIM(code)) = LOWER(TRIM(:code))) LIMIT 1")
    suspend fun findExistingSubject(name: String, code: String): SubjectEntity?

    @Query("SELECT * FROM subjects")
    suspend fun getAllSubjects(): List<SubjectEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(subject: SubjectEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(subjects: List<SubjectEntity>): List<Long>

    @Update
    suspend fun update(subject: SubjectEntity)

    @Delete
    suspend fun delete(subject: SubjectEntity)

    @Query("UPDATE subjects SET isArchived = :isArchived WHERE id = :id")
    suspend fun setArchived(id: Long, isArchived: Boolean)

    @Query("DELETE FROM subjects")
    suspend fun clearAll()
}

@Dao
interface TimetableDao {
    @Query("SELECT * FROM timetable_entries WHERE isActive = 1 ORDER BY dayOfWeek ASC, startTime ASC")
    fun getAllActiveEntriesFlow(): Flow<List<TimetableEntryEntity>>

    @Query("SELECT * FROM timetable_entries ORDER BY dayOfWeek ASC, startTime ASC")
    fun getAllEntriesFlow(): Flow<List<TimetableEntryEntity>>

    @Query("SELECT * FROM timetable_entries")
    suspend fun getAllEntries(): List<TimetableEntryEntity>

    @Query("SELECT * FROM timetable_entries WHERE dayOfWeek = :dayOfWeek AND isActive = 1 ORDER BY startTime ASC")
    fun getEntriesForDayFlow(dayOfWeek: Int): Flow<List<TimetableEntryEntity>>

    @Query("SELECT * FROM timetable_entries WHERE dayOfWeek = :dayOfWeek AND isActive = 1 ORDER BY startTime ASC")
    suspend fun getEntriesForDay(dayOfWeek: Int): List<TimetableEntryEntity>

    @Query("SELECT * FROM timetable_entries WHERE subjectId = :subjectId")
    suspend fun getEntriesForSubject(subjectId: Long): List<TimetableEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: TimetableEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<TimetableEntryEntity>): List<Long>

    @Update
    suspend fun update(entry: TimetableEntryEntity)

    @Delete
    suspend fun delete(entry: TimetableEntryEntity)

    @Query("DELETE FROM timetable_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM timetable_entries WHERE id NOT IN (SELECT MIN(id) FROM timetable_entries GROUP BY dayOfWeek, startTime, subjectId)")
    suspend fun removeDuplicateEntries()

    @Query("DELETE FROM timetable_entries")
    suspend fun clearAll()
}

@Dao
interface ClassSessionDao {
    @Query("SELECT * FROM class_sessions ORDER BY date DESC, startTime DESC")
    fun getAllSessionsFlow(): Flow<List<ClassSessionEntity>>

    @Query("SELECT * FROM class_sessions")
    suspend fun getAllSessions(): List<ClassSessionEntity>

    @Query("SELECT * FROM class_sessions WHERE date = :date ORDER BY startTime ASC")
    fun getSessionsForDateFlow(date: String): Flow<List<ClassSessionEntity>>

    @Query("SELECT * FROM class_sessions WHERE date = :date ORDER BY startTime ASC")
    suspend fun getSessionsForDate(date: String): List<ClassSessionEntity>

    @Query("SELECT * FROM class_sessions WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC, startTime ASC")
    fun getSessionsBetweenDatesFlow(startDate: String, endDate: String): Flow<List<ClassSessionEntity>>

    @Query("SELECT * FROM class_sessions WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC, startTime ASC")
    suspend fun getSessionsBetweenDates(startDate: String, endDate: String): List<ClassSessionEntity>

    @Query("SELECT * FROM class_sessions WHERE subjectId = :subjectId ORDER BY date DESC, startTime DESC")
    fun getSessionsForSubjectFlow(subjectId: Long): Flow<List<ClassSessionEntity>>

    @Query("SELECT * FROM class_sessions WHERE subjectId = :subjectId ORDER BY date DESC, startTime DESC")
    suspend fun getSessionsForSubject(subjectId: Long): List<ClassSessionEntity>

    @Query("SELECT * FROM class_sessions WHERE status = 'PENDING' ORDER BY date ASC, startTime ASC")
    fun getPendingSessionsFlow(): Flow<List<ClassSessionEntity>>

    @Query("SELECT * FROM class_sessions WHERE status = 'PENDING' ORDER BY date ASC, startTime ASC")
    suspend fun getPendingSessions(): List<ClassSessionEntity>

    @Query("SELECT * FROM class_sessions WHERE id = :id")
    suspend fun getSessionById(id: Long): ClassSessionEntity?

    @Query("SELECT * FROM class_sessions WHERE id = :id")
    fun getSessionByIdFlow(id: Long): Flow<ClassSessionEntity?>

    @Query("SELECT * FROM class_sessions WHERE subjectId = :subjectId AND date = :date AND startTime = :startTime LIMIT 1")
    suspend fun findExistingScheduledSession(subjectId: Long, date: String, startTime: String): ClassSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: ClassSessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(sessions: List<ClassSessionEntity>): List<Long>

    @Update
    suspend fun update(session: ClassSessionEntity)

    @Query("UPDATE class_sessions SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: SessionStatus, updatedAt: Long = System.currentTimeMillis())

    @Delete
    suspend fun delete(session: ClassSessionEntity)

    @Query("DELETE FROM class_sessions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM class_sessions WHERE timetableEntryId = :entryId AND date = :date LIMIT 1")
    suspend fun getSessionByTimetableEntryAndDate(entryId: Long, date: String): ClassSessionEntity?

    @Query("DELETE FROM class_sessions WHERE timetableEntryId = :entryId AND status = 'PENDING'")
    suspend fun deletePendingSessionsForTimetableEntry(entryId: Long)

    @Query("DELETE FROM class_sessions WHERE date = :date AND timetableEntryId NOT IN (:activeEntryIds) AND status = 'PENDING' AND source = 'SCHEDULED'")
    suspend fun deleteOrphanedPendingSessions(date: String, activeEntryIds: List<Long>)

    @Query("DELETE FROM class_sessions WHERE date = :date AND status = 'PENDING' AND source = 'SCHEDULED'")
    suspend fun deleteAllPendingScheduledSessionsForDate(date: String)

    @Query("DELETE FROM class_sessions WHERE id NOT IN (SELECT MIN(id) FROM class_sessions GROUP BY date, startTime, subjectId)")
    suspend fun removeDuplicateSessions()

    @Query("DELETE FROM class_sessions WHERE status = 'PENDING' AND date > :todayDate")
    suspend fun deleteFuturePendingSessions(todayDate: String)

    @Query("DELETE FROM class_sessions")
    suspend fun clearAll()
}

@Dao
interface HolidayDao {
    @Query("SELECT * FROM holidays ORDER BY date ASC")
    fun getAllHolidaysFlow(): Flow<List<HolidayEntity>>

    @Query("SELECT * FROM holidays")
    suspend fun getAllHolidays(): List<HolidayEntity>

    @Query("SELECT * FROM holidays WHERE date = :date LIMIT 1")
    suspend fun getHolidayForDate(date: String): HolidayEntity?

    @Query("SELECT * FROM holidays WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    suspend fun getHolidaysBetweenDates(startDate: String, endDate: String): List<HolidayEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(holiday: HolidayEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(holidays: List<HolidayEntity>): List<Long>

    @Delete
    suspend fun delete(holiday: HolidayEntity)

    @Query("DELETE FROM holidays WHERE date = :date")
    suspend fun deleteByDate(date: String)

    @Query("DELETE FROM holidays")
    suspend fun clearAll()
}

@Dao
interface AppSettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    fun getSettingsFlow(): Flow<AppSettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettings(): AppSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: AppSettingsEntity)
}

