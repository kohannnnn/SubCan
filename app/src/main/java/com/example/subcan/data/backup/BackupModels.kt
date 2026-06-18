package com.example.subcan.data.backup

import kotlinx.serialization.Serializable

const val CURRENT_BACKUP_SCHEMA_VERSION = 1

@Serializable
data class SubscriptionBackupFile(
    val schemaVersion: Int,
    val exportedAt: String,
    val appVersion: String? = null,
    val subscriptions: List<SubscriptionBackupItem>
)

@Serializable
data class SubscriptionBackupItem(
    val id: Long,
    val name: String,
    val description: String = "",
    val price: Int,
    val currency: String = "JPY",
    val billingCycle: String,
    val category: String,
    val startDate: String,
    val nextBillingDate: String,
    val isActive: Boolean = true,
    val autoRenew: Boolean = true,
    val canceledAt: String? = null,
    val billingStartPolicy: String = "SIGNUP_DATE",
    val cancellationPolicy: String = "END_OF_PERIOD",
    val hasFreeTrial: Boolean = false,
    val freeTrialDays: Int = 0,
    val freeTrialEndDate: String? = null,
    val reminderDaysBefore: Int = 3,
    val cancellationUrl: String = "",
    val notes: String = "",
    val createdAt: String
)

enum class BackupImportStrategy {
    REPLACE_ALL,
    MERGE
}

data class BackupImportResult(val importedCount: Int, val strategy: BackupImportStrategy)

sealed class BackupException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class EmptyBackupFile : BackupException("Backup file is empty")

    class InvalidJson(cause: Throwable) : BackupException("Backup file is not valid JSON", cause)

    class UnsupportedSchemaVersion(val version: Int) :
        BackupException("Unsupported backup schema version: $version")

    class InvalidBackupContent(reason: String, cause: Throwable? = null) :
        BackupException("Invalid backup content: $reason", cause)

    class UnsupportedImportStrategy(val strategy: BackupImportStrategy) :
        BackupException("Unsupported import strategy: $strategy")

    class DatabaseOperationFailed(cause: Throwable) : BackupException("Backup database operation failed", cause)
}
