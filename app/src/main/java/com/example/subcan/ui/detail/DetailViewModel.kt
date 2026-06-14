package com.example.subcan.ui.detail

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.example.subcan.data.db.Subscription
import com.example.subcan.data.model.SubscriptionLifecycleManager
import com.example.subcan.ui.mvi.BaseMviViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

data class DetailUiState(val subscription: Subscription? = null, val showDeleteDialog: Boolean = false)

sealed interface DetailAction {
    data class Load(val subscriptionId: Long) : DetailAction
    data object BackClick : DetailAction
    data object EditClick : DetailAction
    data object OpenCancellationPage : DetailAction
    data object DeleteClick : DetailAction
    data object DeleteDismiss : DetailAction
    data object DeleteConfirm : DetailAction
    data object ToggleActive : DetailAction
}

sealed interface DetailEffect {
    data object NavigateBack : DetailEffect
    data class NavigateToEdit(val subscriptionId: Long) : DetailEffect
    data object NavigateToHistory : DetailEffect
    data class OpenCancellationUrl(val url: String) : DetailEffect
}

class DetailViewModel(application: Application) :
    BaseMviViewModel<DetailUiState, DetailAction, DetailEffect>(application, DetailUiState()) {

    private var subscriptionJob: Job? = null

    override suspend fun handleAction(action: DetailAction) {
        when (action) {
            is DetailAction.Load -> observeSubscription(action.subscriptionId)

            DetailAction.BackClick -> emitEffect(DetailEffect.NavigateBack)

            DetailAction.EditClick -> {
                uiState.value.subscription?.let { subscription ->
                    emitEffect(DetailEffect.NavigateToEdit(subscription.id))
                }
            }

            DetailAction.OpenCancellationPage -> {
                val cancellationUrl = uiState.value.subscription?.cancellationUrl?.trim().orEmpty()
                if (cancellationUrl.isNotEmpty()) {
                    emitEffect(DetailEffect.OpenCancellationUrl(cancellationUrl))
                }
            }

            DetailAction.DeleteClick -> updateState { copy(showDeleteDialog = true) }

            DetailAction.DeleteDismiss -> updateState { copy(showDeleteDialog = false) }

            DetailAction.ToggleActive -> {
                uiState.value.subscription?.let { subscription ->
                    val updatedSubscription = if (subscription.autoRenew) {
                        SubscriptionLifecycleManager.cancel(subscription)
                    } else {
                        SubscriptionLifecycleManager.resume(subscription)
                    }
                    repository.update(updatedSubscription)
                }
            }

            DetailAction.DeleteConfirm -> {
                val subscription = uiState.value.subscription ?: return
                repository.archiveAndDelete(subscription)
                updateState { copy(showDeleteDialog = false) }
                emitEffect(DetailEffect.NavigateToHistory)
            }
        }
    }

    private fun observeSubscription(subscriptionId: Long) {
        subscriptionJob?.cancel()
        subscriptionJob = repository.getSubscriptionByIdFlow(subscriptionId)
            .onEach { subscription ->
                updateState { copy(subscription = subscription) }
            }
            .launchIn(viewModelScope)
    }
}
