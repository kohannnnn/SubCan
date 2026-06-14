package com.example.subcan.ui.editor

import android.app.Application
import com.example.subcan.data.db.Subscription
import com.example.subcan.data.model.BillingCycle
import com.example.subcan.data.model.BillingStartPolicy
import com.example.subcan.data.model.CancellationPolicy
import com.example.subcan.data.model.SubscriptionCategory
import com.example.subcan.data.model.SubscriptionLifecycleManager
import com.example.subcan.notification.NotificationScheduler
import com.example.subcan.ui.mvi.BaseMviViewModel
import java.time.LocalDate

data class EditorUiState(
    val editingId: Long? = null,
    val name: String = "",
    val description: String = "",
    val priceText: String = "",
    val billingCycle: BillingCycle = BillingCycle.MONTHLY,
    val category: SubscriptionCategory = SubscriptionCategory.OTHER,
    val startDate: LocalDate = LocalDate.now(),
    val canceledAt: LocalDate? = null,
    val billingStartPolicy: BillingStartPolicy = BillingStartPolicy.SIGNUP_DATE,
    val cancellationPolicy: CancellationPolicy = CancellationPolicy.END_OF_PERIOD,
    val hasFreeTrial: Boolean = false,
    val freeTrialDaysText: String = "",
    val reminderDaysBefore: Int = 3,
    val cancellationUrl: String = "",
    val notes: String = "",
    val showDatePicker: Boolean = false
) {
    val isEditing: Boolean
        get() = editingId != null

    val isValid: Boolean
        get() = name.isNotBlank() && (priceText.toIntOrNull() ?: 0) > 0
}

sealed interface EditorAction {
    data class Initialize(val subscriptionId: Long? = null, val templateId: Long? = null) : EditorAction

    data object BackClick : EditorAction
    data object SaveClick : EditorAction
    data class NameChanged(val value: String) : EditorAction
    data class PriceChanged(val value: String) : EditorAction
    data class BillingCycleChanged(val value: BillingCycle) : EditorAction
    data class CategoryChanged(val value: SubscriptionCategory) : EditorAction
    data object OpenDatePicker : EditorAction
    data object DismissDatePicker : EditorAction
    data class DateSelected(val value: LocalDate) : EditorAction
    data class BillingStartPolicyChanged(val value: BillingStartPolicy) : EditorAction
    data class CancellationPolicyChanged(val value: CancellationPolicy) : EditorAction
    data class FreeTrialChanged(val value: Boolean) : EditorAction
    data class FreeTrialDaysChanged(val value: String) : EditorAction
    data class ReminderDaysChanged(val value: Int) : EditorAction
    data class CancellationUrlChanged(val value: String) : EditorAction
    data class NotesChanged(val value: String) : EditorAction
}

sealed interface EditorEffect {
    data object NavigateBack : EditorEffect
}

