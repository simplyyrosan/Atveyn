package com.atveyn.app.core.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.atveyn.app.core.database.AtveynDatabase
import com.atveyn.app.core.model.SessionStatus
import com.atveyn.app.core.notification.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var database: AtveynDatabase

    companion object {
        const val EXTRA_SESSION_ID = "extra_session_id"
        const val EXTRA_SUBJECT_NAME = "extra_subject_name"
        const val EXTRA_SUBJECT_CODE = "extra_subject_code"
        const val EXTRA_START_TIME = "extra_start_time"
        const val EXTRA_END_TIME = "extra_end_time"
        const val EXTRA_IS_REMINDER = "extra_is_reminder"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val sessionId = intent.getLongExtra(EXTRA_SESSION_ID, -1L)
        if (sessionId == -1L) return

        val subjectName = intent.getStringExtra(EXTRA_SUBJECT_NAME) ?: "Class"
        val subjectCode = intent.getStringExtra(EXTRA_SUBJECT_CODE) ?: ""
        val startTime = intent.getStringExtra(EXTRA_START_TIME) ?: ""
        val endTime = intent.getStringExtra(EXTRA_END_TIME) ?: ""

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val session = database.classSessionDao().getSessionById(sessionId)
                val settings = database.appSettingsDao().getSettings()

                // Only show notification if session is still PENDING and notifications are enabled
                if (session != null && session.status == SessionStatus.PENDING && (settings?.notificationsEnabled != false)) {
                    val subject = database.subjectDao().getSubjectById(session.subjectId)
                    val sName = subject?.name ?: subjectName
                    val sCode = subject?.code ?: subjectCode
                    val sStart = session.startTime.ifEmpty { startTime }
                    val sEnd = session.endTime.ifEmpty { endTime }

                    notificationHelper.showAttendanceNotification(
                        sessionId = sessionId,
                        subjectName = sName,
                        subjectCode = sCode,
                        startTime = sStart,
                        endTime = sEnd
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
