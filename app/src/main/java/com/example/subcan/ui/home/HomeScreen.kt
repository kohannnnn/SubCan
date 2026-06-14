package com.example.subcan.ui.home

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.subcan.ui.components.SubscriptionCard
import com.example.subcan.ui.preview.PreviewData
import com.example.subcan.ui.theme.SubCanTheme

@Composable
fun HomeRoute(onAddClick: () -> Unit, onSubscriptionClick: (Long) -> Unit, viewModel: HomeViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                HomeEffect.NavigateToPreset -> onAddClick()
                is HomeEffect.NavigateToDetail -> onSubscriptionClick(effect.subscriptionId)
            }
        }
    }

    HomeScreen(
        uiState = uiState,
        uiAction = viewModel::onAction
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(uiState: HomeUiState, uiAction: (HomeAction) -> Unit) {
    Scaffold(
        topBar = {
            MediumTopAppBar(
                title = {
                    Text("サブスク管理")
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { uiAction(HomeAction.AddClick) },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Filled.Add, contentDescription = "サブスクを追加")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            SubscriptionFilters(
                selectedFilter = uiState.selectedFilter,
                onFilterSelected = { uiAction(HomeAction.FilterSelected(it)) }
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (uiState.subscriptions.isEmpty()) {
                    if (uiState.hasAnySubscriptions) {
                        FilterEmptyState(filter = uiState.selectedFilter)
                    } else {
                        EmptyState()
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = uiState.subscriptions,
                            key = { it.id }
                        ) { subscription ->
                            SubscriptionCard(
                                subscription = subscription,
                                onClick = {
                                    uiAction(HomeAction.SubscriptionClick(subscription.id))
                                }
                            )
                        }
                        // FABと重ならないようにスペースを追加
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubscriptionFilters(selectedFilter: HomeFilter, onFilterSelected: (HomeFilter) -> Unit) {
    val filters = listOf(
        HomeFilter.ALL,
        HomeFilter.ACTIVE,
        HomeFilter.ENDING,
        HomeFilter.INACTIVE
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filters.forEach { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text(filter.label) }
            )
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Subscriptions,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Text(
                text = "サブスクリプションがありません",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "右下の＋ボタンから\nサブスクを登録しましょう",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun FilterEmptyState(filter: HomeFilter, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "${filter.label}のサブスクリプションはありません",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Home - サブスクあり")
@Composable
private fun HomeScreenWithDataPreview() {
    SubCanTheme {
        HomeScreen(
            uiState = HomeUiState(allSubscriptions = PreviewData.sampleSubscriptions),
            uiAction = {}
        )
    }
}

@Preview(showBackground = true, name = "Home - 空状態")
@Composable
private fun HomeScreenEmptyPreview() {
    SubCanTheme {
        EmptyState()
    }
}
