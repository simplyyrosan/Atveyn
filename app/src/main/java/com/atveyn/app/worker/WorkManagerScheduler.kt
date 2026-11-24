package com.atveyn.app.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkManagerScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager = WorkManager.getInstance(context)

    companion object {
        const val PERIODIC_WORK_NAME = "atveyn_daily_reconcile_work"
        const val ONE_TIME_WORK_NAME = "atveyn_immediate_reconcile_work"
    }

    fun scheduleDailyReconciliation() {
        val periodicRequest = PeriodicWorkRequestBuilder<DailyAttendanceWorker>(
            24, TimeUnit.HOURS,
            1, TimeUnit.HOURS
        ).build()

        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicRequest
        )
    }

    fun triggerImmediateReconciliation() {
        val oneTimeRequest = OneTimeWorkRequestBuilder<DailyAttendanceWorker>()
            .build()

        workManager.enqueueUniqueWork(
            ONE_TIME_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            oneTimeRequest
        )
    }
}
