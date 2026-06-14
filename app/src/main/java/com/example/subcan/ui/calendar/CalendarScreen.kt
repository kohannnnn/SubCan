package com.example.subcan.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.subcan.data.model.BillingOccurrence
import com.example.subcan.ui.components.CategoryIcon
import com.example.subcan.ui.preview.PreviewData
import com.example.subcan.ui.theme.SubCanTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val weekdayLabels = listOf("日", "月", "火", "水", "木", "金", "土")

@Composable
fun CalendarRoute(onSubscriptionClick: (Long) -> Unit, viewModel: CalendarViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is CalendarEffect.NavigateToDetail -> onSubscriptionClick(effect.subscriptionId)
            }
        }
    }

    CalendarScreen(
        uiState = uiState,
        uiAction = viewModel::onAction
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CalendarScreen(uiState: CalendarUiState, uiAction: (CalendarAction) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("更新カレンダー") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CalendarControls(
                uiState = uiState,
                uiAction = uiAction
            )

            if (!uiState.hasAnySubscriptions) {
                CalendarEmptyState(
                    title = "請求予定がありません",
                    description = "サブスクを登録すると、月表示と週表示で引き落とし予定を確認できます"
                )
            } else {
                CalendarSummaryCard(summary = uiState.summary, viewMode = uiState.viewMode)

                if (uiState.viewMode == CalendarViewMode.MONTH) {
                    MonthCalendar(
                        monthGrid = uiState.monthGrid,
                        onDaySelected = { uiAction(CalendarAction.DaySelected(it)) }
                    )
                    PeriodAgenda(
                        title = "今月の引き落とし",
                        days = uiState.periodDays.filter { it.occurrences.isNotEmpty() },
                        emptyMessage = "この月の引き落とし予定はありません",
                        showEmptyDays = false,
                        onSubscriptionClick = { uiAction(CalendarAction.SubscriptionClick(it)) }
                    )
                } else {
                    PeriodAgenda(
                        title = "今週の引き落とし",
                        days = uiState.periodDays.filter { it.occurrences.isNotEmpty() },
                        emptyMessage = "この週の引き落とし予定はありません",
                        showEmptyDays = false,
                        onSubscriptionClick = { uiAction(CalendarAction.SubscriptionClick(it)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CalendarControls(uiState: CalendarUiState, uiAction: (CalendarAction) -> Unit) {
    var modeMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                OutlinedButton(
                    onClick = { modeMenuExpanded = true }
                ) {
                    Text(uiState.viewMode.label)
                    Icon(
                        Icons.Filled.ArrowDropDown,
                        contentDescription = "表示モードを選択"
                    )
                }

                DropdownMenu(
                    expanded = modeMenuExpanded,
                    onDismissRequest = { modeMenuExpanded = false }
                ) {
                    CalendarViewMode.entries.forEach { mode ->
                        DropdownMenuItem(
                            text = { Text(mode.label) },
                            onClick = {
                                modeMenuExpanded = false
                                uiAction(CalendarAction.ViewModeSelected(mode))
                            },
                            leadingIcon = {
                                if (uiState.viewMode == mode) {
                                    Icon(Icons.Filled.Check, contentDescription = null)
                                }
                            }
                        )
                    }
                }
            }

            AssistChip(
                onClick = { uiAction(CalendarAction.TodayClick) },
                label = { Text("今日") }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { uiAction(CalendarAction.PreviousPeriodClick) }) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = "前の期間")
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = uiState.periodTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
                IconButton(onClick = { uiAction(CalendarAction.NextPeriodClick) }) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = "次の期間")
                }
            }
        }
    }
}

