package com.example.subcan.ui.calendar

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.example.subcan.data.db.Subscription
import com.example.subcan.data.model.BillingOccurrence
import com.example.subcan.data.model.BillingScheduleCalculator
import com.example.subcan.ui.mvi.BaseMviViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

enum class CalendarViewMode(val label: String) {
    MONTH("月表示"),
    WEEK("週表示")
}

data class CalendarDayUiModel(
    val date: LocalDate,
    val occurrences: List<BillingOccurrence> = emptyList(),
    val isCurrentMonth: Boolean = true
) {
    val totalAmount: Int
        get() = occurrences.sumOf { it.price }
}

data class CalendarSummaryUiModel(
    val scheduledCount: Int = 0,
    val totalAmount: Int = 0,
    val peakDays: List<LocalDate> = emptyList(),
    val peakAmount: Int = 0
)

data class CalendarUiState(
    val allSubscriptions: List<Subscription> = emptyList(),
    val viewMode: CalendarViewMode = CalendarViewMode.MONTH,
    val anchorDate: LocalDate = LocalDate.now(),
    val periodTitle: String = "",
    val periodStart: LocalDate = LocalDate.now(),
    val periodEnd: LocalDate = LocalDate.now(),
    val monthGrid: List<List<CalendarDayUiModel>> = emptyList(),
    val periodDays: List<CalendarDayUiModel> = emptyList(),
    val summary: CalendarSummaryUiModel = CalendarSummaryUiModel()
) {
    val hasAnySubscriptions: Boolean
        get() = allSubscriptions.isNotEmpty()

    val hasScheduledCharges: Boolean
        get() = summary.scheduledCount > 0
}

sealed interface CalendarAction {
    data class ViewModeSelected(val mode: CalendarViewMode) : CalendarAction
    data object PreviousPeriodClick : CalendarAction
    data object NextPeriodClick : CalendarAction
    data object TodayClick : CalendarAction
    data class DaySelected(val date: LocalDate) : CalendarAction
    data class SubscriptionClick(val subscriptionId: Long) : CalendarAction
}

sealed interface CalendarEffect {
    data class NavigateToDetail(val subscriptionId: Long) : CalendarEffect
}

