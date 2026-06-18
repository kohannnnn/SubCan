package com.example.subcan.data.backup

import com.example.subcan.data.db.Subscription
import com.example.subcan.data.model.BillingCycle
import com.example.subcan.data.model.BillingStartPolicy
import com.example.subcan.data.model.CancellationPolicy
import com.example.subcan.data.model.SubscriptionCategory
import java.time.LocalDate
import java.time.format.DateTimeParseException

internal fun Subscription.toBackupItem(): SubscriptionBackupItem = SubscriptionBackupItem(
    id = id,
    name = name,
    description = description,
    price = price,
    billingCycle = billingCycle.name,
    category = category.name,
    startDate = startDate.toString(),
    nextBillingDate = nextBillingDate.toString(),
    isActive = isActive,
    autoRenew = autoRenew,
    canceledAt = canceledAt?.toString(),
    billingStartPolicy = billingStartPolicy.name,
    cancellationPolicy = cancellationPolicy.name,
    hasFreeTrial = hasFreeTrial,
    freeTrialDays = freeTrialDays,
    freeTrialEndDate = freeTrialEndDate?.toString(),
    reminderDaysBefore = reminderDaysBefore,
    cancellationUrl = cancellationUrl,
    notes = notes,
    createdAt = createdAt.toString()
)

internal fun SubscriptionBackupItem.toEntity(): Subscription {
    if (id <= 0) invalidContent("id must be positive")
    if (name.isBlank()) invalidContent("name must not be blank")
    if (price < 0) invalidContent("price must not be negative")
    if (currency != "JPY") invalidContent("currency must be JPY")
    if (freeTrialDays < 0) invalidContent("freeTrialDays must not be negative")
    if (reminderDaysBefore < 0) invalidContent("reminderDaysBefore must not be negative")

    return Subscription(
        id = id,
        name = name,
        description = description,
        price = price,
        billingCycle = enumValue<BillingCycle>(billingCycle, "billingCycle"),
        category = enumValue<SubscriptionCategory>(category, "category"),
        startDate = localDate(startDate, "startDate"),
        nextBillingDate = localDate(nextBillingDate, "nextBillingDate"),
        isActive = isActive,
        autoRenew = autoRenew,
        canceledAt = canceledAt?.let { localDate(it, "canceledAt") },
        billingStartPolicy = enumValue<BillingStartPolicy>(billingStartPolicy, "billingStartPolicy"),
        cancellationPolicy = enumValue<CancellationPolicy>(cancellationPolicy, "cancellationPolicy"),
        hasFreeTrial = hasFreeTrial,
        freeTrialDays = freeTrialDays,
        freeTrialEndDate = freeTrialEndDate?.let { localDate(it, "freeTrialEndDate") },
        reminderDaysBefore = reminderDaysBefore,
        cancellationUrl = cancellationUrl,
        notes = notes,
        createdAt = localDate(createdAt, "createdAt")
    )
}

private inline fun <reified T : Enum<T>> enumValue(value: String, field: String): T =
    enumValues<T>().firstOrNull { it.name == value }
        ?: invalidContent("$field has an unknown value")

private fun localDate(value: String, field: String): LocalDate = try {
    LocalDate.parse(value)
} catch (exception: DateTimeParseException) {
    throw BackupException.InvalidBackupContent("$field is not an ISO-8601 date", exception)
}

private fun invalidContent(reason: String): Nothing = throw BackupException.InvalidBackupContent(reason)
