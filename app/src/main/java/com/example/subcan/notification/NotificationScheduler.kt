package com.example.subcan.notification

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object NotificationScheduler {

    /**
     * 毎日1回、サブスクリプションの更新チェックを行うWorkerをスケジュールする
     */
    fun scheduleDailyCheck(context: Context) {
        val workRequest = PeriodicWorkRequestBuilder<ReminderWorker>(
            1,
            TimeUnit.DAYS
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            ReminderWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    /**
     * スケジュールされたチェックをキャンセルする
     */
    fun cancelDailyCheck(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(ReminderWorker.WORK_NAME)
    }
}
