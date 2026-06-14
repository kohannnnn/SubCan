package com.example.subcan

import android.app.Application
import com.example.subcan.data.db.AppDatabase
import com.example.subcan.data.repository.SubscriptionRepository
import com.example.subcan.notification.NotificationHelper

class SubCanApplication : Application() {

    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    val repository: SubscriptionRepository by lazy {
        SubscriptionRepository(database)
    }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(this)
    }
}
