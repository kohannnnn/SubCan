package com.example.subcan.data.backup

import androidx.room.withTransaction
import com.example.subcan.data.db.AppDatabase
import com.example.subcan.data.db.Subscription
import java.time.Clock
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

interface BackupRepository {
    suspend fun exportSubscriptions(): Result<String>

    suspend fun importSubscriptions(json: String, strategy: BackupImportStrategy): Result<BackupImportResult>
}

interface SubscriptionBackupDataSource {
    suspend fun getAllSubscriptions(): List<Subscription>

    suspend fun replaceAll(subscriptions: List<Subscription>)
}

class DefaultBackupRepository(
    private val dataSource: SubscriptionBackupDataSource,
    private val appVersion: String?,
    private val clock: Clock = Clock.systemUTC(),
    private val jsonFormat: Json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
) : BackupRepository {

    override suspend fun exportSubscriptions(): Result<String> = try {
        val subscriptions = dataSource.getAllSubscriptions()
        val backup = SubscriptionBackupFile(
            schemaVersion = CURRENT_BACKUP_SCHEMA_VERSION,
            exportedAt = Instant.now(clock).toString(),
            appVersion = appVersion,
            subscriptions = subscriptions.map(Subscription::toBackupItem)
        )
        Result.success(jsonFormat.encodeToString(backup))
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        Result.failure(BackupException.DatabaseOperationFailed(exception))
    }

    override suspend fun importSubscriptions(json: String, strategy: BackupImportStrategy): Result<BackupImportResult> =
        try {
            if (strategy != BackupImportStrategy.REPLACE_ALL) {
                throw BackupException.UnsupportedImportStrategy(strategy)
            }

            val backup = decodeAndValidate(json)
            val entities = backup.subscriptions.map(SubscriptionBackupItem::toEntity)
            if (entities.map(Subscription::id).distinct().size != entities.size) {
                throw BackupException.InvalidBackupContent("subscription ids must be unique")
            }

            try {
                dataSource.replaceAll(entities)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                throw BackupException.DatabaseOperationFailed(exception)
            }

            Result.success(
                BackupImportResult(
                    importedCount = entities.size,
                    strategy = strategy
                )
            )
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: BackupException) {
            Result.failure(exception)
        }

    private fun decodeAndValidate(json: String): SubscriptionBackupFile {
        if (json.isBlank()) throw BackupException.EmptyBackupFile()

        val root = try {
            jsonFormat.parseToJsonElement(json) as? JsonObject
                ?: throw BackupException.InvalidBackupContent("root must be a JSON object")
        } catch (exception: SerializationException) {
            throw BackupException.InvalidJson(exception)
        }

        val schemaVersion = (root["schemaVersion"] as? JsonPrimitive)?.intOrNull
            ?: throw BackupException.InvalidBackupContent("schemaVersion is required")
        if (schemaVersion != CURRENT_BACKUP_SCHEMA_VERSION) {
            throw BackupException.UnsupportedSchemaVersion(schemaVersion)
        }

        val backup = try {
            jsonFormat.decodeFromString<SubscriptionBackupFile>(json)
        } catch (exception: SerializationException) {
            throw BackupException.InvalidBackupContent("required fields are missing or invalid", exception)
        }

        try {
            Instant.parse(backup.exportedAt)
        } catch (exception: Exception) {
            throw BackupException.InvalidBackupContent("exportedAt is not an ISO-8601 instant", exception)
        }
        return backup
    }
}

class RoomSubscriptionBackupDataSource(private val database: AppDatabase) : SubscriptionBackupDataSource {
    private val subscriptionDao = database.subscriptionDao()

    override suspend fun getAllSubscriptions(): List<Subscription> = subscriptionDao.getAllSubscriptionsSnapshot()

    override suspend fun replaceAll(subscriptions: List<Subscription>) {
        database.withTransaction {
            subscriptionDao.deleteAll()
            if (subscriptions.isNotEmpty()) {
                subscriptionDao.insertAll(subscriptions)
            }
        }
    }
}
