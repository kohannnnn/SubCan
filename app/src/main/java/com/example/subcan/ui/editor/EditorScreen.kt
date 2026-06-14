package com.example.subcan.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.subcan.data.model.BillingCycle
import com.example.subcan.data.model.BillingStartPolicy
import com.example.subcan.data.model.CancellationPolicy
import com.example.subcan.data.model.SubscriptionCategory
import com.example.subcan.ui.theme.SubCanTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun EditorRoute(
    subscriptionId: Long? = null,
    templateId: Long? = null,
    onBack: () -> Unit,
    viewModel: EditorViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(subscriptionId, templateId) {
        viewModel.onAction(
            EditorAction.Initialize(
                subscriptionId = subscriptionId,
                templateId = templateId
            )
        )
    }

    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                EditorEffect.NavigateBack -> onBack()
            }
        }
    }

    EditorScreen(
        uiState = uiState,
        uiAction = viewModel::onAction
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditorScreen(uiState: EditorUiState, uiAction: (EditorAction) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (uiState.isEditing) "サブスクを編集" else "サブスクを追加")
                },
                navigationIcon = {
                    IconButton(onClick = { uiAction(EditorAction.BackClick) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { uiAction(EditorAction.SaveClick) },
                        enabled = uiState.isValid
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("保存")
                    }
                }
            )
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
            // === 基本情報 ===
            Text(
                text = "基本情報",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = uiState.name,
                onValueChange = { uiAction(EditorAction.NameChanged(it)) },
                label = { Text("サービス名 *") },
                placeholder = { Text("例: Netflix, Spotify") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.priceText,
                onValueChange = { uiAction(EditorAction.PriceChanged(it)) },
                label = { Text("料金（円） *") },
                placeholder = { Text("例: 1490") },
                prefix = { Text("¥") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            // 請求サイクル
            Text(
                text = "請求サイクル",
                style = MaterialTheme.typography.bodyMedium
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                BillingCycle.entries.forEachIndexed { index, cycle ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = BillingCycle.entries.size
                        ),
                        onClick = { uiAction(EditorAction.BillingCycleChanged(cycle)) },
                        selected = uiState.billingCycle == cycle,
                        label = { Text(cycle.label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            // カテゴリ
            Text(
                text = "カテゴリ",
                style = MaterialTheme.typography.bodyMedium
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SubscriptionCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = uiState.category == cat,
                        onClick = { uiAction(EditorAction.CategoryChanged(cat)) },
                        label = { Text(cat.label) },
                        leadingIcon = {
                            if (uiState.category == cat) {
                                Icon(Icons.Filled.Check, contentDescription = null)
                            } else {
                                Icon(
                                    cat.icon,
                                    contentDescription = null,
                                    tint = cat.color
                                )
                            }
                        }
                    )
                }
            }

            // 開始日
            OutlinedTextField(
                value = uiState.startDate.format(DateTimeFormatter.ofPattern("yyyy/MM/dd")),
                onValueChange = {},
                label = { Text("契約開始日") },
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { uiAction(EditorAction.OpenDatePicker) }) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = "日付を選択")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider()

            // === サブスクの特徴 ===
            Text(
                text = "サブスクの特徴",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            // 請求開始ポリシー
            Text(
                text = "請求開始日",
                style = MaterialTheme.typography.bodyMedium
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BillingStartPolicy.entries.forEach { policy ->
                    FilterChip(
                        selected = uiState.billingStartPolicy == policy,
                        onClick = { uiAction(EditorAction.BillingStartPolicyChanged(policy)) },
                        label = {
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Text(policy.label, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    policy.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        leadingIcon = if (uiState.billingStartPolicy == policy) {
                            { Icon(Icons.Filled.Check, contentDescription = null) }
                        } else {
                            null
                        }
                    )
                }
            }

            // 解約ポリシー
            Text(
                text = "解約時の扱い",
                style = MaterialTheme.typography.bodyMedium
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                CancellationPolicy.entries.forEach { policy ->
                    FilterChip(
                        selected = uiState.cancellationPolicy == policy,
                        onClick = { uiAction(EditorAction.CancellationPolicyChanged(policy)) },
                        label = {
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Text(policy.label, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    policy.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        leadingIcon = if (uiState.cancellationPolicy == policy) {
                            { Icon(Icons.Filled.Check, contentDescription = null) }
                        } else {
                            null
                        }
                    )
                }
            }

            // 無料トライアル
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("無料トライアルあり", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = uiState.hasFreeTrial,
                    onCheckedChange = { uiAction(EditorAction.FreeTrialChanged(it)) }
                )
            }
            if (uiState.hasFreeTrial) {
                OutlinedTextField(
                    value = uiState.freeTrialDaysText,
                    onValueChange = { uiAction(EditorAction.FreeTrialDaysChanged(it)) },
                    label = { Text("トライアル日数") },
                    suffix = { Text("日間") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            HorizontalDivider()

            // === 通知設定 ===
            Text(
                text = "通知設定",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "更新の${uiState.reminderDaysBefore}日前に通知",
                style = MaterialTheme.typography.bodyMedium
            )
            Slider(
                value = uiState.reminderDaysBefore.toFloat(),
                onValueChange = { uiAction(EditorAction.ReminderDaysChanged(it.toInt())) },
                valueRange = 1f..14f,
                steps = 12,
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider()

            // === 解約導線 ===
            Text(
                text = "解約導線",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = uiState.cancellationUrl,
                onValueChange = { uiAction(EditorAction.CancellationUrlChanged(it)) },
                label = { Text("解約URL") },
                placeholder = { Text("例: https://example.com/cancel") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider()

            // === メモ ===
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = { uiAction(EditorAction.NotesChanged(it)) },
                label = { Text("メモ") },
                placeholder = { Text("メモを追加（任意）") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Date Picker Dialog
    if (uiState.showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.startDate
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { uiAction(EditorAction.DismissDatePicker) },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val date = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            uiAction(EditorAction.DateSelected(date))
                        } ?: uiAction(EditorAction.DismissDatePicker)
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { uiAction(EditorAction.DismissDatePicker) }) {
                    Text("キャンセル")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EditorScreenPreview() {
    SubCanTheme {
        EditorScreen(
            uiState = EditorUiState(),
            uiAction = {}
        )
    }
}
