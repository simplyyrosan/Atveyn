package com.atveyn.app.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.atveyn.app.core.database.AtveynDatabase
import com.atveyn.app.core.model.SessionStatus
import com.atveyn.app.core.scheduler.AlarmScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AttendanceActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var database: AtveynDatabase

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val sessionId = intent.getLongExtra(NotificationHelper.EXTRA_SESSION_ID, -1L)
        val notificationId = intent.getIntExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, sessionId.toInt())
        val subjectName = intent.getStringExtra(NotificationHelper.EXTRA_SUBJECT_NAME) ?: "Class"
        val action = intent.action ?: return

        if (sessionId == -1L) return

        // Dismiss notification immediately for responsive UX
        notificationHelper.cancelNotification(notificationId)

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    NotificationHelper.ACTION_MARK_PRESENT -> {
                        database.classSessionDao().updateStatus(sessionId, SessionStatus.PRESENT)
                    }
                    NotificationHelper.ACTION_MARK_ABSENT -> {
                        database.classSessionDao().updateStatus(sessionId, SessionStatus.ABSENT)
                    }
                    NotificationHelper.ACTION_REMIND_LATER -> {
                        val settings = database.appSettingsDao().getSettings()
                        val delayMinutes = settings?.reminderDelayMinutes ?: 15
                        alarmScheduler.scheduleReminder(sessionId, subjectName, delayMinutes)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
