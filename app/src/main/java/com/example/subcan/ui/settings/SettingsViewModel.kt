package com.example.subcan.ui.settings

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.subcan.SubCanApplication
import com.example.subcan.data.backup.BackupException
import com.example.subcan.data.backup.BackupImportStrategy
import com.example.subcan.data.drive.DriveBackupException
import com.example.subcan.notification.NotificationScheduler
import com.example.subcan.ui.mvi.BaseMviViewModel
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CancellationException

enum class SettingsBackupOperation {
    FILE_EXPORT,
    FILE_IMPORT,
    DRIVE_UPLOAD,
    DRIVE_RESTORE
}

data class SettingsUiState(
    val notificationsEnabled: Boolean = true,
    val activeBackupOperation: SettingsBackupOperation? = null,
    val isImportConfirmDialogVisible: Boolean = false,
    val lastDriveBackupAt: Instant? = null
) {
    val isBackupLoading: Boolean
        get() = activeBackupOperation != null
}

sealed interface SettingsAction {
    data class NotificationsToggled(val enabled: Boolean) : SettingsAction
    data class NotificationPermissionResult(val granted: Boolean) : SettingsAction
    data object HistoryClick : SettingsAction
    data object ExportClick : SettingsAction
    data object ExportFileWritten : SettingsAction
    data object ExportFileWriteFailed : SettingsAction
    data object ImportClick : SettingsAction
    data class ImportJsonRead(val json: String) : SettingsAction
    data object ImportFileReadFailed : SettingsAction
    data object ImportConfirm : SettingsAction
    data object ImportCancel : SettingsAction
    data object DriveBackupClick : SettingsAction
    data object DriveRestoreClick : SettingsAction
    data class DriveAuthorizationGranted(val accessToken: String) : SettingsAction
    data object DriveAuthorizationCanceled : SettingsAction
    data object DriveAuthorizationFailed : SettingsAction
}

sealed interface SettingsEffect {
    data object NavigateHistory : SettingsEffect
    data object RequestNotificationPermission : SettingsEffect
    data class LaunchCreateBackupDocument(val fileName: String, val json: String) : SettingsEffect
    data object LaunchOpenBackupDocument : SettingsEffect
    data object RequestDriveAuthorization : SettingsEffect
    data class ShowSnackbar(val message: String) : SettingsEffect
}

private enum class DriveOperation {
    UPLOAD,
    RESTORE
}

