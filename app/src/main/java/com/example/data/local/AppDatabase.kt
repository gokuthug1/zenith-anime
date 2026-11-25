package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.Anime
import com.example.data.model.AnimeWatchStats
import com.example.data.model.AppNotification
import com.example.data.model.DailyUsageStats
import com.example.data.model.DownloadItem
import com.example.data.model.Episode
import com.example.data.model.WatchProgress

@Database(
    entities = [
        Anime::class,
        Episode::class,
        WatchProgress::class,
        DownloadItem::class,
        AppNotification::class,
        DailyUsageStats::class,
        AnimeWatchStats::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun animeDao(): AnimeDao
    abstract fun watchProgressDao(): WatchProgressDao
    abstract fun downloadDao(): DownloadDao
    abstract fun notificationDao(): NotificationDao
    abstract fun usageStatsDao(): UsageStatsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE anime ADD COLUMN englishTitle TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE anime ADD COLUMN romajiTitle TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notifications ADD COLUMN isUnread INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `daily_usage_stats` (
                        `dateKey` TEXT NOT NULL,
                        `appTimeSec` INTEGER NOT NULL,
                        `watchTimeSec` INTEGER NOT NULL,
                        `episodesCompleted` INTEGER NOT NULL,
                        `sessionsCount` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`dateKey`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `anime_watch_stats` (
                        `animeId` TEXT NOT NULL,
                        `animeTitle` TEXT NOT NULL,
                        `coverImage` TEXT NOT NULL,
                        `genres` TEXT NOT NULL,
                        `watchTimeSec` INTEGER NOT NULL,
                        `episodesWatchedCount` INTEGER NOT NULL,
                        `lastWatchedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`animeId`)
                    )
                    """.trimIndent()
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "zenith_anime_database"
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
