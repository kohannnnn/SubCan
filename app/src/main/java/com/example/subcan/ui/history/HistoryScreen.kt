package com.example.subcan.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.subcan.data.db.SubscriptionArchive
import com.example.subcan.ui.components.CategoryIcon
import com.example.subcan.ui.preview.PreviewData
import com.example.subcan.ui.theme.SubCanTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun HistoryRoute(onBack: () -> Unit, viewModel: HistoryViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                HistoryEffect.NavigateBack -> onBack()
            }
        }
    }

    HistoryScreen(
        uiState = uiState,
        uiAction = viewModel::onAction
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(uiState: HistoryUiState, uiAction: (HistoryAction) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("解約済み履歴") },
                navigationIcon = {
                    IconButton(onClick = { uiAction(HistoryAction.BackClick) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.archives.isEmpty()) {
            EmptyHistoryState(modifier = Modifier.padding(padding))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                HistorySummaryCard(uiState = uiState)

                uiState.archives.forEach { archive ->
                    ArchiveCard(
                        archive = archive,
                        onDeleteClick = {
                            uiAction(HistoryAction.DeleteClick(archive))
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    uiState.pendingDeleteArchive?.let { archive ->
        AlertDialog(
            onDismissRequest = { uiAction(HistoryAction.DeleteDismiss) },
            title = { Text("完全に削除") },
            text = {
                Text("「${archive.name}」を履歴から完全に削除します。この操作は取り消せません。")
            },
            confirmButton = {
                TextButton(onClick = { uiAction(HistoryAction.DeleteConfirm) }) {
                    Text("完全に削除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { uiAction(HistoryAction.DeleteDismiss) }) {
                    Text("キャンセル")
                }
            }
        )
    }
}

@Composable
private fun HistorySummaryCard(uiState: HistoryUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "これまでに減らせた固定費",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = "${uiState.archives.size}件の解約履歴",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
            )
            Text(
                text = "月額換算 ¥${"%,.0f".format(uiState.totalMonthlySavings)}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = "年額換算 ¥${"%,.0f".format(uiState.totalYearlySavings)}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun ArchiveCard(archive: SubscriptionArchive, onDeleteClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CategoryIcon(category = archive.category, size = 44)
                Text(
                    text = archive.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "¥${"%,d".format(archive.price)} / ${archive.billingCycle.label}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider()

            HistoryFact(label = "解約日", value = archive.canceledAt.toJapaneseDate())
            HistoryFact(label = "最終利用日", value = archive.finalUsageDate.toJapaneseDate())
            HistoryFact(label = "削減できた月額", value = "¥${"%,.0f".format(archive.monthlySavings)}")
            HistoryFact(label = "削減できた年額", value = "¥${"%,.0f".format(archive.yearlySavings)}")

            if (archive.notes.isNotBlank()) {
                HorizontalDivider()
                Text(
                    text = archive.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedButton(
                onClick = onDeleteClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.DeleteForever, contentDescription = null)
                Text("履歴から完全に削除")
            }
        }
    }
}

@Composable
private fun HistoryFact(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun EmptyHistoryState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.History,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Text(
                text = "解約済み履歴はまだありません",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "契約一覧から履歴へ移したサブスクが\nここに残ります",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun LocalDate.toJapaneseDate(): String = format(DateTimeFormatter.ofPattern("yyyy年M月d日"))

@Preview(showBackground = true)
@Composable
private fun HistoryScreenPreview() {
    val sampleArchive = SubscriptionArchive.fromSubscription(
        subscription = PreviewData.sampleSubscriptions.first().copy(
            autoRenew = false,
            canceledAt = LocalDate.now().minusDays(10)
        ),
        archivedAt = LocalDate.now()
    )

    SubCanTheme {
        HistoryScreen(
            uiState = HistoryUiState(
                archives = listOf(sampleArchive)
            ),
            uiAction = {}
        )
    }
}
