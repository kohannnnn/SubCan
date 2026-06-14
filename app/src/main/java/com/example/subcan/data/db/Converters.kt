package com.example.subcan.data.db

import androidx.room.TypeConverter
import com.example.subcan.data.model.BillingCycle
import com.example.subcan.data.model.BillingStartPolicy
import com.example.subcan.data.model.CancellationPolicy
import com.example.subcan.data.model.SubscriptionCategory
import java.time.LocalDate

class Converters {
    // LocalDate
    @TypeConverter
    fun fromLocalDate(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun toLocalDate(epochDay: Long?): LocalDate? = epochDay?.let { LocalDate.ofEpochDay(it) }

    // BillingCycle
    @TypeConverter
    fun fromBillingCycle(value: BillingCycle): String = value.name

    @TypeConverter
    fun toBillingCycle(value: String): BillingCycle = BillingCycle.valueOf(value)

    // BillingStartPolicy
    @TypeConverter
    fun fromBillingStartPolicy(value: BillingStartPolicy): String = value.name

    @TypeConverter
    fun toBillingStartPolicy(value: String): BillingStartPolicy = BillingStartPolicy.valueOf(value)

    // CancellationPolicy
    @TypeConverter
    fun fromCancellationPolicy(value: CancellationPolicy): String = value.name

    @TypeConverter
    fun toCancellationPolicy(value: String): CancellationPolicy = CancellationPolicy.valueOf(value)

    // SubscriptionCategory
    @TypeConverter
    fun fromCategory(value: SubscriptionCategory): String = value.name

    @TypeConverter
    fun toCategory(value: String): SubscriptionCategory = SubscriptionCategory.valueOf(value)
}
