package com.example.subcan.ui.detail

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.subcan.ui.components.CategoryIcon
import com.example.subcan.ui.preview.PreviewData
import com.example.subcan.ui.theme.SubCanTheme
import java.time.format.DateTimeFormatter

@Composable
fun DetailRoute(
    subscriptionId: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onArchived: () -> Unit,
    viewModel: DetailViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(subscriptionId) {
        viewModel.onAction(DetailAction.Load(subscriptionId))
    }

    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                DetailEffect.NavigateBack -> onBack()

                is DetailEffect.NavigateToEdit -> onEdit(effect.subscriptionId)

                DetailEffect.NavigateToHistory -> onArchived()

                is DetailEffect.OpenCancellationUrl -> {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, effect.url.toBrowserUrl().toUri())
                    )
                }
            }
        }
    }

    DetailScreen(
        uiState = uiState,
        uiAction = viewModel::onAction
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(uiState: DetailUiState, uiAction: (DetailAction) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("詳細") },
                navigationIcon = {
                    IconButton(onClick = { uiAction(DetailAction.BackClick) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
                actions = {
                    uiState.subscription?.let {
                        IconButton(onClick = { uiAction(DetailAction.EditClick) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "編集")
                        }
                        IconButton(onClick = { uiAction(DetailAction.DeleteClick) }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "削除",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        uiState.subscription?.let { sub ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ヘッダー
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CategoryIcon(category = sub.category, size = 64)
                        Text(
                            text = sub.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "¥${"%,d".format(sub.price)} / ${sub.billingCycle.label}",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        if (!sub.isActive) {
                            Text(
                                text = "⏸ 停止中",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else if (!sub.autoRenew) {
                            Text(
                                text = "⏳ 期間終了で停止予定",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        Text(
                            text = "月額換算: ¥${"%,.0f".format(
                                sub.monthlyPrice
                            )} / 年額換算: ¥${"%,.0f".format(sub.yearlyPrice)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }

                // 契約情報
                Text(
                    text = "契約情報",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        ListItem(
                            headlineContent = { Text("契約開始日") },
                            supportingContent = {
                                Text(sub.startDate.format(DateTimeFormatter.ofPattern("yyyy年M月d日")))
                            },
                            leadingContent = {
                                Icon(Icons.Filled.CalendarMonth, contentDescription = null)
                            }
                        )
                        HorizontalDivider()
                        ListItem(
                            headlineContent = {
                                Text(if (sub.autoRenew) "次回更新日" else "利用終了日")
                            },
                            supportingContent = {
                                Text(sub.nextBillingDate.format(DateTimeFormatter.ofPattern("yyyy年M月d日")))
                            },
                            leadingContent = {
                                Icon(Icons.Filled.CreditCard, contentDescription = null)
                            }
                        )
                        HorizontalDivider()
                        ListItem(
                            headlineContent = { Text("通知") },
                            supportingContent = {
                                Text("更新の${sub.reminderDaysBefore}日前に通知")
                            },
                            leadingContent = {
                                Icon(Icons.Filled.Notifications, contentDescription = null)
                            }
                        )
                    }
                }

                // サブスクの特徴
                Text(
                    text = "サブスクの特徴",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        ListItem(
                            headlineContent = { Text("請求開始日") },
                            supportingContent = {
                                Text(sub.billingStartPolicy.label)
                            },
                            leadingContent = {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            }
                        )
                        HorizontalDivider()
                        ListItem(
                            headlineContent = { Text("解約時の扱い") },
                            supportingContent = {
                                Column {
                                    Text(sub.cancellationPolicy.label)
                                    Text(
                                        sub.cancellationPolicy.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            leadingContent = {
                                Icon(Icons.Filled.Info, contentDescription = null)
                            }
                        )
                        if (sub.hasFreeTrial) {
                            HorizontalDivider()
                            ListItem(
                                headlineContent = { Text("無料トライアル") },
                                supportingContent = {
                                    val endStr = sub.freeTrialEndDate?.format(
                                        DateTimeFormatter.ofPattern("yyyy年M月d日")
                                    ) ?: ""
                                    Text("${sub.freeTrialDays}日間${if (endStr.isNotEmpty()) " (${endStr}まで)" else ""}")
                                },
                                leadingContent = {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = null)
                                }
                            )
                        }
                    }
                }

                // メモ
                if (sub.notes.isNotBlank()) {
                    Text(
                        text = "メモ",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = sub.notes,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // 重要操作
                Text(
                    text = "操作",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedButton(
                    onClick = { uiAction(DetailAction.ToggleActive) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        if (sub.autoRenew) Icons.Filled.Block else Icons.Filled.CheckCircle,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        when {
                            sub.autoRenew -> "解約する"
                            sub.isActive -> "継続する"
                            else -> "再開する"
                        }
                    )
                }

                if (sub.cancellationUrl.isNotBlank()) {
                    OutlinedButton(
                        onClick = { uiAction(DetailAction.OpenCancellationPage) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("解約ページを開く")
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // 削除確認ダイアログ
    if (uiState.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { uiAction(DetailAction.DeleteDismiss) },
            title = { Text("解約済み履歴へ移動") },
            text = {
                Text(
                    "「${uiState.subscription?.name}」を契約一覧から外して履歴へ移動します。"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { uiAction(DetailAction.DeleteConfirm) }
                ) {
                    Text("履歴へ移動", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { uiAction(DetailAction.DeleteDismiss) }) {
                    Text("キャンセル")
                }
            }
        )
    }
}

private fun String.toBrowserUrl(): String =
    if (startsWith("http://") || startsWith("https://")) this else "https://$this"

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Detail")
@Composable
private fun DetailScreenPreview() {
    SubCanTheme {
        DetailScreen(
            uiState = DetailUiState(subscription = PreviewData.sampleSubscriptions[2]),
            uiAction = {}
        )
    }
}
