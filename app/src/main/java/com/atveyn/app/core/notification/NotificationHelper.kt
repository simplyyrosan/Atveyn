package com.atveyn.app.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.atveyn.app.MainActivity
import com.atveyn.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_ID = "attendance_reminders_channel"
        const val CHANNEL_NAME = "Class Attendance Reminders"

        const val ACTION_MARK_PRESENT = "com.atveyn.app.ACTION_MARK_PRESENT"
        const val ACTION_MARK_ABSENT = "com.atveyn.app.ACTION_MARK_ABSENT"
        const val ACTION_REMIND_LATER = "com.atveyn.app.ACTION_REMIND_LATER"

        const val EXTRA_SESSION_ID = "extra_session_id"
        const val EXTRA_SUBJECT_NAME = "extra_subject_name"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications sent at the end of classes to log attendance"
                enableVibration(true)
                setShowBadge(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun showAttendanceNotification(
        sessionId: Long,
        subjectName: String,
        subjectCode: String,
        startTime: String,
        endTime: String
    ) {
        val notificationId = sessionId.toInt()

        // Content intent: Opens app to Dashboard
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_SESSION_ID, sessionId)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Present
        val presentIntent = Intent(context, AttendanceActionReceiver::class.java).apply {
            action = ACTION_MARK_PRESENT
            putExtra(EXTRA_SESSION_ID, sessionId)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val presentPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 1,
            presentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Absent
        val absentIntent = Intent(context, AttendanceActionReceiver::class.java).apply {
            action = ACTION_MARK_ABSENT
            putExtra(EXTRA_SESSION_ID, sessionId)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val absentPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 2,
            absentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Remind Later
        val remindIntent = Intent(context, AttendanceActionReceiver::class.java).apply {
            action = ACTION_REMIND_LATER
            putExtra(EXTRA_SESSION_ID, sessionId)
            putExtra(EXTRA_SUBJECT_NAME, subjectName)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val remindPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 3,
            remindIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Class Finished: $subjectName")
            .setContentText("Did you attend $subjectCode ($startTime - $endTime)?")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(0, "Present", presentPendingIntent)
            .addAction(0, "Absent", absentPendingIntent)
            .addAction(0, "Remind Later", remindPendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            // Android 13 POST_NOTIFICATIONS permission not granted
        }
    }

    fun cancelNotification(notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }
}
