package com.example.subcan.data.drive

import java.time.Instant
import kotlinx.serialization.Serializable

const val DRIVE_APP_DATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
const val DRIVE_BACKUP_FILE_NAME = "subcan-backup.json"

data class DriveBackupMetadata(val fileId: String, val modifiedAt: Instant?)

data class DownloadedDriveBackup(val json: String, val metadata: DriveBackupMetadata)

sealed class DriveBackupException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class AuthorizationRequired : DriveBackupException("Google Drive authorization is required")

    class BackupNotFound : DriveBackupException("Google Drive backup was not found")

    class PayloadTooLarge : DriveBackupException("Google Drive backup exceeds the size limit")

    class Network(cause: Throwable) : DriveBackupException("Google Drive network request failed", cause)

    class Remote(val statusCode: Int) :
        DriveBackupException("Google Drive request failed with status $statusCode")
}

interface DriveBackupDataSource {
    suspend fun uploadBackup(accessToken: String, json: String): DriveBackupMetadata

    suspend fun downloadLatestBackup(accessToken: String): DownloadedDriveBackup
}

interface DriveApiClient {
    suspend fun findLatestFile(accessToken: String, fileName: String): DriveRemoteFile?

    suspend fun createFile(accessToken: String, fileName: String, content: String): DriveRemoteFile

    suspend fun updateFile(accessToken: String, fileId: String, content: String): DriveRemoteFile

    suspend fun downloadFile(accessToken: String, fileId: String): String
}

@Serializable
data class DriveRemoteFile(val id: String, val modifiedTime: String? = null)

class DefaultDriveBackupDataSource(private val apiClient: DriveApiClient) : DriveBackupDataSource {
    override suspend fun uploadBackup(accessToken: String, json: String): DriveBackupMetadata {
        requireAccessToken(accessToken)
        val remoteFile = apiClient.findLatestFile(accessToken, DRIVE_BACKUP_FILE_NAME)
        val uploadedFile = if (remoteFile == null) {
            apiClient.createFile(accessToken, DRIVE_BACKUP_FILE_NAME, json)
        } else {
            apiClient.updateFile(accessToken, remoteFile.id, json)
        }
        return uploadedFile.toMetadata()
    }

    override suspend fun downloadLatestBackup(accessToken: String): DownloadedDriveBackup {
        requireAccessToken(accessToken)
        val remoteFile = apiClient.findLatestFile(accessToken, DRIVE_BACKUP_FILE_NAME)
            ?: throw DriveBackupException.BackupNotFound()
        return DownloadedDriveBackup(
            json = apiClient.downloadFile(accessToken, remoteFile.id),
            metadata = remoteFile.toMetadata()
        )
    }

    private fun requireAccessToken(accessToken: String) {
        if (accessToken.isBlank()) throw DriveBackupException.AuthorizationRequired()
    }
}

private fun DriveRemoteFile.toMetadata(): DriveBackupMetadata = DriveBackupMetadata(
    fileId = id,
    modifiedAt = modifiedTime?.let { runCatching { Instant.parse(it) }.getOrNull() }
)
