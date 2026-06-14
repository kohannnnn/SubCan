package com.example.subcan.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.subcan.data.db.AppDatabase
import com.example.subcan.data.repository.SubscriptionRepository
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val dao = database.subscriptionDao()
        val repository = SubscriptionRepository(database)

        val today = LocalDate.now()
        repository.syncSubscriptions(today)

        // 7日先までの更新をチェック
        val checkUntil = today.plusDays(7)
        val upcomingSubscriptions = dao.getSubscriptionsDueBefore(checkUntil.toEpochDay())

        for (subscription in upcomingSubscriptions) {
            if (!subscription.isActive || !subscription.autoRenew) continue

            val daysUntil = ChronoUnit.DAYS.between(today, subscription.nextBillingDate).toInt()
            if (daysUntil < 0) continue

            // リマインダー日数以内なら通知
            if (daysUntil <= subscription.reminderDaysBefore) {
                NotificationHelper.showReminderNotification(
                    context = applicationContext,
                    subscriptionId = subscription.id,
                    subscriptionName = subscription.name,
                    daysUntilRenewal = daysUntil,
                    price = subscription.price
                )
            }
        }

        return Result.success()
    }

    companion object {
        const val WORK_NAME = "subcan_reminder_check"
    }
}
