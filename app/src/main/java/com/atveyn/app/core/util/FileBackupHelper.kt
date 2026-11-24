package com.atveyn.app.core.util

import android.content.Context
import android.net.Uri
import com.atveyn.app.core.database.AtveynDatabase
import com.atveyn.app.core.database.entity.AppSettingsEntity
import com.atveyn.app.core.database.entity.ClassSessionEntity
import com.atveyn.app.core.database.entity.HolidayEntity
import com.atveyn.app.core.database.entity.SubjectEntity
import com.atveyn.app.core.database.entity.TimetableEntryEntity
import com.atveyn.app.core.model.SessionSource
import com.atveyn.app.core.model.SessionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream

object FileBackupHelper {

    suspend fun exportToJson(context: Context, database: AtveynDatabase, outputUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject()
            root.put("version", 1)
            root.put("exportedAt", System.currentTimeMillis())

            // Subjects
            val subjects = database.subjectDao().getAllSubjects()
            val subjectsArr = JSONArray()
            for (s in subjects) {
                val obj = JSONObject()
                obj.put("id", s.id)
                obj.put("name", s.name)
                obj.put("code", s.code)
                obj.put("colorHex", s.colorHex)
                obj.put("targetThreshold", s.targetAttendancePercentage)
                obj.put("isArchived", s.isArchived)
                obj.put("createdAt", s.createdAt)
                subjectsArr.put(obj)
            }
            root.put("subjects", subjectsArr)

            // Timetable Entries
            val entries = database.timetableDao().getAllEntries()
            val entriesArr = JSONArray()
            for (e in entries) {
                val obj = JSONObject()
                obj.put("id", e.id)
                obj.put("subjectId", e.subjectId)
                obj.put("dayOfWeek", e.dayOfWeek)
                obj.put("startTime", e.startTime)
                obj.put("endTime", e.endTime)
                obj.put("room", e.room)
                obj.put("isActive", e.isActive)
                entriesArr.put(obj)
            }
            root.put("timetableEntries", entriesArr)

            // Class Sessions
            val sessions = database.classSessionDao().getAllSessions()
            val sessionsArr = JSONArray()
            for (cs in sessions) {
                val obj = JSONObject()
                obj.put("id", cs.id)
                obj.put("subjectId", cs.subjectId)
                obj.put("timetableEntryId", cs.timetableEntryId ?: -1L)
                obj.put("date", cs.date)
                obj.put("startTime", cs.startTime)
                obj.put("endTime", cs.endTime)
                obj.put("status", cs.status.name)
                obj.put("source", cs.source.name)
                obj.put("notes", cs.notes)
                obj.put("createdAt", cs.createdAt)
                obj.put("updatedAt", cs.updatedAt)
                sessionsArr.put(obj)
            }
            root.put("classSessions", sessionsArr)

            // Holidays
            val holidays = database.holidayDao().getAllHolidays()
            val holidaysArr = JSONArray()
            for (h in holidays) {
                val obj = JSONObject()
                obj.put("id", h.id)
                obj.put("date", h.date)
                obj.put("name", h.name)
                obj.put("isFullDay", h.isFullDay)
                holidaysArr.put(obj)
            }
            root.put("holidays", holidaysArr)

            // Write to OutputStream
            context.contentResolver.openOutputStream(outputUri)?.use { os: OutputStream ->
                os.write(root.toString(2).toByteArray(Charsets.UTF_8))
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun importFromJson(context: Context, database: AtveynDatabase, inputUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.contentResolver.openInputStream(inputUri)?.use { input: InputStream ->
                input.bufferedReader(Charsets.UTF_8).readText()
            } ?: return@withContext false

            val root = JSONObject(jsonString)

            // Clear current data before restore
            database.classSessionDao().clearAll()
            database.timetableDao().clearAll()
            database.subjectDao().clearAll()
            database.holidayDao().clearAll()

            // Restore Subjects
            val subjectsArr = root.optJSONArray("subjects") ?: JSONArray()
            val subjects = mutableListOf<SubjectEntity>()
            for (i in 0 until subjectsArr.length()) {
                val obj = subjectsArr.getJSONObject(i)
                subjects.add(
                    SubjectEntity(
                        id = obj.optLong("id", 0L),
                        name = obj.getString("name"),
                        code = obj.optString("code", ""),
                        colorHex = obj.optString("colorHex", "#6366F1"),
                        targetAttendancePercentage = obj.optDouble("targetThreshold", 75.0),
                        isArchived = obj.optBoolean("isArchived", false),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
            database.subjectDao().insertAll(subjects)

            // Restore Timetable Entries
            val entriesArr = root.optJSONArray("timetableEntries") ?: JSONArray()
            val entries = mutableListOf<TimetableEntryEntity>()
            for (i in 0 until entriesArr.length()) {
                val obj = entriesArr.getJSONObject(i)
                entries.add(
                    TimetableEntryEntity(
                        id = obj.optLong("id", 0L),
                        subjectId = obj.getLong("subjectId"),
                        dayOfWeek = obj.getInt("dayOfWeek"),
                        startTime = obj.getString("startTime"),
                        endTime = obj.getString("endTime"),
                        room = obj.optString("room", ""),
                        isActive = obj.optBoolean("isActive", true)
                    )
                )
            }
            database.timetableDao().insertAll(entries)

            // Restore Class Sessions
            val sessionsArr = root.optJSONArray("classSessions") ?: JSONArray()
            val sessions = mutableListOf<ClassSessionEntity>()
            for (i in 0 until sessionsArr.length()) {
                val obj = sessionsArr.getJSONObject(i)
                val tId = obj.optLong("timetableEntryId", -1L)
                val statusStr = obj.optString("status", "PENDING")
                val sourceStr = obj.optString("source", "SCHEDULED")

                sessions.add(
                    ClassSessionEntity(
                        id = obj.optLong("id", 0L),
                        subjectId = obj.getLong("subjectId"),
                        timetableEntryId = if (tId > 0) tId else null,
                        date = obj.getString("date"),
                        startTime = obj.getString("startTime"),
                        endTime = obj.getString("endTime"),
                        status = try { SessionStatus.valueOf(statusStr) } catch (e: Exception) { SessionStatus.PENDING },
                        source = try { SessionSource.valueOf(sourceStr) } catch (e: Exception) { SessionSource.SCHEDULED },
                        notes = obj.optString("notes", ""),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }
            database.classSessionDao().insertAll(sessions)

            // Restore Holidays
            val holidaysArr = root.optJSONArray("holidays") ?: JSONArray()
            val holidays = mutableListOf<HolidayEntity>()
            for (i in 0 until holidaysArr.length()) {
                val obj = holidaysArr.getJSONObject(i)
                holidays.add(
                    HolidayEntity(
                        id = obj.optLong("id", 0L),
                        date = obj.getString("date"),
                        name = obj.getString("name"),
                        isFullDay = obj.optBoolean("isFullDay", true)
                    )
                )
            }
            database.holidayDao().insertAll(holidays)

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
