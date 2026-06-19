package com.example.subcan.data.drive

import java.io.IOException
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DriveBackupDataSourceTest {
    @Test
    fun `upload creates backup when app data file does not exist`() = runBlocking {
        val api = FakeDriveApiClient()
        val dataSource = DefaultDriveBackupDataSource(api)

        val result = dataSource.uploadBackup("token", "{\"schemaVersion\":1}")

        assertEquals("created-id", result.fileId)
        assertEquals(DRIVE_BACKUP_FILE_NAME, api.createdFileName)
        assertEquals("{\"schemaVersion\":1}", api.uploadedContent)
        assertNull(api.updatedFileId)
    }

    @Test
    fun `upload updates latest existing backup`() = runBlocking {
        val api = FakeDriveApiClient(
            latestFile = DriveRemoteFile("existing-id", "2026-06-18T10:00:00Z")
        )
        val dataSource = DefaultDriveBackupDataSource(api)

        val result = dataSource.uploadBackup("token", "new-json")

        assertEquals("existing-id", result.fileId)
        assertEquals("existing-id", api.updatedFileId)
        assertEquals("new-json", api.uploadedContent)
        assertNull(api.createdFileName)
    }

    @Test
    fun `download returns latest backup and metadata`() = runBlocking {
        val api = FakeDriveApiClient(
            latestFile = DriveRemoteFile("backup-id", "2026-06-18T10:00:00Z"),
            downloadedContent = "backup-json"
        )
        val dataSource = DefaultDriveBackupDataSource(api)

        val result = dataSource.downloadLatestBackup("token")

        assertEquals("backup-json", result.json)
        assertEquals("backup-id", result.metadata.fileId)
        assertEquals(Instant.parse("2026-06-18T10:00:00Z"), result.metadata.modifiedAt)
    }

    @Test
    fun `download fails when backup does not exist`() {
        val result = runCatching {
            runBlocking {
                DefaultDriveBackupDataSource(FakeDriveApiClient())
                    .downloadLatestBackup("token")
            }
        }

        assertTrue(result.exceptionOrNull() is DriveBackupException.BackupNotFound)
    }

    @Test
    fun `blank access token is rejected before api call`() {
        val api = FakeDriveApiClient()
        val result = runCatching {
            runBlocking {
                DefaultDriveBackupDataSource(api).uploadBackup("", "json")
            }
        }

        assertTrue(result.exceptionOrNull() is DriveBackupException.AuthorizationRequired)
        assertEquals(0, api.findCalls)
    }

    @Test
    fun `network failure is propagated without exposing content`() {
        val api = FakeDriveApiClient(
            failure = DriveBackupException.Network(IOException("offline"))
        )
        val result = runCatching {
            runBlocking {
                DefaultDriveBackupDataSource(api).uploadBackup("token", "private-json")
            }
        }

        assertTrue(result.exceptionOrNull() is DriveBackupException.Network)
    }
}

private class FakeDriveApiClient(
    private val latestFile: DriveRemoteFile? = null,
    private val downloadedContent: String = "",
    private val failure: DriveBackupException? = null
) : DriveApiClient {
    var findCalls: Int = 0
    var createdFileName: String? = null
    var updatedFileId: String? = null
    var uploadedContent: String? = null

    override suspend fun findLatestFile(accessToken: String, fileName: String): DriveRemoteFile? {
        findCalls += 1
        failure?.let { throw it }
        return latestFile
    }

    override suspend fun createFile(accessToken: String, fileName: String, content: String): DriveRemoteFile {
        failure?.let { throw it }
        createdFileName = fileName
        uploadedContent = content
        return DriveRemoteFile("created-id", "2026-06-19T01:00:00Z")
    }

    override suspend fun updateFile(accessToken: String, fileId: String, content: String): DriveRemoteFile {
        failure?.let { throw it }
        updatedFileId = fileId
        uploadedContent = content
        return DriveRemoteFile(fileId, "2026-06-19T01:00:00Z")
    }

    override suspend fun downloadFile(accessToken: String, fileId: String): String {
        failure?.let { throw it }
        return downloadedContent
    }
}
