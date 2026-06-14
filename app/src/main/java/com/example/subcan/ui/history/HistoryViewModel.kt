package com.example.subcan.ui.history

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.example.subcan.data.db.SubscriptionArchive
import com.example.subcan.ui.mvi.BaseMviViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

data class HistoryUiState(
    val archives: List<SubscriptionArchive> = emptyList(),
    val pendingDeleteArchive: SubscriptionArchive? = null
) {
    val totalMonthlySavings: Double
        get() = archives.sumOf { it.monthlySavings }

    val totalYearlySavings: Double
        get() = archives.sumOf { it.yearlySavings }
}

sealed interface HistoryAction {
    data object BackClick : HistoryAction
    data class DeleteClick(val archive: SubscriptionArchive) : HistoryAction
    data object DeleteDismiss : HistoryAction
    data object DeleteConfirm : HistoryAction
}

sealed interface HistoryEffect {
    data object NavigateBack : HistoryEffect
}

class HistoryViewModel(application: Application) :
    BaseMviViewModel<HistoryUiState, HistoryAction, HistoryEffect>(application, HistoryUiState()) {

    init {
        repository.getArchivedSubscriptions()
            .onEach { archives ->
                updateState {
                    copy(
                        archives = archives,
                        pendingDeleteArchive = pendingDeleteArchive?.let { pending ->
                            archives.find { it.id == pending.id }
                        }
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    override suspend fun handleAction(action: HistoryAction) {
        when (action) {
            HistoryAction.BackClick -> emitEffect(HistoryEffect.NavigateBack)

            is HistoryAction.DeleteClick -> updateState { copy(pendingDeleteArchive = action.archive) }

            HistoryAction.DeleteDismiss -> updateState { copy(pendingDeleteArchive = null) }

            HistoryAction.DeleteConfirm -> {
                val archive = uiState.value.pendingDeleteArchive ?: return
                repository.deleteArchivedById(archive.id)
                updateState { copy(pendingDeleteArchive = null) }
            }
        }
    }
}
