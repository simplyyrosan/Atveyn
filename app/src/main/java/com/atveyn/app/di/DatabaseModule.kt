package com.atveyn.app.di

import android.content.Context
import com.atveyn.app.core.database.AtveynDatabase
import com.atveyn.app.core.database.dao.AppSettingsDao
import com.atveyn.app.core.database.dao.ClassSessionDao
import com.atveyn.app.core.database.dao.HolidayDao
import com.atveyn.app.core.database.dao.SubjectDao
import com.atveyn.app.core.database.dao.TimetableDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AtveynDatabase {
        return AtveynDatabase.buildDatabase(context)
    }

    @Provides
    fun provideSubjectDao(database: AtveynDatabase): SubjectDao = database.subjectDao()

    @Provides
    fun provideTimetableDao(database: AtveynDatabase): TimetableDao = database.timetableDao()

    @Provides
    fun provideClassSessionDao(database: AtveynDatabase): ClassSessionDao = database.classSessionDao()

    @Provides
    fun provideHolidayDao(database: AtveynDatabase): HolidayDao = database.holidayDao()

    @Provides
    fun provideAppSettingsDao(database: AtveynDatabase): AppSettingsDao = database.appSettingsDao()
}