@Composable
private fun CalendarSummaryCard(summary: CalendarSummaryUiModel, viewMode: CalendarViewMode) {
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
                text = if (viewMode == CalendarViewMode.MONTH) "この月の請求サマリー" else "この週の請求サマリー",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryValue(
                    label = "件数",
                    value = "${summary.scheduledCount}件",
                    modifier = Modifier.weight(1f)
                )
                SummaryValue(
                    label = "合計",
                    value = "¥${"%,d".format(summary.totalAmount)}",
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = if (summary.peakDays.isEmpty()) {
                    "請求が集中する日はまだありません"
                } else {
                    "集中日: ${formatPeakDays(summary.peakDays)}  合計 ¥${"%,d".format(summary.peakAmount)}"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun SummaryValue(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
private fun MonthCalendar(monthGrid: List<List<CalendarDayUiModel>>, onDaySelected: (LocalDate) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "月内の引き落とし分布",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    weekdayLabels.forEach { label ->
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                monthGrid.forEach { week ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        week.forEach { day ->
                            MonthDayCell(
                                day = day,
                                onClick = { onDaySelected(day.date) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthDayCell(day: CalendarDayUiModel, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val isToday = day.date == LocalDate.now()
    val hasCharges = day.occurrences.isNotEmpty()
    val containerColor = when {
        !day.isCurrentMonth -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        hasCharges -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }
    val contentColor = when {
        !day.isCurrentMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
        hasCharges -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Surface(
        modifier = modifier
            .aspectRatio(0.82f)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        tonalElevation = if (hasCharges) 2.dp else 0.dp,
        border = if (isToday) {
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = day.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                color = contentColor
            )

            if (hasCharges) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "¥${"%,d".format(day.totalAmount)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor
                    )
                    Text(
                        text = "${day.occurrences.size}件",
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.8f)
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        day.occurrences.take(4).forEach { occurrence ->
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(occurrence.category.color)
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = " ",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun PeriodAgenda(
    title: String,
    days: List<CalendarDayUiModel>,
    emptyMessage: String,
    showEmptyDays: Boolean,
    onSubscriptionClick: (Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )

        if (days.none { it.occurrences.isNotEmpty() }) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = emptyMessage,
                    modifier = Modifier.padding(20.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val targetDays = if (showEmptyDays) days else days.filter { it.occurrences.isNotEmpty() }
            targetDays.forEach { day ->
                DayAgendaCard(
                    day = day,
                    showEmptyState = showEmptyDays,
                    onSubscriptionClick = onSubscriptionClick
                )
            }
        }
    }
}

@Composable
private fun DayAgendaCard(day: CalendarDayUiModel, showEmptyState: Boolean, onSubscriptionClick: (Long) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = day.date.format(DateTimeFormatter.ofPattern("M月d日 (E)", Locale.JAPAN)),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (day.occurrences.isEmpty()) {
                            "引き落とし予定なし"
                        } else {
                            "${day.occurrences.size}件の請求予定"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = if (day.occurrences.isEmpty()) {
                        "-"
                    } else {
                        "¥${"%,d".format(day.totalAmount)}"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (day.occurrences.isEmpty()) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            }

            if (day.occurrences.isEmpty()) {
                if (showEmptyState) {
                    HorizontalDivider()
                    Text(
                        text = "この日に予定されている引き落としはありません",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                HorizontalDivider()
                day.occurrences.forEachIndexed { index, occurrence ->
                    if (index > 0) {
                        HorizontalDivider()
                    }
                    BillingOccurrenceRow(
                        occurrence = occurrence,
                        onClick = { onSubscriptionClick(occurrence.subscriptionId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BillingOccurrenceRow(occurrence: BillingOccurrence, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryIcon(category = occurrence.category, size = 36)
        Spacer(modifier = Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = occurrence.subscriptionName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = occurrence.billingCycle.label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "¥${"%,d".format(occurrence.price)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun CalendarEmptyState(title: String, description: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.CalendarMonth,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun formatPeakDays(dates: List<LocalDate>): String {
    val visibleDates = dates.take(2).joinToString(", ") {
        it.format(DateTimeFormatter.ofPattern("M/d", Locale.JAPAN))
    }
    return if (dates.size <= 2) {
        visibleDates
    } else {
        "$visibleDates ほか${dates.size - 2}日"
    }
}

@Preview(showBackground = true)
@Composable
private fun CalendarScreenPreview() {
    val previewOccurrence = BillingOccurrence(
        subscriptionId = 1,
        subscriptionName = "Netflix",
        date = LocalDate.now(),
        price = 1_590,
        category = PreviewData.sampleSubscriptions.first().category,
        billingCycle = PreviewData.sampleSubscriptions.first().billingCycle
    )
    val sampleDays = listOf(
        CalendarDayUiModel(
            date = LocalDate.now(),
            occurrences = listOf(previewOccurrence),
            isCurrentMonth = true
        )
    )

    SubCanTheme {
        CalendarScreen(
            uiState = CalendarUiState(
                allSubscriptions = PreviewData.sampleSubscriptions,
                viewMode = CalendarViewMode.WEEK,
                anchorDate = LocalDate.now(),
                periodTitle = "プレビュー",
                periodStart = LocalDate.now(),
                periodEnd = LocalDate.now().plusDays(6),
                periodDays = sampleDays,
                summary = CalendarSummaryUiModel(
                    scheduledCount = sampleDays.sumOf { it.occurrences.size },
                    totalAmount = sampleDays.sumOf { it.totalAmount }
                )
            ),
            uiAction = {}
        )
    }
}
