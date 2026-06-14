package com.example.subcan.data.model

import com.example.subcan.data.db.Subscription
import java.time.LocalDate

data class BillingOccurrence(
    val subscriptionId: Long,
    val subscriptionName: String,
    val date: LocalDate,
    val price: Int,
    val category: SubscriptionCategory,
    val billingCycle: BillingCycle
)

object BillingScheduleCalculator {

    fun occurrencesInRange(
        subscriptions: List<Subscription>,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<BillingOccurrence> {
        require(!endDate.isBefore(startDate)) {
            "endDate must not be before startDate"
        }

        return subscriptions.asSequence()
            .filter { it.isActive && it.autoRenew }
            .flatMap { occurrencesForSubscription(it, startDate, endDate).asSequence() }
            .sortedWith(
                compareBy<BillingOccurrence> { it.date }
                    .thenByDescending { it.price }
                    .thenBy { it.subscriptionName }
            )
            .toList()
    }

    private fun occurrencesForSubscription(
        subscription: Subscription,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<BillingOccurrence> {
        var billingDate = subscription.nextBillingDate
        if (billingDate.isBefore(startDate)) {
            billingDate = SubscriptionLifecycleManager.calculateNextBillingDate(
                startDate = billingDate,
                billingCycle = subscription.billingCycle,
                referenceDate = startDate.minusDays(1)
            )
        }

        val occurrences = mutableListOf<BillingOccurrence>()
        while (!billingDate.isAfter(endDate)) {
            occurrences += BillingOccurrence(
                subscriptionId = subscription.id,
                subscriptionName = subscription.name,
                date = billingDate,
                price = subscription.price,
                category = subscription.category,
                billingCycle = subscription.billingCycle
            )
            billingDate = advanceBillingDate(billingDate, subscription.billingCycle)
        }

        return occurrences
    }

    private fun advanceBillingDate(date: LocalDate, billingCycle: BillingCycle): LocalDate = when (billingCycle) {
        BillingCycle.WEEKLY -> date.plusWeeks(1)
        BillingCycle.MONTHLY -> date.plusMonths(1)
        BillingCycle.QUARTERLY -> date.plusMonths(3)
        BillingCycle.SEMI_ANNUAL -> date.plusMonths(6)
        BillingCycle.ANNUAL -> date.plusYears(1)
    }
}
