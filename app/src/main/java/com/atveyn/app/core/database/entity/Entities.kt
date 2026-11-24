package com.atveyn.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.atveyn.app.core.model.SessionSource
import com.atveyn.app.core.model.SessionStatus

@Entity(
    tableName = "subjects",
    indices = [
        Index(value = ["code"], unique = false),
        Index(value = ["isArchived"])
    ]
)
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val code: String,
    val colorHex: String = "#6366F1",
    val targetAttendancePercentage: Double = 75.0,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "timetable_entries",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["subjectId"]),
        Index(value = ["dayOfWeek"])
    ]
)
data class TimetableEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subjectId: Long,
    val dayOfWeek: Int, // 1 = Monday ... 7 = Sunday
    val startTime: String, // "HH:mm"
    val endTime: String,   // "HH:mm"
    val room: String = "",
    val isActive: Boolean = true
)

@Entity(
    tableName = "class_sessions",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TimetableEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["timetableEntryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["subjectId"]),
        Index(value = ["date"]),
        Index(value = ["status"]),
        Index(value = ["timetableEntryId"]),
        Index(value = ["date", "startTime", "subjectId"], unique = false)
    ]
)
data class ClassSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subjectId: Long,
    val timetableEntryId: Long? = null,
    val date: String, // "yyyy-MM-dd"
    val startTime: String, // "HH:mm"
    val endTime: String,   // "HH:mm"
    val status: SessionStatus = SessionStatus.PENDING,
    val source: SessionSource = SessionSource.SCHEDULED,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "holidays",
    indices = [
        Index(value = ["date"], unique = true)
    ]
)
data class HolidayEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String, // "yyyy-MM-dd"
    val name: String,
    val isFullDay: Boolean = true
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val defaultThreshold: Double = 75.0,
    val notificationsEnabled: Boolean = true,
    val reminderDelayMinutes: Int = 15,
    val themeMode: String = "SYSTEM",
    val semesterStartDate: String? = null,
    val semesterEndDate: String? = null,
    val hasCompletedOnboarding: Boolean = false
)
