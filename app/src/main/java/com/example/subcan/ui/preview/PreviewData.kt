package com.example.subcan.ui.preview

import com.example.subcan.data.db.Subscription
import com.example.subcan.data.model.BillingCycle
import com.example.subcan.data.model.BillingStartPolicy
import com.example.subcan.data.model.CancellationPolicy
import com.example.subcan.data.model.SubscriptionCategory
import java.time.LocalDate

/**
 * プレビュー用ダミーデータ
 */
object PreviewData {

    val sampleSubscriptions = listOf(
        Subscription(
            id = 1,
            name = "Netflix",
            price = 1590,
            billingCycle = BillingCycle.MONTHLY,
            category = SubscriptionCategory.VIDEO,
            startDate = LocalDate.of(2024, 1, 15),
            nextBillingDate = LocalDate.now().plusDays(5),
            cancellationPolicy = CancellationPolicy.END_OF_PERIOD,
            cancellationUrl = "https://www.netflix.com/cancelplan",
            reminderDaysBefore = 3
        ),
        Subscription(
            id = 2,
            name = "Spotify Premium",
            price = 980,
            billingCycle = BillingCycle.MONTHLY,
            category = SubscriptionCategory.MUSIC,
            startDate = LocalDate.of(2023, 6, 1),
            nextBillingDate = LocalDate.now().plusDays(12),
            cancellationPolicy = CancellationPolicy.END_OF_PERIOD,
            hasFreeTrial = true,
            freeTrialDays = 30,
            freeTrialEndDate = LocalDate.of(2023, 7, 1)
        ),
        Subscription(
            id = 3,
            name = "Adobe Creative Cloud",
            price = 7780,
            billingCycle = BillingCycle.MONTHLY,
            category = SubscriptionCategory.PRODUCTIVITY,
            startDate = LocalDate.of(2024, 3, 10),
            nextBillingDate = LocalDate.now().plusDays(20),
            cancellationPolicy = CancellationPolicy.IMMEDIATE,
            billingStartPolicy = BillingStartPolicy.SIGNUP_DATE,
            cancellationUrl = "https://account.adobe.com/plans",
            notes = "年間契約のため途中解約注意"
        ),
        Subscription(
            id = 4,
            name = "U-NEXT",
            price = 2189,
            billingCycle = BillingCycle.MONTHLY,
            category = SubscriptionCategory.VIDEO,
            startDate = LocalDate.of(2024, 5, 1),
            nextBillingDate = LocalDate.now().plusDays(2),
            billingStartPolicy = BillingStartPolicy.FIRST_OF_MONTH,
            cancellationPolicy = CancellationPolicy.END_OF_PERIOD
        ),
        Subscription(
            id = 5,
            name = "Nintendo Switch Online",
            price = 2400,
            billingCycle = BillingCycle.ANNUAL,
            category = SubscriptionCategory.GAMING,
            startDate = LocalDate.of(2024, 8, 1),
            nextBillingDate = LocalDate.now().plusDays(90),
            cancellationPolicy = CancellationPolicy.END_OF_PERIOD
        )
    )

    val singleSubscription = sampleSubscriptions[0]
}
