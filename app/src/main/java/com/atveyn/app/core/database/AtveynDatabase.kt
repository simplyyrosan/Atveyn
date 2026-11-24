package com.atveyn.app.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        SubjectEntity::class,
        TimetableEntryEntity::class,
        ClassSessionEntity::class,
        HolidayEntity::class,
        AppSettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AtveynDatabase : RoomDatabase() {
    abstract fun subjectDao(): SubjectDao
    abstract fun timetableDao(): TimetableDao
    abstract fun classSessionDao(): ClassSessionDao
    abstract fun holidayDao(): HolidayDao
    abstract fun appSettingsDao(): AppSettingsDao

    companion object {
        const val DATABASE_NAME = "atveyn_database"

        fun buildDatabase(context: Context): AtveynDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AtveynDatabase::class.java,
                DATABASE_NAME
            ).fallbackToDestructiveMigration().addCallback(object : Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    // Insert initial default settings
                    db.execSQL(
                        "INSERT OR IGNORE INTO app_settings (id, defaultThreshold, notificationsEnabled, reminderDelayMinutes, themeMode, hasCompletedOnboarding) " +
                                "VALUES (1, 75.0, 1, 15, 'SYSTEM', 0)"
                    )
                }
            }).build()
        }
    }
}

