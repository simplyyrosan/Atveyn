package com.atveyn.app.core.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.atveyn.app.core.database.entity.ClassSessionEntity
import com.atveyn.app.core.util.DateTimeUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleClassEndAlarm(session: ClassSessionEntity, subjectName: String, subjectCode: String) {
        try {
            val sessionDate = DateTimeUtils.parseDate(session.date)
            val sessionEndTime = DateTimeUtils.parseTime(session.endTime)
            val triggerDateTime = LocalDateTime.of(sessionDate, sessionEndTime)
            val triggerMillis = triggerDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            // If the class end time is in the past, skip scheduling
            if (triggerMillis <= System.currentTimeMillis()) {
                return
            }

            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra(AlarmReceiver.EXTRA_SESSION_ID, session.id)
                putExtra(AlarmReceiver.EXTRA_SUBJECT_NAME, subjectName)
                putExtra(AlarmReceiver.EXTRA_SUBJECT_CODE, subjectCode)
                putExtra(AlarmReceiver.EXTRA_START_TIME, session.startTime)
                putExtra(AlarmReceiver.EXTRA_END_TIME, session.endTime)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                session.id.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            scheduleAlarmInternal(triggerMillis, pendingIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun scheduleReminder(sessionId: Long, subjectName: String, delayMinutes: Int = 15) {
        val triggerMillis = System.currentTimeMillis() + (delayMinutes * 60 * 1000L)

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_SESSION_ID, sessionId)
            putExtra(AlarmReceiver.EXTRA_SUBJECT_NAME, subjectName)
            putExtra(AlarmReceiver.EXTRA_IS_REMINDER, true)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            sessionId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleAlarmInternal(triggerMillis, pendingIntent)
    }

    fun cancelAlarm(sessionId: Long) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            sessionId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun scheduleAlarmInternal(triggerMillis: Long, pendingIntent: PendingIntent) {
        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        if (canExact) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            }
        } else {
            // Inexact fallback under battery restrictions / missing exact alarm permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            }
        }
    }

    fun canScheduleExact(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }
}
