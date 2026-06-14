package com.example.subcan.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.subcan.data.model.BillingCycle
import com.example.subcan.data.model.BillingStartPolicy
import com.example.subcan.data.model.CancellationPolicy
import com.example.subcan.data.model.SubscriptionCategory
import java.time.LocalDate

@Entity(tableName = "subscription_archives")
data class SubscriptionArchive(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val originalSubscriptionId: Long? = null,
    val name: String,
    val description: String = "",
    val price: Int,
    val billingCycle: BillingCycle,
    val category: SubscriptionCategory,
    val startDate: LocalDate,
    val canceledAt: LocalDate,
    val finalUsageDate: LocalDate,
    val billingStartPolicy: BillingStartPolicy = BillingStartPolicy.SIGNUP_DATE,
    val cancellationPolicy: CancellationPolicy = CancellationPolicy.END_OF_PERIOD,
    val hasFreeTrial: Boolean = false,
    val freeTrialDays: Int = 0,
    val freeTrialEndDate: LocalDate? = null,
    val cancellationUrl: String = "",
    val notes: String = "",
    val createdAt: LocalDate = LocalDate.now(),
    val archivedAt: LocalDate = LocalDate.now()
) {
    val monthlySavings: Double
        get() = price * billingCycle.toMonthlyFactor()

    val yearlySavings: Double
        get() = monthlySavings * 12

    companion object {
        fun fromSubscription(subscription: Subscription, archivedAt: LocalDate = LocalDate.now()): SubscriptionArchive {
            val canceledAt = subscription.canceledAt ?: archivedAt
            val finalUsageDate = if (
                !subscription.autoRenew &&
                subscription.cancellationPolicy == CancellationPolicy.END_OF_PERIOD
            ) {
                subscription.nextBillingDate
            } else {
                canceledAt
            }

            return SubscriptionArchive(
                originalSubscriptionId = subscription.id,
                name = subscription.name,
                description = subscription.description,
                price = subscription.price,
                billingCycle = subscription.billingCycle,
                category = subscription.category,
                startDate = subscription.startDate,
                canceledAt = canceledAt,
                finalUsageDate = finalUsageDate,
                billingStartPolicy = subscription.billingStartPolicy,
                cancellationPolicy = subscription.cancellationPolicy,
                hasFreeTrial = subscription.hasFreeTrial,
                freeTrialDays = subscription.freeTrialDays,
                freeTrialEndDate = subscription.freeTrialEndDate,
                cancellationUrl = subscription.cancellationUrl,
                notes = subscription.notes,
                createdAt = subscription.createdAt,
                archivedAt = archivedAt
            )
        }
    }
}