class EditorViewModel(application: Application) :
    BaseMviViewModel<EditorUiState, EditorAction, EditorEffect>(application, EditorUiState()) {

    override suspend fun handleAction(action: EditorAction) {
        when (action) {
            is EditorAction.Initialize -> initialize(action.subscriptionId, action.templateId)

            EditorAction.BackClick -> emitEffect(EditorEffect.NavigateBack)

            EditorAction.SaveClick -> save()

            is EditorAction.NameChanged -> updateState { copy(name = action.value) }

            is EditorAction.PriceChanged -> {
                updateState { copy(priceText = action.value.filter { it.isDigit() }) }
            }

            is EditorAction.BillingCycleChanged -> updateState { copy(billingCycle = action.value) }

            is EditorAction.CategoryChanged -> updateState { copy(category = action.value) }

            EditorAction.OpenDatePicker -> updateState { copy(showDatePicker = true) }

            EditorAction.DismissDatePicker -> updateState { copy(showDatePicker = false) }

            is EditorAction.DateSelected -> {
                updateState {
                    copy(
                        startDate = action.value,
                        showDatePicker = false
                    )
                }
            }

            is EditorAction.BillingStartPolicyChanged -> {
                updateState { copy(billingStartPolicy = action.value) }
            }

            is EditorAction.CancellationPolicyChanged -> {
                updateState { copy(cancellationPolicy = action.value) }
            }

            is EditorAction.FreeTrialChanged -> {
                updateState {
                    copy(
                        hasFreeTrial = action.value,
                        freeTrialDaysText = if (action.value) freeTrialDaysText else ""
                    )
                }
            }

            is EditorAction.FreeTrialDaysChanged -> {
                updateState { copy(freeTrialDaysText = action.value.filter { it.isDigit() }) }
            }

            is EditorAction.ReminderDaysChanged -> {
                updateState { copy(reminderDaysBefore = action.value) }
            }

            is EditorAction.CancellationUrlChanged -> {
                updateState { copy(cancellationUrl = action.value) }
            }

            is EditorAction.NotesChanged -> updateState { copy(notes = action.value) }
        }
    }

    private suspend fun initialize(subscriptionId: Long?, templateId: Long?) {
        when {
            subscriptionId != null && subscriptionId > 0 -> loadSubscription(subscriptionId)
            templateId != null && templateId > 0 -> applyTemplate(templateId)
            else -> Unit
        }
    }

    private suspend fun loadSubscription(id: Long) {
        repository.getSubscriptionById(id)?.let { sub ->
            setState(
                uiState.value.copy(
                    editingId = sub.id,
                    name = sub.name,
                    description = sub.description,
                    priceText = sub.price.toString(),
                    billingCycle = sub.billingCycle,
                    category = sub.category,
                    startDate = sub.startDate,
                    canceledAt = sub.canceledAt,
                    billingStartPolicy = sub.billingStartPolicy,
                    cancellationPolicy = sub.cancellationPolicy,
                    hasFreeTrial = sub.hasFreeTrial,
                    freeTrialDaysText = if (sub.freeTrialDays > 0) sub.freeTrialDays.toString() else "",
                    reminderDaysBefore = sub.reminderDaysBefore,
                    cancellationUrl = sub.cancellationUrl,
                    notes = sub.notes,
                    showDatePicker = false
                )
            )
        }
    }

    private suspend fun applyTemplate(templateId: Long) {
        repository.getSubscriptionById(templateId)?.let { subscription ->
            setState(
                EditorUiState(
                    name = subscription.name,
                    description = subscription.description,
                    priceText = subscription.price.toString(),
                    billingCycle = subscription.billingCycle,
                    category = subscription.category,
                    startDate = LocalDate.now(),
                    canceledAt = null,
                    billingStartPolicy = subscription.billingStartPolicy,
                    cancellationPolicy = subscription.cancellationPolicy,
                    hasFreeTrial = subscription.hasFreeTrial,
                    freeTrialDaysText = if (subscription.freeTrialDays > 0) {
                        subscription.freeTrialDays.toString()
                    } else {
                        ""
                    },
                    reminderDaysBefore = subscription.reminderDaysBefore,
                    cancellationUrl = subscription.cancellationUrl,
                    notes = subscription.notes,
                    showDatePicker = false
                )
            )
        }
    }

    private suspend fun save() {
        val currentState = uiState.value
        if (!currentState.isValid) return

        val price = currentState.priceText.toInt()
        val freeTrialDays = currentState.freeTrialDaysText.toIntOrNull() ?: 0
        val nextBilling = SubscriptionLifecycleManager.calculateNextBillingDate(
            startDate = currentState.startDate,
            billingCycle = currentState.billingCycle
        )
        val freeTrialEndDate = if (currentState.hasFreeTrial && freeTrialDays > 0) {
            currentState.startDate.plusDays(freeTrialDays.toLong())
        } else {
            null
        }

        val subscription = Subscription(
            id = currentState.editingId ?: 0,
            name = currentState.name,
            description = currentState.description,
            price = price,
            billingCycle = currentState.billingCycle,
            category = currentState.category,
            startDate = currentState.startDate,
            nextBillingDate = nextBilling,
            billingStartPolicy = currentState.billingStartPolicy,
            canceledAt = currentState.canceledAt,
            cancellationPolicy = currentState.cancellationPolicy,
            hasFreeTrial = currentState.hasFreeTrial,
            freeTrialDays = freeTrialDays,
            freeTrialEndDate = freeTrialEndDate,
            reminderDaysBefore = currentState.reminderDaysBefore,
            cancellationUrl = currentState.cancellationUrl.trim(),
            notes = currentState.notes
        )

        if (currentState.editingId != null) {
            repository.update(subscription)
        } else {
            repository.insert(subscription)
        }

        NotificationScheduler.scheduleDailyCheck(getApplication())
        emitEffect(EditorEffect.NavigateBack)
    }
}
