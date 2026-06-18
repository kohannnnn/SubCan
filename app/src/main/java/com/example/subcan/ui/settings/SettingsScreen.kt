package com.example.subcan.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.subcan.BuildConfig
import com.example.subcan.ui.theme.SubCanTheme
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MAX_BACKUP_FILE_BYTES = 5 * 1024 * 1024

@Composable
fun SettingsRoute(onHistoryClick: () -> Unit, viewModel: SettingsViewModel = viewModel()) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingExportJson by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.onAction(SettingsAction.NotificationPermissionResult(isGranted))
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val json = pendingExportJson
        pendingExportJson = null
        if (uri != null && json != null) {
            scope.launch {
                val result = runCatching {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                            writer.write(json)
                        } ?: throw IOException("Unable to open backup destination")
                    }
                }
                viewModel.onAction(
                    if (result.isSuccess) {
                        SettingsAction.ExportFileWritten
                    } else {
                        SettingsAction.ExportFileWriteFailed
                    }
                )
            }
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val result = runCatching {
                    withContext(Dispatchers.IO) {
                        readBackupJson(context.contentResolver.openInputStream(uri), uri)
                    }
                }
                result.fold(
                    onSuccess = { json ->
                        viewModel.onAction(SettingsAction.ImportJsonRead(json))
                    },
                    onFailure = {
                        viewModel.onAction(SettingsAction.ImportFileReadFailed)
                    }
                )
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                SettingsEffect.NavigateHistory -> onHistoryClick()

                SettingsEffect.RequestNotificationPermission -> {
                    permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }

                is SettingsEffect.LaunchCreateBackupDocument -> {
                    pendingExportJson = effect.json
                    createDocumentLauncher.launch(effect.fileName)
                }

                SettingsEffect.LaunchOpenBackupDocument -> {
                    openDocumentLauncher.launch(arrayOf("application/json"))
                }

                is SettingsEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    SettingsScreen(
        uiState = uiState,
        uiAction = viewModel::onAction,
        snackbarHostState = snackbarHostState
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    uiAction: (SettingsAction) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("設定") })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "通知",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            Card {
                ListItem(
                    headlineContent = { Text("更新リマインダー") },
                    supportingContent = { Text("サブスクの更新日前に通知でお知らせします") },
                    leadingContent = {
                        Icon(
                            if (uiState.notificationsEnabled) {
                                Icons.Filled.NotificationsActive
                            } else {
                                Icons.Filled.Notifications
                            },
                            contentDescription = null
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = uiState.notificationsEnabled,
                            onCheckedChange = {
                                uiAction(SettingsAction.NotificationsToggled(it))
                            }
                        )
                    }
                )
            }

            Text(
                text = "データ",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            Card {
                Column {
                    ListItem(
                        headlineContent = { Text("解約済み履歴") },
                        supportingContent = { Text("削除したサブスクと削減額を確認できます") },
                        leadingContent = {
                            Icon(Icons.Filled.History, contentDescription = null)
                        },
                        trailingContent = {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null
                            )
                        },
                        modifier = Modifier.clickable {
                            uiAction(SettingsAction.HistoryClick)
                        }
                    )

                    HorizontalDivider()

                    ListItem(
                        headlineContent = { Text("JSONにエクスポート") },
                        supportingContent = { Text("登録情報をJSONファイルとして保存します") },
                        leadingContent = {
                            Icon(Icons.Filled.FileUpload, contentDescription = null)
                        },
                        trailingContent = {
                            if (uiState.isBackupLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        },
                        modifier = Modifier.clickable(enabled = !uiState.isBackupLoading) {
                            uiAction(SettingsAction.ExportClick)
                        }
                    )

                    HorizontalDivider()

                    ListItem(
                        headlineContent = { Text("JSONからインポート") },
                        supportingContent = { Text("バックアップで現在の登録情報を置き換えます") },
                        leadingContent = {
                            Icon(Icons.Filled.FileDownload, contentDescription = null)
                        },
                        modifier = Modifier.clickable(enabled = !uiState.isBackupLoading) {
                            uiAction(SettingsAction.ImportClick)
                        }
                    )

                    Text(
                        text = "サブスク登録情報をJSONファイルとして保存できます。Google Drive連携は今後対応予定です。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }

            Text(
                text = "アプリ情報",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            Card {
                Column {
                    ListItem(
                        headlineContent = { Text("SubCan") },
                        supportingContent = { Text("サブスクリプション管理アプリ") },
                        leadingContent = {
                            Icon(Icons.Filled.Info, contentDescription = null)
                        }
                    )
                    ListItem(
                        headlineContent = { Text("バージョン") },
                        supportingContent = { Text(BuildConfig.VERSION_NAME) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (uiState.isImportConfirmDialogVisible) {
        AlertDialog(
            onDismissRequest = { uiAction(SettingsAction.ImportCancel) },
            title = { Text("バックアップから復元") },
            text = {
                Text("現在の登録情報をバックアップファイルの内容で置き換えます。よろしいですか？")
            },
            confirmButton = {
                TextButton(onClick = { uiAction(SettingsAction.ImportConfirm) }) {
                    Text("置き換える")
                }
            },
            dismissButton = {
                TextButton(onClick = { uiAction(SettingsAction.ImportCancel) }) {
                    Text("キャンセル")
                }
            }
        )
    }
}

private fun readBackupJson(inputStream: java.io.InputStream?, uri: Uri): String {
    val stream = inputStream ?: throw IOException("Unable to open backup source: $uri")
    return stream.use { input ->
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var totalBytes = 0
        while (true) {
            val bytesRead = input.read(buffer)
            if (bytesRead < 0) break
            totalBytes += bytesRead
            if (totalBytes > MAX_BACKUP_FILE_BYTES) {
                throw IOException("Backup file is too large")
            }
            output.write(buffer, 0, bytesRead)
        }
        output.toString(Charsets.UTF_8.name())
    }
}

@Preview(showBackground = true, name = "Settings")
@Composable
private fun SettingsScreenPreview() {
    SubCanTheme {
        SettingsScreen(
            uiState = SettingsUiState(notificationsEnabled = true),
            uiAction = {}
        )
    }
}