class CalendarViewModel(application: Application) :
    BaseMviViewModel<CalendarUiState, CalendarAction, CalendarEffect>(application, CalendarUiState()) {

    init {
        repository.getAllSubscriptions()
            .onEach { subscriptions ->
                rebuildState(subscriptions = subscriptions)
            }
            .launchIn(viewModelScope)
    }

    override suspend fun handleAction(action: CalendarAction) {
        when (action) {
            is CalendarAction.ViewModeSelected -> {
                rebuildState(viewMode = action.mode)
            }

            CalendarAction.PreviousPeriodClick -> {
                val current = uiState.value
                val nextAnchor = when (current.viewMode) {
                    CalendarViewMode.MONTH -> current.anchorDate.minusMonths(1)
                    CalendarViewMode.WEEK -> current.anchorDate.minusWeeks(1)
                }
                rebuildState(anchorDate = nextAnchor)
            }

            CalendarAction.NextPeriodClick -> {
                val current = uiState.value
                val nextAnchor = when (current.viewMode) {
                    CalendarViewMode.MONTH -> current.anchorDate.plusMonths(1)
                    CalendarViewMode.WEEK -> current.anchorDate.plusWeeks(1)
                }
                rebuildState(anchorDate = nextAnchor)
            }

            CalendarAction.TodayClick -> rebuildState(anchorDate = LocalDate.now())

            is CalendarAction.DaySelected -> rebuildState(
                viewMode = CalendarViewMode.WEEK,
                anchorDate = action.date
            )

            is CalendarAction.SubscriptionClick -> {
                emitEffect(CalendarEffect.NavigateToDetail(action.subscriptionId))
            }
        }
    }

    private fun rebuildState(
        subscriptions: List<Subscription> = uiState.value.allSubscriptions,
        viewMode: CalendarViewMode = uiState.value.viewMode,
        anchorDate: LocalDate = uiState.value.anchorDate
    ) {
        val periodStart = when (viewMode) {
            CalendarViewMode.MONTH -> anchorDate.withDayOfMonth(1)
            CalendarViewMode.WEEK -> anchorDate.startOfWeek()
        }
        val periodEnd = when (viewMode) {
            CalendarViewMode.MONTH -> anchorDate.withDayOfMonth(anchorDate.lengthOfMonth())
            CalendarViewMode.WEEK -> anchorDate.endOfWeek()
        }
        val gridStart = if (viewMode == CalendarViewMode.MONTH) periodStart.startOfWeek() else periodStart
        val gridEnd = if (viewMode == CalendarViewMode.MONTH) periodEnd.endOfWeek() else periodEnd

        val occurrencesByDate = BillingScheduleCalculator.occurrencesInRange(
            subscriptions = subscriptions,
            startDate = gridStart,
            endDate = gridEnd
        ).groupBy { it.date }

        val periodDays = datesInRange(periodStart, periodEnd).map { date ->
            CalendarDayUiModel(
                date = date,
                occurrences = occurrencesByDate[date].orEmpty(),
                isCurrentMonth = true
            )
        }

        val monthGrid = if (viewMode == CalendarViewMode.MONTH) {
            datesInRange(gridStart, gridEnd)
                .map { date ->
                    CalendarDayUiModel(
                        date = date,
                        occurrences = occurrencesByDate[date].orEmpty(),
                        isCurrentMonth = date.month == anchorDate.month && date.year == anchorDate.year
                    )
                }
                .chunked(7)
        } else {
            emptyList()
        }

        setState(
            CalendarUiState(
                allSubscriptions = subscriptions,
                viewMode = viewMode,
                anchorDate = anchorDate,
                periodTitle = buildPeriodTitle(viewMode, periodStart, periodEnd),
                periodStart = periodStart,
                periodEnd = periodEnd,
                monthGrid = monthGrid,
                periodDays = periodDays,
                summary = buildSummary(periodDays)
            )
        )
    }

    private fun buildSummary(periodDays: List<CalendarDayUiModel>): CalendarSummaryUiModel {
        val daysWithCharges = periodDays.filter { it.occurrences.isNotEmpty() }
        val peakAmount = daysWithCharges.maxOfOrNull { it.totalAmount } ?: 0
        val peakDays = if (peakAmount == 0) {
            emptyList()
        } else {
            daysWithCharges.filter { it.totalAmount == peakAmount }.map { it.date }
        }

        return CalendarSummaryUiModel(
            scheduledCount = periodDays.sumOf { it.occurrences.size },
            totalAmount = periodDays.sumOf { it.totalAmount },
            peakDays = peakDays,
            peakAmount = peakAmount
        )
    }

    private fun buildPeriodTitle(viewMode: CalendarViewMode, periodStart: LocalDate, periodEnd: LocalDate): String =
        when (viewMode) {
            CalendarViewMode.MONTH -> periodStart.format(
                DateTimeFormatter.ofPattern("yyyy年M月", Locale.JAPAN)
            )

            CalendarViewMode.WEEK -> {
                val startText = periodStart.format(
                    DateTimeFormatter.ofPattern("M月d日", Locale.JAPAN)
                )
                val endText = periodEnd.format(
                    DateTimeFormatter.ofPattern("M月d日", Locale.JAPAN)
                )
                "${periodStart.year}年 $startText - $endText"
            }
        }

    private fun datesInRange(start: LocalDate, end: LocalDate): List<LocalDate> {
        val dates = mutableListOf<LocalDate>()
        var current = start
        while (!current.isAfter(end)) {
            dates += current
            current = current.plusDays(1)
        }
        return dates
    }

    private fun LocalDate.startOfWeek(): LocalDate {
        val daysFromSunday = dayOfWeek.value % 7
        return minusDays(daysFromSunday.toLong())
    }

    private fun LocalDate.endOfWeek(): LocalDate {
        val daysUntilSaturday = (6 - (dayOfWeek.value % 7)).toLong()
        return plusDays(daysUntilSaturday)
    }
}
