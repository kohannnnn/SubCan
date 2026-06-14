package com.example.subcan.ui.mvi

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.subcan.SubCanApplication
import com.example.subcan.data.repository.SubscriptionRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

abstract class BaseMviViewModel<UiState, UiAction, UiEffect>(application: Application, initialState: UiState) :
    AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<UiEffect>(extraBufferCapacity = 1)
    val uiEffect: SharedFlow<UiEffect> = _uiEffect.asSharedFlow()

    protected val repository: SubscriptionRepository
        get() = (getApplication<SubCanApplication>()).repository

    fun onAction(action: UiAction) {
        viewModelScope.launch {
            handleAction(action)
        }
    }

    protected abstract suspend fun handleAction(action: UiAction)

    protected fun updateState(transform: UiState.() -> UiState) {
        _uiState.update(transform)
    }

    protected fun setState(state: UiState) {
        _uiState.value = state
    }

    protected suspend fun emitEffect(effect: UiEffect) {
        _uiEffect.emit(effect)
    }
}
