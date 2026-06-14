package com.example.subcan.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.subcan.notification.NotificationScheduler
import com.example.subcan.ui.theme.SubCanTheme

data class SettingsUiState(val notificationsEnabled: Boolean = true)

sealed interface SettingsAction {
    data class NotificationsToggled(val enabled: Boolean) : SettingsAction
    data object HistoryClick : SettingsAction
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsRoute(onHistoryClick: () -> Unit) {
    val context = LocalContext.current

    var uiState by remember {
        mutableStateOf(
            SettingsUiState(
                notificationsEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                } else {
                    true
                }
            )
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        uiState = uiState.copy(notificationsEnabled = isGranted)
        if (isGranted) {
            NotificationScheduler.scheduleDailyCheck(context)
        } else {
            NotificationScheduler.cancelDailyCheck(context)
        }
    }

    SettingsScreen(
        uiState = uiState,
        uiAction = { action ->
            when (action) {
                SettingsAction.HistoryClick -> onHistoryClick()

                is SettingsAction.NotificationsToggled -> {
                    if (action.enabled) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            uiState = uiState.copy(notificationsEnabled = true)
                            NotificationScheduler.scheduleDailyCheck(context)
                        }
                    } else {
                        uiState = uiState.copy(notificationsEnabled = false)
                        NotificationScheduler.cancelDailyCheck(context)
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(uiState: SettingsUiState, uiAction: (SettingsAction) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("設定") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 通知設定
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
            }

            // アプリ情報
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
                        supportingContent = { Text("1.0.0") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
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
