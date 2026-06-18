package com.example.subcan.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionDao {

    @Query("SELECT * FROM subscriptions ORDER BY nextBillingDate ASC")
    fun getAllSubscriptions(): Flow<List<Subscription>>

    @Query("SELECT * FROM subscriptions ORDER BY nextBillingDate ASC")
    suspend fun getAllSubscriptionsSnapshot(): List<Subscription>

    @Query("SELECT * FROM subscriptions WHERE isActive = 1 ORDER BY nextBillingDate ASC")
    fun getActiveSubscriptions(): Flow<List<Subscription>>

    @Query("SELECT * FROM subscriptions WHERE id = :id")
    suspend fun getSubscriptionById(id: Long): Subscription?

    @Query("SELECT * FROM subscriptions WHERE id = :id")
    fun getSubscriptionByIdFlow(id: Long): Flow<Subscription?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(subscription: Subscription): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(subscriptions: List<Subscription>)

    @Update
    suspend fun update(subscription: Subscription)

    @Delete
    suspend fun delete(subscription: Subscription)

    @Query("DELETE FROM subscriptions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM subscriptions")
    suspend fun deleteAll()

    @Query("SELECT SUM(price) FROM subscriptions WHERE isActive = 1 AND billingCycle = 'MONTHLY'")
    fun getTotalMonthlyDirect(): Flow<Int?>

    @Query("SELECT COUNT(*) FROM subscriptions WHERE isActive = 1")
    fun getActiveCount(): Flow<Int>

    @Query("SELECT * FROM subscriptions WHERE isActive = 1 AND nextBillingDate <= :date ORDER BY nextBillingDate ASC")
    suspend fun getSubscriptionsDueBefore(date: Long): List<Subscription>
}
