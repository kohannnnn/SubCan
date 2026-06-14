package com.example.subcan.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionArchiveDao {

    @Query("SELECT * FROM subscription_archives ORDER BY archivedAt DESC, canceledAt DESC, id DESC")
    fun getAllArchives(): Flow<List<SubscriptionArchive>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(archive: SubscriptionArchive): Long

    @Query("DELETE FROM subscription_archives WHERE id = :id")
    suspend fun deleteById(id: Long)
}
