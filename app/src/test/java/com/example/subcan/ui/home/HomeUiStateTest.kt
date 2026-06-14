package com.example.subcan.ui.home

import com.example.subcan.data.db.Subscription
import com.example.subcan.data.model.BillingCycle
import com.example.subcan.data.model.SubscriptionCategory
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeUiStateTest {

    private val active = subscription(id = 1, isActive = true, autoRenew = true)
    private val ending = subscription(id = 2, isActive = true, autoRenew = false)
    private val inactive = subscription(id = 3, isActive = false, autoRenew = false)
    private val subscriptions = listOf(active, ending, inactive)

    @Test
    fun allFilterReturnsEverySubscription() {
        val state = HomeUiState(subscriptions, HomeFilter.ALL)

        assertEquals(subscriptions, state.subscriptions)
    }

    @Test
    fun activeFilterReturnsActiveAutoRenewingSubscriptions() {
        val state = HomeUiState(subscriptions, HomeFilter.ACTIVE)

        assertEquals(listOf(active), state.subscriptions)
    }

    @Test
    fun endingFilterReturnsActiveNonRenewingSubscriptions() {
        val state = HomeUiState(subscriptions, HomeFilter.ENDING)

        assertEquals(listOf(ending), state.subscriptions)
    }

    @Test
    fun inactiveFilterReturnsStoppedSubscriptions() {
        val state = HomeUiState(subscriptions, HomeFilter.INACTIVE)

        assertEquals(listOf(inactive), state.subscriptions)
    }

    private fun subscription(id: Long, isActive: Boolean, autoRenew: Boolean) = Subscription(
        id = id,
        name = "Subscription $id",
        price = 1_000,
        billingCycle = BillingCycle.MONTHLY,
        category = SubscriptionCategory.OTHER,
        startDate = LocalDate.of(2026, 1, 1),
        nextBillingDate = LocalDate.of(2026, 7, 1),
        isActive = isActive,
        autoRenew = autoRenew
    )
}
