package com.example.subcan.data.db

import com.example.subcan.data.model.BillingCycle
import com.example.subcan.data.model.BillingStartPolicy
import com.example.subcan.data.model.CancellationPolicy
import com.example.subcan.data.model.SubscriptionCategory
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class SubscriptionArchiveTest {

    @Test
    fun fromSubscriptionKeepsScheduledEndDateForEndOfPeriodCancellation() {
        val archivedAt = LocalDate.of(2026, 6, 10)
        val subscription = baseSubscription(
            nextBillingDate = LocalDate.of(2026, 6, 20),
            autoRenew = false,
            cancellationPolicy = CancellationPolicy.END_OF_PERIOD,
            canceledAt = LocalDate.of(2026, 6, 1)
        )

        val archive = SubscriptionArchive.fromSubscription(subscription, archivedAt)

        assertEquals(LocalDate.of(2026, 6, 1), archive.canceledAt)
        assertEquals(LocalDate.of(2026, 6, 20), archive.finalUsageDate)
        assertEquals(archive.monthlySavings * 12, archive.yearlySavings, 0.0)
    }

    @Test
    fun fromSubscriptionFallsBackToArchiveDateWhenCancellationDateIsUnknown() {
        val archivedAt = LocalDate.of(2026, 6, 10)
        val subscription = baseSubscription(
            nextBillingDate = LocalDate.of(2026, 7, 1),
            autoRenew = true,
            cancellationPolicy = CancellationPolicy.IMMEDIATE,
            canceledAt = null
        )

        val archive = SubscriptionArchive.fromSubscription(subscription, archivedAt)

        assertEquals(archivedAt, archive.canceledAt)
        assertEquals(archivedAt, archive.finalUsageDate)
    }

    private fun baseSubscription(
        nextBillingDate: LocalDate,
        autoRenew: Boolean,
        cancellationPolicy: CancellationPolicy,
        canceledAt: LocalDate?
    ) = Subscription(
        id = 7,
        name = "Sample",
        description = "desc",
        price = 1800,
        billingCycle = BillingCycle.MONTHLY,
        category = SubscriptionCategory.OTHER,
        startDate = LocalDate.of(2026, 1, 1),
        nextBillingDate = nextBillingDate,
        isActive = true,
        autoRenew = autoRenew,
        canceledAt = canceledAt,
        billingStartPolicy = BillingStartPolicy.SIGNUP_DATE,
        cancellationPolicy = cancellationPolicy
    )
}
