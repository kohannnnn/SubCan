package com.example.subcan.data.model

import com.example.subcan.data.db.Subscription
import java.time.LocalDate

object SubscriptionLifecycleManager {

    fun calculateNextBillingDate(
        startDate: LocalDate,
        billingCycle: BillingCycle,
        referenceDate: LocalDate = LocalDate.now()
    ): LocalDate {
        var nextBillingDate = startDate
        while (!nextBillingDate.isAfter(referenceDate)) {
            nextBillingDate = advanceBillingDate(nextBillingDate, billingCycle)
        }
        return nextBillingDate
    }

    fun resolveDueSubscription(subscription: Subscription, today: LocalDate = LocalDate.now()): Subscription? {
        if (!subscription.isActive || subscription.nextBillingDate.isAfter(today)) {
            return null
        }

        return when {
            subscription.autoRenew -> subscription.copy(
                nextBillingDate = calculateNextBillingDate(
                    startDate = subscription.nextBillingDate,
                    billingCycle = subscription.billingCycle,
                    referenceDate = today
                )
            )

            subscription.cancellationPolicy == CancellationPolicy.END_OF_PERIOD -> {
                subscription.copy(isActive = false)
            }

            else -> null
        }
    }

    fun cancel(subscription: Subscription, today: LocalDate = LocalDate.now()): Subscription =
        when (subscription.cancellationPolicy) {
            CancellationPolicy.END_OF_PERIOD -> subscription.copy(
                autoRenew = false,
                canceledAt = today
            )

            CancellationPolicy.IMMEDIATE,
            CancellationPolicy.REFUNDABLE -> subscription.copy(
                isActive = false,
                autoRenew = false,
                canceledAt = today
            )
        }

    fun resume(subscription: Subscription, today: LocalDate = LocalDate.now()): Subscription {
        val nextBillingDate = if (subscription.nextBillingDate.isAfter(today)) {
            subscription.nextBillingDate
        } else {
            calculateNextBillingDate(
                startDate = subscription.nextBillingDate,
                billingCycle = subscription.billingCycle,
                referenceDate = today
            )
        }

        return subscription.copy(
            isActive = true,
            autoRenew = true,
            nextBillingDate = nextBillingDate,
            canceledAt = null
        )
    }

    private fun advanceBillingDate(date: LocalDate, billingCycle: BillingCycle): LocalDate = when (billingCycle) {
        BillingCycle.WEEKLY -> date.plusWeeks(1)
        BillingCycle.MONTHLY -> date.plusMonths(1)
        BillingCycle.QUARTERLY -> date.plusMonths(3)
        BillingCycle.SEMI_ANNUAL -> date.plusMonths(6)
        BillingCycle.ANNUAL -> date.plusYears(1)
    }
}
