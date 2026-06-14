package com.example.subcan.ui.home

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.example.subcan.data.db.Subscription
import com.example.subcan.ui.mvi.BaseMviViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

enum class HomeFilter(val label: String) {
    ACTIVE("契約中"),
    ALL("すべて"),
    ENDING("解約予定"),
    INACTIVE("停止済み")
}

data class HomeUiState(
    val allSubscriptions: List<Subscription> = emptyList(),
    val selectedFilter: HomeFilter = HomeFilter.ACTIVE
) {
    val subscriptions: List<Subscription>
        get() = when (selectedFilter) {
            HomeFilter.ACTIVE -> allSubscriptions.filter { it.isActive && it.autoRenew }
            HomeFilter.ALL -> allSubscriptions
            HomeFilter.ENDING -> allSubscriptions.filter { it.isActive && !it.autoRenew }
            HomeFilter.INACTIVE -> allSubscriptions.filter { !it.isActive }
        }

    val hasAnySubscriptions: Boolean
        get() = allSubscriptions.isNotEmpty()
}

sealed interface HomeAction {
    data object AddClick : HomeAction
    data class FilterSelected(val filter: HomeFilter) : HomeAction
    data class SubscriptionClick(val subscriptionId: Long) : HomeAction
}

sealed interface HomeEffect {
    data object NavigateToPreset : HomeEffect
    data class NavigateToDetail(val subscriptionId: Long) : HomeEffect
}

class HomeViewModel(application: Application) :
    BaseMviViewModel<HomeUiState, HomeAction, HomeEffect>(application, HomeUiState()) {

    init {
        repository.getAllSubscriptions()
            .onEach { subscriptions ->
                updateState { copy(allSubscriptions = subscriptions) }
            }
            .launchIn(viewModelScope)
    }

    override suspend fun handleAction(action: HomeAction) {
        when (action) {
            HomeAction.AddClick -> emitEffect(HomeEffect.NavigateToPreset)

            is HomeAction.FilterSelected -> {
                updateState { copy(selectedFilter = action.filter) }
            }

            is HomeAction.SubscriptionClick -> {
                emitEffect(HomeEffect.NavigateToDetail(action.subscriptionId))
            }
        }
    }
}
