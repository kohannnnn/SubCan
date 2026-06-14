package com.example.subcan.data.repository

import androidx.room.withTransaction
import com.example.subcan.data.db.AppDatabase
import com.example.subcan.data.db.Subscription
import com.example.subcan.data.db.SubscriptionArchive
import com.example.subcan.data.model.SubscriptionLifecycleManager
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

class SubscriptionRepository(private val database: AppDatabase) {
    private val subscriptionDao = database.subscriptionDao()
    private val archiveDao = database.subscriptionArchiveDao()

    fun getAllSubscriptions(): Flow<List<Subscription>> = subscriptionDao.getAllSubscriptions()

    fun getActiveSubscriptions(): Flow<List<Subscription>> = subscriptionDao.getActiveSubscriptions()

    fun getArchivedSubscriptions(): Flow<List<SubscriptionArchive>> = archiveDao.getAllArchives()

    suspend fun getSubscriptionById(id: Long): Subscription? = subscriptionDao.getSubscriptionById(id)

    fun getSubscriptionByIdFlow(id: Long): Flow<Subscription?> = subscriptionDao.getSubscriptionByIdFlow(id)

    suspend fun insert(subscription: Subscription): Long = subscriptionDao.insert(subscription)

    suspend fun update(subscription: Subscription) = subscriptionDao.update(subscription)

    suspend fun delete(subscription: Subscription) = subscriptionDao.delete(subscription)

    suspend fun deleteById(id: Long) = subscriptionDao.deleteById(id)

    suspend fun archiveAndDelete(subscription: Subscription, archivedAt: LocalDate = LocalDate.now()) {
        database.withTransaction {
            archiveDao.insert(
                SubscriptionArchive.fromSubscription(
                    subscription = subscription,
                    archivedAt = archivedAt
                )
            )
            subscriptionDao.delete(subscription)
        }
    }

    suspend fun deleteArchivedById(id: Long) = archiveDao.deleteById(id)

    fun getActiveCount(): Flow<Int> = subscriptionDao.getActiveCount()

    suspend fun getSubscriptionsDueBefore(date: LocalDate): List<Subscription> =
        subscriptionDao.getSubscriptionsDueBefore(date.toEpochDay())

    suspend fun syncSubscriptions(today: LocalDate = LocalDate.now()) {
        subscriptionDao.getSubscriptionsDueBefore(today.toEpochDay())
            .forEach { subscription ->
                SubscriptionLifecycleManager.resolveDueSubscription(subscription, today)
                    ?.let { updatedSubscription ->
                        subscriptionDao.update(updatedSubscription)
                    }
            }
    }
}
