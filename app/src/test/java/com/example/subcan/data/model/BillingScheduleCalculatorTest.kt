package com.example.subcan.data.model

import com.example.subcan.data.db.Subscription
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class BillingScheduleCalculatorTest {

    @Test
    fun generatesRepeatedOccurrencesInsideRange() {
        val subscription = subscription(
            id = 1,
            nextBillingDate = LocalDate.of(2026, 6, 15)
        )

        val occurrences = BillingScheduleCalculator.occurrencesInRange(
            subscriptions = listOf(subscription),
            startDate = LocalDate.of(2026, 6, 1),
            endDate = LocalDate.of(2026, 8, 31)
        )

        assertEquals(
            listOf(
                LocalDate.of(2026, 6, 15),
                LocalDate.of(2026, 7, 15),
                LocalDate.of(2026, 8, 15)
            ),
            occurrences.map { it.date }
        )
    }

    @Test
    fun skipsPastOccurrencesAndStartsFromFirstDateInRange() {
        val subscription = subscription(
            id = 1,
            nextBillingDate = LocalDate.of(2026, 1, 15)
        )

        val occurrences = BillingScheduleCalculator.occurrencesInRange(
            subscriptions = listOf(subscription),
            startDate = LocalDate.of(2026, 6, 1),
            endDate = LocalDate.of(2026, 6, 30)
        )

        assertEquals(
            listOf(LocalDate.of(2026, 6, 15)),
            occurrences.map { it.date }
        )
    }

    @Test
    fun excludesInactiveAndCancelledSubscriptions() {
        val active = subscription(
            id = 1,
            nextBillingDate = LocalDate.of(2026, 6, 15),
            isActive = true,
            autoRenew = true
        )
        val ending = subscription(
            id = 2,
            nextBillingDate = LocalDate.of(2026, 6, 20),
            isActive = true,
            autoRenew = false
        )
        val inactive = subscription(
            id = 3,
            nextBillingDate = LocalDate.of(2026, 6, 25),
            isActive = false,
            autoRenew = true
        )

        val occurrences = BillingScheduleCalculator.occurrencesInRange(
            subscriptions = listOf(active, ending, inactive),
            startDate = LocalDate.of(2026, 6, 1),
            endDate = LocalDate.of(2026, 6, 30)
        )

        assertEquals(listOf(1L), occurrences.map { it.subscriptionId })
        assertEquals(listOf(LocalDate.of(2026, 6, 15)), occurrences.map { it.date })
    }

    private fun subscription(
        id: Long,
        nextBillingDate: LocalDate,
        isActive: Boolean = true,
        autoRenew: Boolean = true
    ) = Subscription(
        id = id,
        name = "Subscription $id",
        price = 1_200,
        billingCycle = BillingCycle.MONTHLY,
        category = SubscriptionCategory.OTHER,
        startDate = LocalDate.of(2026, 1, 1),
        nextBillingDate = nextBillingDate,
        isActive = isActive,
        autoRenew = autoRenew
    )
}
