package com.example.subcan.ui.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.subcan.ui.components.CategoryIcon
import com.example.subcan.ui.preview.PreviewData
import com.example.subcan.ui.theme.SubCanTheme

@Composable
fun AnalyticsRoute(viewModel: AnalyticsViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AnalyticsScreen(
        uiState = uiState,
        uiAction = viewModel::onAction
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("UNUSED_PARAMETER")
@Composable
fun AnalyticsScreen(uiState: AnalyticsUiState, uiAction: (AnalyticsAction) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("アナリティクス") })
        }
    ) { padding ->
        if (uiState.activeCount == 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Filled.Analytics,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Text(
                        text = "データがありません",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "サブスクを登録すると\nここに分析結果が表示されます",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // サマリーカード
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SummaryCard(
                        title = "月額合計",
                        value = "¥${"%,.0f".format(uiState.totalMonthly)}",
                        icon = Icons.Filled.CreditCard,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        title = "年額合計",
                        value = "¥${"%,.0f".format(uiState.totalYearly)}",
                        icon = Icons.Filled.CalendarMonth,
                        modifier = Modifier.weight(1f)
                    )
                }

                SummaryCard(
                    title = "契約中のサブスク",
                    value = "${uiState.activeCount}件",
                    icon = Icons.Filled.Subscriptions,
                    modifier = Modifier.fillMaxWidth()
                )

                // カテゴリ別内訳
                Text(
                    text = "カテゴリ別内訳（月額換算）",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val maxMonthly = uiState.categoryBreakdowns.maxOfOrNull { it.monthlyTotal } ?: 1.0

                        uiState.categoryBreakdowns.forEachIndexed { index, breakdown ->
                            if (index > 0) {
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                CategoryIcon(
                                    category = breakdown.category,
                                    size = 32
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${breakdown.category.label}（${breakdown.count}件）",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = "¥${"%,.0f".format(breakdown.monthlyTotal)}/月",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { (breakdown.monthlyTotal / maxMonthly).toFloat() },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(CircleShape),
                                        color = breakdown.category.color,
                                        trackColor = breakdown.category.color.copy(alpha = 0.12f)
                                    )
                                }
                            }
                        }
                    }
                }

                // サブスク一覧（金額順）
                Text(
                    text = "金額順一覧",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        val sorted = uiState.subscriptions.sortedByDescending { it.monthlyPrice }
                        sorted.forEach { sub ->
                            ListItem(
                                headlineContent = { Text(sub.name) },
                                supportingContent = {
                                    Text("${sub.billingCycle.label} ¥${"%,d".format(sub.price)}")
                                },
                                trailingContent = {
                                    Text(
                                        text = "¥${"%,.0f".format(sub.monthlyPrice)}/月",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                },
                                leadingContent = {
                                    CategoryIcon(category = sub.category, size = 36)
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SummaryCard(title: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Analytics")
@Composable
private fun AnalyticsScreenPreview() {
    val subs = PreviewData.sampleSubscriptions
    val totalMonthly = subs.sumOf { it.monthlyPrice }
    val breakdowns = subs.groupBy { it.category }.map { (cat, list) ->
        CategoryBreakdown(cat, list.sumOf { it.monthlyPrice }, list.size)
    }.sortedByDescending { it.monthlyTotal }

    SubCanTheme {
        AnalyticsScreen(
            uiState = AnalyticsUiState(
                totalMonthly = totalMonthly,
                totalYearly = totalMonthly * 12,
                activeCount = subs.size,
                categoryBreakdowns = breakdowns,
                subscriptions = subs
            ),
            uiAction = {}
        )
    }
}
