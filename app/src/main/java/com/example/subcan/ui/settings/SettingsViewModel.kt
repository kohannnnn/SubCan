package com.example.subcan.ui.settings

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.subcan.SubCanApplication
import com.example.subcan.data.backup.BackupException
import com.example.subcan.data.backup.BackupImportStrategy
import com.example.subcan.notification.NotificationScheduler
import com.example.subcan.ui.mvi.BaseMviViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class SettingsUiState(
    val notificationsEnabled: Boolean = true,
    val isBackupLoading: Boolean = false,
    val isImportConfirmDialogVisible: Boolean = false
)

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
}

sealed interface SettingsEffect {
    data object NavigateHistory : SettingsEffect
    data object RequestNotificationPermission : SettingsEffect
    data class LaunchCreateBackupDocument(val fileName: String, val json: String) : SettingsEffect
    data object LaunchOpenBackupDocument : SettingsEffect
    data class ShowSnackbar(val message: String) : SettingsEffect
}

class SettingsViewModel(application: Application) :
    BaseMviViewModel<SettingsUiState, SettingsAction, SettingsEffect>(
        application = application,
        initialState = SettingsUiState(
            notificationsEnabled = hasNotificationPermission(application)
        )
    ) {
    private val backupRepository = (application as SubCanApplication).backupRepository
    private var pendingImportJson: String? = null

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
        updateState { copy(isBackupLoading = true) }

        backupRepository.exportSubscriptions()
            .onSuccess { json ->
                updateState { copy(isBackupLoading = false) }
                emitEffect(
                    SettingsEffect.LaunchCreateBackupDocument(
                        fileName = backupFileName(),
                        json = json
                    )
                )
            }
            .onFailure {
                updateState { copy(isBackupLoading = false) }
                emitEffect(SettingsEffect.ShowSnackbar("バックアップファイルの作成に失敗しました"))
            }
    }

    private suspend fun importBackup() {
        if (uiState.value.isBackupLoading) return
        val json = pendingImportJson ?: return
        pendingImportJson = null
        updateState {
            copy(
                isBackupLoading = true,
                isImportConfirmDialogVisible = false
            )
        }

        backupRepository.importSubscriptions(json, BackupImportStrategy.REPLACE_ALL)
            .onSuccess {
                updateState { copy(isBackupLoading = false) }
                emitEffect(SettingsEffect.ShowSnackbar("バックアップから復元しました"))
            }
            .onFailure { error ->
                updateState { copy(isBackupLoading = false) }
                emitEffect(SettingsEffect.ShowSnackbar(error.toMessage()))
            }
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
