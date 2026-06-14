package com.example.subcan.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.subcan.data.model.BillingCycle
import com.example.subcan.data.model.BillingStartPolicy
import com.example.subcan.data.model.CancellationPolicy
import com.example.subcan.data.model.SubscriptionCategory
import java.time.LocalDate

@Entity(tableName = "subscriptions")
data class Subscription(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val price: Int, // 円単位
    val billingCycle: BillingCycle,
    val category: SubscriptionCategory,
    val startDate: LocalDate,
    val nextBillingDate: LocalDate,
    val isActive: Boolean = true,
    val autoRenew: Boolean = true,
    val canceledAt: LocalDate? = null,
    val billingStartPolicy: BillingStartPolicy = BillingStartPolicy.SIGNUP_DATE,
    val cancellationPolicy: CancellationPolicy = CancellationPolicy.END_OF_PERIOD,
    val hasFreeTrial: Boolean = false,
    val freeTrialDays: Int = 0,
    val freeTrialEndDate: LocalDate? = null,
    val reminderDaysBefore: Int = 3,
    val cancellationUrl: String = "",
    val notes: String = "",
    val createdAt: LocalDate = LocalDate.now()
) {
    /** 月額換算の金額 */
    val monthlyPrice: Double
        get() = price * billingCycle.toMonthlyFactor()

    /** 年額換算の金額 */
    val yearlyPrice: Double
        get() = monthlyPrice * 12
}
