package com.example.subcan.data.drive

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val DRIVE_API_BASE_URL = "https://www.googleapis.com/drive/v3"
private const val DRIVE_UPLOAD_BASE_URL = "https://www.googleapis.com/upload/drive/v3"
private const val JSON_MIME_TYPE = "application/json"
private const val MAX_DRIVE_BACKUP_BYTES = 5 * 1024 * 1024
private const val CONNECT_TIMEOUT_MILLIS = 15_000
private const val READ_TIMEOUT_MILLIS = 30_000

class HttpDriveApiClient(
    private val jsonFormat: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
) : DriveApiClient {

    override suspend fun findLatestFile(accessToken: String, fileName: String): DriveRemoteFile? =
        withContext(Dispatchers.IO) {
            val query = encodeQuery("name = '${fileName.replace("'", "\\'")}' and trashed = false")
            val fields = encodeQuery("files(id,modifiedTime)")
            val response = execute(
                method = "GET",
                url = "$DRIVE_API_BASE_URL/files?spaces=appDataFolder&q=$query" +
                    "&orderBy=modifiedTime%20desc&pageSize=1&fields=$fields",
                accessToken = accessToken
            )
            jsonFormat.decodeFromString<DriveFileListResponse>(response).files.firstOrNull()
        }

    override suspend fun createFile(accessToken: String, fileName: String, content: String): DriveRemoteFile =
        withContext(Dispatchers.IO) {
            val contentBytes = content.toByteArray(StandardCharsets.UTF_8)
            requireSizeWithinLimit(contentBytes.size)
            val boundary = "subcan-${UUID.randomUUID()}"
            val metadata = jsonFormat.encodeToString(
                DriveCreateFileRequest(
                    name = fileName,
                    parents = listOf("appDataFolder")
                )
            )
            val body = multipartBody(boundary, metadata, contentBytes)
            val response = execute(
                method = "POST",
                url = "$DRIVE_UPLOAD_BASE_URL/files?uploadType=multipart&fields=id,modifiedTime",
                accessToken = accessToken,
                contentType = "multipart/related; boundary=$boundary",
                body = body
            )
            jsonFormat.decodeFromString(response)
        }

    override suspend fun updateFile(accessToken: String, fileId: String, content: String): DriveRemoteFile =
        withContext(Dispatchers.IO) {
            val contentBytes = content.toByteArray(StandardCharsets.UTF_8)
            requireSizeWithinLimit(contentBytes.size)
            val response = execute(
                method = "POST",
                url = "$DRIVE_UPLOAD_BASE_URL/files/${encodePath(fileId)}?uploadType=media&fields=id,modifiedTime",
                accessToken = accessToken,
                contentType = JSON_MIME_TYPE,
                body = contentBytes,
                methodOverride = "PATCH"
            )
            jsonFormat.decodeFromString(response)
        }

    override suspend fun downloadFile(accessToken: String, fileId: String): String = withContext(Dispatchers.IO) {
        execute(
            method = "GET",
            url = "$DRIVE_API_BASE_URL/files/${encodePath(fileId)}?alt=media",
            accessToken = accessToken
        )
    }

    private fun execute(
        method: String,
        url: String,
        accessToken: String,
        contentType: String? = null,
        body: ByteArray? = null,
        methodOverride: String? = null
    ): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = CONNECT_TIMEOUT_MILLIS
            readTimeout = READ_TIMEOUT_MILLIS
            setRequestProperty("Authorization", "Bearer $accessToken")
            setRequestProperty("Accept", JSON_MIME_TYPE)
            if (contentType != null) setRequestProperty("Content-Type", contentType)
            if (methodOverride != null) setRequestProperty("X-HTTP-Method-Override", methodOverride)
            if (body != null) {
                doOutput = true
                setFixedLengthStreamingMode(body.size)
            }
        }

        try {
            if (body != null) {
                connection.outputStream.use { it.write(body) }
            }
            val statusCode = connection.responseCode
            if (statusCode !in 200..299) {
                connection.errorStream?.close()
                throw when (statusCode) {
                    HttpURLConnection.HTTP_UNAUTHORIZED -> DriveBackupException.AuthorizationRequired()
                    else -> DriveBackupException.Remote(statusCode)
                }
            }

            val contentLength = connection.contentLengthLong
            if (contentLength > MAX_DRIVE_BACKUP_BYTES) {
                throw DriveBackupException.PayloadTooLarge()
            }
            return connection.inputStream.use(::readUtf8WithLimit)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: DriveBackupException) {
            throw exception
        } catch (exception: IOException) {
            throw DriveBackupException.Network(exception)
        } finally {
            connection.disconnect()
        }
    }

    private fun readUtf8WithLimit(input: java.io.InputStream): String {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var totalBytes = 0
        while (true) {
            val bytesRead = input.read(buffer)
            if (bytesRead < 0) break
            totalBytes += bytesRead
            requireSizeWithinLimit(totalBytes)
            output.write(buffer, 0, bytesRead)
        }
        return output.toString(StandardCharsets.UTF_8.name())
    }

    private fun multipartBody(boundary: String, metadata: String, content: ByteArray): ByteArray {
        val output = ByteArrayOutputStream()
        output.write("--$boundary\r\n".toByteArray(StandardCharsets.UTF_8))
        output.write(
            "Content-Type: $JSON_MIME_TYPE; charset=UTF-8\r\n\r\n"
                .toByteArray(StandardCharsets.UTF_8)
        )
        output.write(metadata.toByteArray(StandardCharsets.UTF_8))
        output.write("\r\n--$boundary\r\n".toByteArray(StandardCharsets.UTF_8))
        output.write(
            "Content-Type: $JSON_MIME_TYPE\r\n\r\n".toByteArray(StandardCharsets.UTF_8)
        )
        output.write(content)
        output.write("\r\n--$boundary--\r\n".toByteArray(StandardCharsets.UTF_8))
        return output.toByteArray()
    }

    private fun requireSizeWithinLimit(size: Int) {
        if (size > MAX_DRIVE_BACKUP_BYTES) throw DriveBackupException.PayloadTooLarge()
    }

    private fun encodeQuery(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.name())

    private fun encodePath(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20")
}

@Serializable
private data class DriveFileListResponse(val files: List<DriveRemoteFile> = emptyList())

@Serializable
private data class DriveCreateFileRequest(
    val name: String,
    val parents: List<String>,
    @SerialName("mimeType") val mimeType: String = JSON_MIME_TYPE
)
