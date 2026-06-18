package com.example.subcan.data.backup

import com.example.subcan.data.db.Subscription
import com.example.subcan.data.model.BillingCycle
import com.example.subcan.data.model.BillingStartPolicy
import com.example.subcan.data.model.CancellationPolicy
import com.example.subcan.data.model.SubscriptionCategory
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupRepositoryTest {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }
    private val clock = Clock.fixed(Instant.parse("2026-06-18T01:23:45Z"), ZoneOffset.UTC)

    @Test
    fun `subscriptions can be exported and parsed again`() = runBlocking {
        val source = FakeBackupDataSource(mutableListOf(subscription()))
        val repository = repository(source)

        val exported = repository.exportSubscriptions().getOrThrow()
        val backup = json.decodeFromString<SubscriptionBackupFile>(exported)

        assertEquals(CURRENT_BACKUP_SCHEMA_VERSION, backup.schemaVersion)
        assertEquals("2026-06-18T01:23:45Z", backup.exportedAt)
        assertEquals("1.0-test", backup.appVersion)
        assertEquals(listOf(subscription().toBackupItem()), backup.subscriptions)
    }

    @Test
    fun `schema version 1 is accepted`() = runBlocking {
        val source = FakeBackupDataSource()
        val result = repository(source).importSubscriptions(
            backupJson(items = listOf(backupItem())),
            BackupImportStrategy.REPLACE_ALL
        )

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow().importedCount)
    }

    @Test
    fun `unknown schema version is rejected`() = runBlocking {
        val result = repository(FakeBackupDataSource()).importSubscriptions(
            backupJson(schemaVersion = 2, items = emptyList()),
            BackupImportStrategy.REPLACE_ALL
        )

        assertTrue(result.exceptionOrNull() is BackupException.UnsupportedSchemaVersion)
    }

    @Test
    fun `invalid json is rejected`() = runBlocking {
        val result = repository(FakeBackupDataSource()).importSubscriptions(
            "{not-json",
            BackupImportStrategy.REPLACE_ALL
        )

        assertTrue(result.exceptionOrNull() is BackupException.InvalidJson)
    }

    @Test
    fun `non numeric schema version is rejected as invalid content`() = runBlocking {
        val result = repository(FakeBackupDataSource()).importSubscriptions(
            """{"schemaVersion": {}, "exportedAt": "2026-06-18T01:23:45Z", "subscriptions": []}""",
            BackupImportStrategy.REPLACE_ALL
        )

        assertTrue(result.exceptionOrNull() is BackupException.InvalidBackupContent)
    }

    @Test
    fun `replace all import replaces existing subscriptions`() = runBlocking {
        val source = FakeBackupDataSource(mutableListOf(subscription(id = 99, name = "Old")))
        val replacement = backupItem(id = 7, name = "Restored")

        repository(source).importSubscriptions(
            backupJson(items = listOf(replacement)),
            BackupImportStrategy.REPLACE_ALL
        ).getOrThrow()

        assertEquals(listOf(replacement.toEntity()), source.subscriptions)
    }

    @Test
    fun `empty subscriptions replace existing data with an empty list`() = runBlocking {
        val source = FakeBackupDataSource(mutableListOf(subscription()))

        repository(source).importSubscriptions(
            backupJson(items = emptyList()),
            BackupImportStrategy.REPLACE_ALL
        ).getOrThrow()

        assertTrue(source.subscriptions.isEmpty())
    }

    @Test
    fun `nullable fields can be imported`() = runBlocking {
        val source = FakeBackupDataSource()
        val item = backupItem(canceledAt = null, freeTrialEndDate = null)

        repository(source).importSubscriptions(
            backupJson(items = listOf(item)),
            BackupImportStrategy.REPLACE_ALL
        ).getOrThrow()

        assertNull(source.subscriptions.single().canceledAt)
        assertNull(source.subscriptions.single().freeTrialEndDate)
    }

    @Test
    fun `unknown enum value is rejected without changing existing data`() = runBlocking {
        val existing = subscription(id = 42, name = "Existing")
        val source = FakeBackupDataSource(mutableListOf(existing))

        val result = repository(source).importSubscriptions(
            backupJson(items = listOf(backupItem(billingCycle = "UNKNOWN"))),
            BackupImportStrategy.REPLACE_ALL
        )

        assertTrue(result.exceptionOrNull() is BackupException.InvalidBackupContent)
        assertEquals(listOf(existing), source.subscriptions)
    }

    private fun repository(source: SubscriptionBackupDataSource) = DefaultBackupRepository(
        dataSource = source,
        appVersion = "1.0-test",
        clock = clock
    )

    private fun backupJson(
        schemaVersion: Int = CURRENT_BACKUP_SCHEMA_VERSION,
        items: List<SubscriptionBackupItem>
    ): String = json.encodeToString(
        SubscriptionBackupFile(
            schemaVersion = schemaVersion,
            exportedAt = "2026-06-18T01:23:45Z",
            appVersion = "1.0-test",
            subscriptions = items
        )
    )

    private fun subscription(id: Long = 1, name: String = "Example") = Subscription(
        id = id,
        name = name,
        description = "Description",
        price = 980,
        billingCycle = BillingCycle.MONTHLY,
        category = SubscriptionCategory.PRODUCTIVITY,
        startDate = LocalDate.parse("2026-01-02"),
        nextBillingDate = LocalDate.parse("2026-07-02"),
        isActive = true,
        autoRenew = true,
        canceledAt = null,
        billingStartPolicy = BillingStartPolicy.SIGNUP_DATE,
        cancellationPolicy = CancellationPolicy.END_OF_PERIOD,
        hasFreeTrial = true,
        freeTrialDays = 14,
        freeTrialEndDate = LocalDate.parse("2026-01-16"),
        reminderDaysBefore = 3,
        cancellationUrl = "https://example.com/cancel",
        notes = "Memo",
        createdAt = LocalDate.parse("2026-01-01")
    )

    private fun backupItem(
        id: Long = 1,
        name: String = "Example",
        billingCycle: String = "MONTHLY",
        canceledAt: String? = null,
        freeTrialEndDate: String? = null
    ) = SubscriptionBackupItem(
        id = id,
        name = name,
        description = "Description",
        price = 980,
        billingCycle = billingCycle,
        category = "PRODUCTIVITY",
        startDate = "2026-01-02",
        nextBillingDate = "2026-07-02",
        isActive = true,
        autoRenew = true,
        canceledAt = canceledAt,
        billingStartPolicy = "SIGNUP_DATE",
        cancellationPolicy = "END_OF_PERIOD",
        hasFreeTrial = false,
        freeTrialDays = 0,
        freeTrialEndDate = freeTrialEndDate,
        reminderDaysBefore = 3,
        cancellationUrl = "https://example.com/cancel",
        notes = "Memo",
        createdAt = "2026-01-01"
    )
}

private class FakeBackupDataSource(val subscriptions: MutableList<Subscription> = mutableListOf()) :
    SubscriptionBackupDataSource {
    override suspend fun getAllSubscriptions(): List<Subscription> = subscriptions.toList()

    override suspend fun replaceAll(subscriptions: List<Subscription>) {
        this.subscriptions.clear()
        this.subscriptions.addAll(subscriptions)
    }
}
