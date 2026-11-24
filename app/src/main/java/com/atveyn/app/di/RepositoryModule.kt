package com.atveyn.app.di

import com.atveyn.app.data.repository.AttendanceRepositoryImpl
import com.atveyn.app.data.repository.HolidayRepositoryImpl
import com.atveyn.app.data.repository.SettingsRepositoryImpl
import com.atveyn.app.data.repository.SubjectRepositoryImpl
import com.atveyn.app.data.repository.TimetableRepositoryImpl
import com.atveyn.app.domain.repository.AttendanceRepository
import com.atveyn.app.domain.repository.HolidayRepository
import com.atveyn.app.domain.repository.SettingsRepository
import com.atveyn.app.domain.repository.SubjectRepository
import com.atveyn.app.domain.repository.TimetableRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSubjectRepository(impl: SubjectRepositoryImpl): SubjectRepository

    @Binds
    @Singleton
    abstract fun bindTimetableRepository(impl: TimetableRepositoryImpl): TimetableRepository

    @Binds
    @Singleton
    abstract fun bindAttendanceRepository(impl: AttendanceRepositoryImpl): AttendanceRepository

    @Binds
    @Singleton
    abstract fun bindHolidayRepository(impl: HolidayRepositoryImpl): HolidayRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
