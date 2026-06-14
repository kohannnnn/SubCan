package com.example.subcan.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Subscription::class, SubscriptionArchive::class],
    version = 4,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun subscriptionArchiveDao(): SubscriptionArchiveDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        private val migration1To2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE subscriptions ADD COLUMN autoRenew INTEGER NOT NULL DEFAULT 1"
                )
            }
        }

        private val migration2To3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE subscriptions ADD COLUMN cancellationUrl TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        private val migration3To4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE subscriptions ADD COLUMN canceledAt INTEGER"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `subscription_archives` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `originalSubscriptionId` INTEGER,
                        `name` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `price` INTEGER NOT NULL,
                        `billingCycle` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `startDate` INTEGER NOT NULL,
                        `canceledAt` INTEGER NOT NULL,
                        `finalUsageDate` INTEGER NOT NULL,
                        `billingStartPolicy` TEXT NOT NULL,
                        `cancellationPolicy` TEXT NOT NULL,
                        `hasFreeTrial` INTEGER NOT NULL,
                        `freeTrialDays` INTEGER NOT NULL,
                        `freeTrialEndDate` INTEGER,
                        `cancellationUrl` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `archivedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase = instance ?: synchronized(this) {
            val database = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "subcan_database"
            )
                .addMigrations(migration1To2, migration2To3, migration3To4)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
            instance = database
            database
        }
    }
}