class SettingsViewModel(application: Application) :
    BaseMviViewModel<SettingsUiState, SettingsAction, SettingsEffect>(
        application = application,
        initialState = SettingsUiState(
            notificationsEnabled = hasNotificationPermission(application)
        )
    ) {
    private val backupRepository = (application as SubCanApplication).backupRepository
    private val driveBackupDataSource = (application as SubCanApplication).driveBackupDataSource
    private var pendingImportJson: String? = null
    private var pendingDriveOperation: DriveOperation? = null

    override suspend fun handleAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.NotificationsToggled -> handleNotificationToggle(action.enabled)

            is SettingsAction.NotificationPermissionResult -> setNotificationsEnabled(action.granted)

            SettingsAction.HistoryClick -> emitEffect(SettingsEffect.NavigateHistory)

            SettingsAction.ExportClick -> exportBackup()

            SettingsAction.ExportFileWritten -> {
                emitEffect(SettingsEffect.ShowSnackbar("バックアップファイルを保存しました"))
            }

            SettingsAction.ExportFileWriteFailed -> {
                emitEffect(SettingsEffect.ShowSnackbar("ファイルの読み書きに失敗しました"))
            }

            SettingsAction.ImportClick -> emitEffect(SettingsEffect.LaunchOpenBackupDocument)

            is SettingsAction.ImportJsonRead -> {
                pendingImportJson = action.json
                updateState { copy(isImportConfirmDialogVisible = true) }
            }

            SettingsAction.ImportFileReadFailed -> {
                emitEffect(SettingsEffect.ShowSnackbar("ファイルの読み書きに失敗しました"))
            }

            SettingsAction.ImportConfirm -> importBackup()

            SettingsAction.ImportCancel -> {
                pendingImportJson = null
                updateState { copy(isImportConfirmDialogVisible = false) }
            }

            SettingsAction.DriveBackupClick -> requestDriveAuthorization(DriveOperation.UPLOAD)

            SettingsAction.DriveRestoreClick -> requestDriveAuthorization(DriveOperation.RESTORE)

            is SettingsAction.DriveAuthorizationGranted -> {
                handleDriveAuthorization(action.accessToken)
            }

            SettingsAction.DriveAuthorizationCanceled -> {
                pendingDriveOperation = null
                updateState { copy(activeBackupOperation = null) }
                emitEffect(SettingsEffect.ShowSnackbar("Google Driveの認証をキャンセルしました"))
            }

            SettingsAction.DriveAuthorizationFailed -> {
                pendingDriveOperation = null
                updateState { copy(activeBackupOperation = null) }
                emitEffect(SettingsEffect.ShowSnackbar("Google Driveの認証に失敗しました"))
            }
        }
    }

    private suspend fun handleNotificationToggle(enabled: Boolean) {
        if (enabled &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !hasNotificationPermission(getApplication())
        ) {
            emitEffect(SettingsEffect.RequestNotificationPermission)
            return
        }
        setNotificationsEnabled(enabled)
    }

    private fun setNotificationsEnabled(enabled: Boolean) {
        updateState { copy(notificationsEnabled = enabled) }
        if (enabled) {
            NotificationScheduler.scheduleDailyCheck(getApplication())
        } else {
            NotificationScheduler.cancelDailyCheck(getApplication())
        }
    }

    private suspend fun exportBackup() {
        if (uiState.value.isBackupLoading) return
        updateState { copy(activeBackupOperation = SettingsBackupOperation.FILE_EXPORT) }

        backupRepository.exportSubscriptions()
            .onSuccess { json ->
                updateState { copy(activeBackupOperation = null) }
                emitEffect(
                    SettingsEffect.LaunchCreateBackupDocument(
                        fileName = backupFileName(),
                        json = json
                    )
                )
            }
            .onFailure {
                updateState { copy(activeBackupOperation = null) }
                emitEffect(SettingsEffect.ShowSnackbar("バックアップファイルの作成に失敗しました"))
            }
    }

    private suspend fun importBackup() {
        if (uiState.value.isBackupLoading) return
        val json = pendingImportJson ?: return
        pendingImportJson = null
        updateState {
            copy(
                activeBackupOperation = SettingsBackupOperation.FILE_IMPORT,
                isImportConfirmDialogVisible = false
            )
        }

        backupRepository.importSubscriptions(json, BackupImportStrategy.REPLACE_ALL)
            .onSuccess {
                updateState { copy(activeBackupOperation = null) }
                emitEffect(SettingsEffect.ShowSnackbar("バックアップから復元しました"))
            }
            .onFailure { error ->
                updateState { copy(activeBackupOperation = null) }
                emitEffect(SettingsEffect.ShowSnackbar(error.toMessage()))
            }
    }

    private suspend fun requestDriveAuthorization(operation: DriveOperation) {
        if (uiState.value.isBackupLoading) return
        pendingDriveOperation = operation
        updateState {
            copy(
                activeBackupOperation = when (operation) {
                    DriveOperation.UPLOAD -> SettingsBackupOperation.DRIVE_UPLOAD
                    DriveOperation.RESTORE -> SettingsBackupOperation.DRIVE_RESTORE
                }
            )
        }
        emitEffect(SettingsEffect.RequestDriveAuthorization)
    }

    private suspend fun handleDriveAuthorization(accessToken: String) {
        when (pendingDriveOperation.also { pendingDriveOperation = null }) {
            DriveOperation.UPLOAD -> uploadDriveBackup(accessToken)
            DriveOperation.RESTORE -> downloadDriveBackup(accessToken)
            null -> updateState { copy(activeBackupOperation = null) }
        }
    }

    private suspend fun uploadDriveBackup(accessToken: String) {
        val backupJson = backupRepository.exportSubscriptions().getOrElse {
            updateState { copy(activeBackupOperation = null) }
            emitEffect(SettingsEffect.ShowSnackbar("バックアップファイルの作成に失敗しました"))
            return
        }

        try {
            val metadata = driveBackupDataSource.uploadBackup(accessToken, backupJson)
            updateState {
                copy(
                    activeBackupOperation = null,
                    lastDriveBackupAt = metadata.modifiedAt ?: Instant.now()
                )
            }
            emitEffect(SettingsEffect.ShowSnackbar("Google Driveにバックアップしました"))
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            handleDriveFailure(exception)
        }
    }

    private suspend fun downloadDriveBackup(accessToken: String) {
        try {
            val backup = driveBackupDataSource.downloadLatestBackup(accessToken)
            pendingImportJson = backup.json
            updateState {
                copy(
                    activeBackupOperation = null,
                    isImportConfirmDialogVisible = true,
                    lastDriveBackupAt = backup.metadata.modifiedAt
                )
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            handleDriveFailure(exception)
        }
    }

    private suspend fun handleDriveFailure(error: Throwable) {
        updateState { copy(activeBackupOperation = null) }
        emitEffect(SettingsEffect.ShowSnackbar(error.toDriveMessage()))
    }

    private fun backupFileName(): String {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
        return "subscriptions-backup-$timestamp.json"
    }
}

private fun hasNotificationPermission(application: Application): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(
            application,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

private fun Throwable.toMessage(): String = when (this) {
    is BackupException.InvalidJson,
    is BackupException.EmptyBackupFile,
    is BackupException.InvalidBackupContent -> "バックアップファイルの形式が正しくありません"

    is BackupException.UnsupportedSchemaVersion -> "このバックアップ形式には対応していません"

    else -> "バックアップの復元に失敗しました"
}

private fun Throwable.toDriveMessage(): String = when (this) {
    is DriveBackupException.AuthorizationRequired -> "Google Driveの再認証が必要です"
    is DriveBackupException.BackupNotFound -> "Google Driveにバックアップがありません"
    is DriveBackupException.PayloadTooLarge -> "バックアップファイルのサイズが大きすぎます"
    is DriveBackupException.Network -> "Google Driveへの接続に失敗しました"
    is DriveBackupException.Remote -> "Google Driveの操作に失敗しました"
    else -> "Google Driveの操作に失敗しました"
}
