package com.example.subcan.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.subcan.MainActivity
import com.example.subcan.R

object NotificationHelper {

    const val CHANNEL_ID = "subcan_reminders"
    private const val CHANNEL_NAME = "サブスク更新リマインダー"
    private const val CHANNEL_DESCRIPTION = "サブスクリプションの更新日が近づいた時にお知らせします"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESCRIPTION
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    fun showReminderNotification(
        context: Context,
        subscriptionId: Long,
        subscriptionName: String,
        daysUntilRenewal: Int,
        price: Int
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("subscription_id", subscriptionId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            subscriptionId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "📅 $subscriptionName の更新日が近づいています"
        val message = if (daysUntilRenewal == 0) {
            "本日が更新日です（¥${"%,d".format(price)}）"
        } else {
            "あと${daysUntilRenewal}日で更新されます（¥${"%,d".format(price)}）"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context)
                .notify(subscriptionId.toInt(), notification)
        } catch (_: SecurityException) {
            // 通知権限がない場合は無視
        }
    }
}
