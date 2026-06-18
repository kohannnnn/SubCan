package com.example.subcan

import android.app.Application
import com.example.subcan.BuildConfig
import com.example.subcan.data.backup.BackupRepository
import com.example.subcan.data.backup.DefaultBackupRepository
import com.example.subcan.data.backup.RoomSubscriptionBackupDataSource
import com.example.subcan.data.db.AppDatabase
import com.example.subcan.data.repository.SubscriptionRepository
import com.example.subcan.notification.NotificationHelper

class SubCanApplication : Application() {

    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    val repository: SubscriptionRepository by lazy {
        SubscriptionRepository(database)
    }
    val backupRepository: BackupRepository by lazy {
        DefaultBackupRepository(
            dataSource = RoomSubscriptionBackupDataSource(database),
            appVersion = BuildConfig.VERSION_NAME
        )
    }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(this)
    }
}
