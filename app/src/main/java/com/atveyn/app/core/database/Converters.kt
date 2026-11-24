package com.atveyn.app.core.database

import androidx.room.TypeConverter
import com.atveyn.app.core.model.SessionSource
import com.atveyn.app.core.model.SessionStatus

class Converters {
    @TypeConverter
    fun fromSessionStatus(status: SessionStatus): String = status.name

    @TypeConverter
    fun toSessionStatus(value: String): SessionStatus = try {
        SessionStatus.valueOf(value)
    } catch (e: Exception) {
        SessionStatus.PENDING
    }

    @TypeConverter
    fun fromSessionSource(source: SessionSource): String = source.name

    @TypeConverter
    fun toSessionSource(value: String): SessionSource = try {
        SessionSource.valueOf(value)
    } catch (e: Exception) {
        SessionSource.SCHEDULED
    }
}
