package com.example.subcan.ui.preset

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.example.subcan.data.db.Subscription
import com.example.subcan.ui.mvi.BaseMviViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

data class PresetSelectUiState(val subscriptions: List<Subscription> = emptyList())

sealed interface PresetSelectAction {
    data class TemplateClick(val subscriptionId: Long) : PresetSelectAction
    data object AddClick : PresetSelectAction
    data object BackClick : PresetSelectAction
}

sealed interface PresetSelectEffect {
    data class NavigateToTemplate(val subscriptionId: Long) : PresetSelectEffect
    data object NavigateToCustomEditor : PresetSelectEffect
    data object NavigateBack : PresetSelectEffect
}

class PresetSelectViewModel(application: Application) :
    BaseMviViewModel<PresetSelectUiState, PresetSelectAction, PresetSelectEffect>(
        application = application,
        initialState = PresetSelectUiState()
    ) {

    init {
        repository.getAllSubscriptions()
            .onEach { subscriptions ->
                setState(
                    PresetSelectUiState(
                        subscriptions = subscriptions.sortedByDescending { it.id }
                    )
                )
            }
            .launchIn(viewModelScope)
    }

    override suspend fun handleAction(action: PresetSelectAction) {
        when (action) {
            PresetSelectAction.AddClick -> emitEffect(PresetSelectEffect.NavigateToCustomEditor)

            PresetSelectAction.BackClick -> emitEffect(PresetSelectEffect.NavigateBack)

            is PresetSelectAction.TemplateClick -> {
                emitEffect(PresetSelectEffect.NavigateToTemplate(action.subscriptionId))
            }
        }
    }
}
