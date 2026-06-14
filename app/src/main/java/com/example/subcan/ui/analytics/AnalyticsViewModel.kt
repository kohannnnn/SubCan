package com.example.subcan.ui.analytics

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.example.subcan.data.db.Subscription
import com.example.subcan.data.model.SubscriptionCategory
import com.example.subcan.ui.mvi.BaseMviViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

data class CategoryBreakdown(val category: SubscriptionCategory, val monthlyTotal: Double, val count: Int)

data class AnalyticsUiState(
    val totalMonthly: Double = 0.0,
    val totalYearly: Double = 0.0,
    val activeCount: Int = 0,
    val categoryBreakdowns: List<CategoryBreakdown> = emptyList(),
    val subscriptions: List<Subscription> = emptyList()
)

sealed interface AnalyticsAction

sealed interface AnalyticsEffect

class AnalyticsViewModel(application: Application) :
    BaseMviViewModel<AnalyticsUiState, AnalyticsAction, AnalyticsEffect>(application, AnalyticsUiState()) {

    init {
        repository.getActiveSubscriptions()
            .onEach { subscriptions ->
                val totalMonthly = subscriptions.sumOf { it.monthlyPrice }
                val totalYearly = totalMonthly * 12

                val categoryBreakdowns = subscriptions
                    .groupBy { it.category }
                    .map { (category, subs) ->
                        CategoryBreakdown(
                            category = category,
                            monthlyTotal = subs.sumOf { it.monthlyPrice },
                            count = subs.size
                        )
                    }
                    .sortedByDescending { it.monthlyTotal }

                setState(
                    AnalyticsUiState(
                        totalMonthly = totalMonthly,
                        totalYearly = totalYearly,
                        activeCount = subscriptions.size,
                        categoryBreakdowns = categoryBreakdowns,
                        subscriptions = subscriptions
                    )
                )
            }
            .launchIn(viewModelScope)
    }

    override suspend fun handleAction(action: AnalyticsAction) = Unit
}
