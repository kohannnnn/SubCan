package com.example.subcan.data.model

import com.example.subcan.data.db.Subscription
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubscriptionLifecycleManagerTest {

    @Test
    fun `due auto-renew subscription advances to next future billing date`() {
        val subscription = baseSubscription(
            nextBillingDate = LocalDate.of(2026, 5, 1),
            billingCycle = BillingCycle.MONTHLY,
            autoRenew = true,
            isActive = true
        )

        val updated = SubscriptionLifecycleManager.resolveDueSubscription(
            subscription = subscription,
            today = LocalDate.of(2026, 6, 6)
        )

        assertEquals(LocalDate.of(2026, 7, 1), updated?.nextBillingDate)
        assertTrue(updated?.isActive == true)
        assertTrue(updated?.autoRenew == true)
    }

    @Test
    fun `end of period cancellation stays active before billing date`() {
        val subscription = baseSubscription(
            nextBillingDate = LocalDate.of(2026, 6, 20),
            autoRenew = false,
            isActive = true,
            cancellationPolicy = CancellationPolicy.END_OF_PERIOD
        )

        val updated = SubscriptionLifecycleManager.resolveDueSubscription(
            subscription = subscription,
            today = LocalDate.of(2026, 6, 6)
        )

        assertEquals(null, updated)
    }

    @Test
    fun `end of period cancellation becomes inactive on billing date`() {
        val subscription = baseSubscription(
            nextBillingDate = LocalDate.of(2026, 6, 6),
            autoRenew = false,
            isActive = true,
            cancellationPolicy = CancellationPolicy.END_OF_PERIOD,
            canceledAt = LocalDate.of(2026, 6, 1)
        )

        val updated = SubscriptionLifecycleManager.resolveDueSubscription(
            subscription = subscription,
            today = LocalDate.of(2026, 6, 6)
        )

        assertTrue(updated != null)
        assertFalse(updated!!.isActive)
        assertFalse(updated.autoRenew)
        assertEquals(LocalDate.of(2026, 6, 1), updated.canceledAt)
    }

    @Test
    fun `cancel stores cancellation date for end of period subscriptions`() {
        val today = LocalDate.of(2026, 6, 6)
        val subscription = baseSubscription(
            nextBillingDate = LocalDate.of(2026, 6, 20),
            autoRenew = true,
            isActive = true,
            cancellationPolicy = CancellationPolicy.END_OF_PERIOD
        )

        val canceled = SubscriptionLifecycleManager.cancel(
            subscription = subscription,
            today = today
        )

        assertFalse(canceled.autoRenew)
        assertEquals(today, canceled.canceledAt)
    }

    @Test
    fun `resume re-enables auto renew and moves stale next billing date forward`() {
        val subscription = baseSubscription(
            nextBillingDate = LocalDate.of(2026, 5, 1),
            autoRenew = false,
            isActive = false
        )

        val resumed = SubscriptionLifecycleManager.resume(
            subscription = subscription,
            today = LocalDate.of(2026, 6, 6)
        )

        assertTrue(resumed.isActive)
        assertTrue(resumed.autoRenew)
        assertEquals(LocalDate.of(2026, 7, 1), resumed.nextBillingDate)
        assertEquals(null, resumed.canceledAt)
    }

    private fun baseSubscription(
        nextBillingDate: LocalDate,
        billingCycle: BillingCycle = BillingCycle.MONTHLY,
        autoRenew: Boolean = true,
        isActive: Boolean = true,
        cancellationPolicy: CancellationPolicy = CancellationPolicy.END_OF_PERIOD,
        canceledAt: LocalDate? = null
    ) = Subscription(
        id = 1,
        name = "Test Subscription",
        description = "",
        price = 1000,
        billingCycle = billingCycle,
        category = SubscriptionCategory.OTHER,
        startDate = LocalDate.of(2026, 4, 1),
        nextBillingDate = nextBillingDate,
        isActive = isActive,
        autoRenew = autoRenew,
        canceledAt = canceledAt,
        billingStartPolicy = BillingStartPolicy.SIGNUP_DATE,
        cancellationPolicy = cancellationPolicy,
        hasFreeTrial = false,
        freeTrialDays = 0,
        freeTrialEndDate = null,
        reminderDaysBefore = 3,
        notes = ""
    )
}
