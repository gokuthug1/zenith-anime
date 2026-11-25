package com.example.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Anime
import com.example.data.model.AnimeWatchStats
import com.example.data.model.DailyUsageStats
import com.example.data.model.Episode
import com.example.data.model.UsageStatsCalculator
import com.example.data.model.UsageStatsRange
import com.example.data.repository.AnimeRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class UsageStatsTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase("zenith_anime_database")
        context.getSharedPreferences("zenith_anime_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun migration5To6PreservesExistingTablesAndCreatesUsageStatsTables(): Unit = runBlocking {
        val dbName = "migration_5_6_test.db"
        context.deleteDatabase(dbName)
        val dbFile = context.getDatabasePath(dbName)
        dbFile.parentFile?.mkdirs()

        // Create an authentic Room v5 SQLite schema with existing rows
        val sqlite = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        sqlite.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `anime` (
                `id` TEXT NOT NULL,
                `malId` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `englishTitle` TEXT NOT NULL,
                `romajiTitle` TEXT NOT NULL,
                `nativeTitle` TEXT NOT NULL,
                `coverImage` TEXT NOT NULL,
                `bannerImage` TEXT NOT NULL,
                `synopsis` TEXT NOT NULL,
                `rating` REAL NOT NULL,
                `ageRating` TEXT NOT NULL,
                `quality` TEXT NOT NULL,
                `year` INTEGER NOT NULL,
                `season` TEXT NOT NULL,
                `status` TEXT NOT NULL,
                `type` TEXT NOT NULL,
                `totalEpisodes` INTEGER NOT NULL,
                `genres` TEXT NOT NULL,
                `studio` TEXT NOT NULL,
                `scheduleDay` TEXT NOT NULL,
                `scheduleTime` TEXT NOT NULL,
                `isFavorite` INTEGER NOT NULL,
                `isInWatchlist` INTEGER NOT NULL,
                `isCompleted` INTEGER NOT NULL,
                `nextAiringEpisode` INTEGER,
                `nextAiringTimeUntilSec` INTEGER,
                `nextAiringAtTimestamp` INTEGER,
                `watchStatus` TEXT NOT NULL,
                `lastUpdated` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
        sqlite.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `episodes` (
                `id` TEXT NOT NULL,
                `animeId` TEXT NOT NULL,
                `number` INTEGER NOT NULL,
                `title` TEXT NOT NULL,
                `thumbnail` TEXT NOT NULL,
                `durationSec` INTEGER NOT NULL,
                `hasSub` INTEGER NOT NULL,
                `hasDub` INTEGER NOT NULL,
                `isOffline` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
        sqlite.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `watch_progress` (
                `id` TEXT NOT NULL,
                `animeId` TEXT NOT NULL,
                `episodeNumber` INTEGER NOT NULL,
                `episodeTitle` TEXT NOT NULL,
                `animeTitle` TEXT NOT NULL,
                `coverImage` TEXT NOT NULL,
                `currentTimeSec` INTEGER NOT NULL,
                `durationSec` INTEGER NOT NULL,
                `language` TEXT NOT NULL,
                `completed` INTEGER NOT NULL,
                `lastWatchedAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
        sqlite.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `downloads` (
                `id` TEXT NOT NULL,
                `animeId` TEXT NOT NULL,
                `animeTitle` TEXT NOT NULL,
                `episodeNumber` INTEGER NOT NULL,
                `episodeTitle` TEXT NOT NULL,
                `thumbnail` TEXT NOT NULL,
                `quality` TEXT NOT NULL,
                `fileSize` TEXT NOT NULL,
                `downloadedAt` INTEGER NOT NULL,
                `filePath` TEXT NOT NULL,
                `status` TEXT NOT NULL,
                `progress` INTEGER NOT NULL,
                `streamUrl` TEXT NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
        sqlite.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `notifications` (
                `id` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `message` TEXT NOT NULL,
                `timestamp` TEXT NOT NULL,
                `animeThumbnail` TEXT NOT NULL,
                `tag` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `isUnread` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
        sqlite.execSQL(
            """
            INSERT INTO `anime` VALUES (
                '21', '21', 'One Piece', 'One Piece', 'One Piece', 'ONE PIECE',
                'https://img/cover.jpg', '', 'Pirates', 8.7, 'PG-13', 'HD', 1999, 'Fall 1999',
                'RELEASING', 'TV', 1100, 'Action, Adventure', 'Toei', 'Sun', '09:30',
                1, 1, 0, 1101, NULL, NULL, 'WATCHING', 1700000000000
            )
            """.trimIndent()
        )
        sqlite.version = 5
        sqlite.close()

        // Open with Room v6 + MIGRATION_5_6 (WITHOUT fallbackToDestructiveMigration)
        val migratedDb = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5, AppDatabase.MIGRATION_5_6)
            .allowMainThreadQueries()
            .build()

        val preservedAnime = migratedDb.animeDao().getAnimeByIdOnce("21")
        assertNotNull(preservedAnime)
        assertEquals("One Piece", preservedAnime?.title)

        val statsDao = migratedDb.usageStatsDao()
        statsDao.upsertDailyUsage(
            DailyUsageStats(
                dateKey = "2026-11-25",
                appTimeSec = 120L,
                watchTimeSec = 600L,
                episodesCompleted = 1,
                sessionsCount = 2,
                updatedAt = 1764060000000L
            )
        )
        statsDao.upsertAnimeWatchStats(
            AnimeWatchStats(
                animeId = "21",
                animeTitle = "One Piece",
                coverImage = "https://img/cover.jpg",
                genres = "Action, Adventure",
                watchTimeSec = 600L,
                episodesWatchedCount = 1,
                lastWatchedAt = 1764060000000L
            )
        )

        assertEquals(600L, statsDao.getDailyUsageByDate("2026-11-25")?.watchTimeSec)
        assertEquals(1, statsDao.getAnimeWatchStatsById("21")?.episodesWatchedCount)

        migratedDb.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun repositoryRecordsClampedUsageRespectsIncognitoAndAvoidsDuplicateCompletions(): Unit = runBlocking {
        val repo = AnimeRepository(context)
        repo.clearUsageStats()

        val sampleAnime = Anime(
            id = "16498",
            title = "Attack on Titan",
            coverImage = "https://img/aot.jpg",
            bannerImage = "",
            synopsis = "Titans",
            status = "Finished",
            genres = "Action, Drama",
            totalEpisodes = 25
        )
        val episode1 = Episode(
            id = "16498_1",
            animeId = "16498",
            number = 1,
            title = "Episode 1",
            thumbnail = "",
            durationSec = 1440L
        )

        val fixedTs = 1764060000000L
        val expectedKey = UsageStatsCalculator.localDateKey(fixedTs)

        // Delta clamping: > 60s should clamp to 60s
        repo.recordAppUsage(deltaSec = 300L, incrementSession = true, timestampMs = fixedTs)
        repo.recordWatchTime(sampleAnime, deltaSec = 250L, timestampMs = fixedTs)

        val dailyAfterClamp = repo.dailyUsageStats.first().first { it.dateKey == expectedKey }
        assertEquals(60L, dailyAfterClamp.appTimeSec)
        assertEquals(60L, dailyAfterClamp.watchTimeSec)
        assertEquals(1, dailyAfterClamp.sessionsCount)

        // Save completed twice -> episode completion increments only once
        repo.saveWatchProgress(sampleAnime, episode1, currentTimeSec = 1400L, durationSec = 1440L, language = "sub", completed = true)
        repo.saveWatchProgress(sampleAnime, episode1, currentTimeSec = 1420L, durationSec = 1440L, language = "sub", completed = true)

        val todayKey = UsageStatsCalculator.localDateKey()
        val todayStats = repo.dailyUsageStats.first().first { it.dateKey == todayKey }
        val animeStats = repo.animeWatchStats.first().first { it.animeId == "16498" }
        assertEquals(1, todayStats.episodesCompleted)
        assertEquals(1, animeStats.episodesWatchedCount)

        // Incognito mode must block new recordings
        repo.settingsManager.updateIncognitoMode(true)
        repo.recordAppUsage(deltaSec = 30L, incrementSession = true, timestampMs = fixedTs)
        repo.recordWatchTime(sampleAnime, deltaSec = 30L, timestampMs = fixedTs)
        repo.recordEpisodeCompleted(sampleAnime, timestampMs = fixedTs)

        val unchanged = repo.dailyUsageStats.first().first { it.dateKey == expectedKey }
        assertEquals(60L, unchanged.appTimeSec)
        assertEquals(60L, unchanged.watchTimeSec)
        assertEquals(1, unchanged.sessionsCount)

        repo.settingsManager.updateIncognitoMode(false)
        repo.clearUsageStats()
    }

    @Test
    fun calculatorComputesStreaksRangesAndTopGenresAccurately() {
        val daily = listOf(
            DailyUsageStats("2026-11-21", appTimeSec = 300, watchTimeSec = 1200, episodesCompleted = 1, sessionsCount = 1),
            DailyUsageStats("2026-11-23", appTimeSec = 400, watchTimeSec = 1800, episodesCompleted = 2, sessionsCount = 1),
            DailyUsageStats("2026-11-24", appTimeSec = 500, watchTimeSec = 2400, episodesCompleted = 2, sessionsCount = 2),
            DailyUsageStats("2026-11-25", appTimeSec = 200, watchTimeSec = 900, episodesCompleted = 1, sessionsCount = 1)
        )
        val anime = listOf(
            AnimeWatchStats("1", "Frieren", "", "Adventure, Fantasy", watchTimeSec = 3600, episodesWatchedCount = 3),
            AnimeWatchStats("2", "Solo Leveling", "", "Action, Fantasy", watchTimeSec = 1800, episodesWatchedCount = 2)
        )

        val weekSummary = UsageStatsCalculator.computeSummary(
            dailyList = daily,
            animeList = anime,
            range = UsageStatsRange.WEEK_7,
            todayKey = "2026-11-25"
        )
        assertEquals(6300L, weekSummary.totalWatchTimeSec)
        assertEquals(1400L, weekSummary.totalAppTimeSec)
        assertEquals(6, weekSummary.episodesCompleted)
        assertEquals(5, weekSummary.sessionsCount)
        assertEquals(4, weekSummary.activeDays)
        assertEquals(3, weekSummary.currentStreakDays)
        assertEquals(3, weekSummary.longestStreakDays)
        assertEquals(7, weekSummary.chartDays.size)
        assertEquals("Frieren", weekSummary.topAnime.first().animeTitle)
        assertEquals("Fantasy", weekSummary.topGenres.first().genre)
        assertEquals(5400L, weekSummary.topGenres.first().watchTimeSec)
        assertEquals(2, weekSummary.topGenres.first().animeCount)

        val todaySummary = UsageStatsCalculator.computeSummary(
            dailyList = daily,
            animeList = anime,
            range = UsageStatsRange.TODAY,
            todayKey = "2026-11-25"
        )
        assertEquals(900L, todaySummary.totalWatchTimeSec)
        assertEquals(200L, todaySummary.totalAppTimeSec)
        assertEquals(1, todaySummary.episodesCompleted)
    }

    @Test
    fun backupExportAndImportAreIdempotentAndAcceptWebProgressState(): Unit = runBlocking {
        val repo = AnimeRepository(context)
        repo.clearUsageStats()

        val webPayload = JSONObject().apply {
            put("version", 2)
            put("app", "ZenithAnime")
            put("animeState", JSONArray())
            put("progressState", JSONArray().apply {
                put(JSONObject().apply {
                    put("id", "999_ep_2")
                    put("animeId", "999")
                    put("episodeNumber", 2)
                    put("episodeTitle", "Episode 2")
                    put("animeTitle", "Web Show")
                    put("coverImage", "")
                    put("currentTimeSec", 500)
                    put("durationSec", 1440)
                    put("language", "sub")
                    put("completed", false)
                    put("lastWatchedAt", 1764060000000L)
                })
            })
            put("usageStats", JSONObject().apply {
                put("schemaVersion", 1)
                put("dailyUsage", JSONArray().apply {
                    put(JSONObject().apply {
                        put("dateKey", "2026-11-24")
                        put("appTimeSec", 300)
                        put("watchTimeSec", 1500)
                        put("episodesCompleted", 1)
                        put("sessionsCount", 2)
                        put("updatedAt", 1764000000000L)
                    })
                })
                put("animeStats", JSONArray().apply {
                    put(JSONObject().apply {
                        put("animeId", "999")
                        put("animeTitle", "Web Show")
                        put("coverImage", "")
                        put("genres", "Sci-Fi, Action")
                        put("watchTimeSec", 1500)
                        put("episodesWatchedCount", 1)
                        put("lastWatchedAt", 1764000000000L)
                    })
                })
            })
        }.toString()

        // Import once and twice in MERGE mode -> counters must NOT double-count
        val res1 = repo.importBackupJson(webPayload, mode = "MERGE")
        val res2 = repo.importBackupJson(webPayload, mode = "MERGE")
        assertTrue(res1.contains("Successfully processed 1"))
        assertTrue(res2.contains("Successfully processed"))

        val daily = repo.dailyUsageStats.first().first { it.dateKey == "2026-11-24" }
        assertEquals(1500L, daily.watchTimeSec)
        assertEquals(300L, daily.appTimeSec)
        assertEquals(2, daily.sessionsCount)

        val exportedJson = JSONObject(repo.exportBackupJson())
        assertTrue(exportedJson.has("usageStats"))
        val exportedDaily = exportedJson.getJSONObject("usageStats").getJSONArray("dailyUsage")
        assertEquals(1, exportedDaily.length())

        // REPLACE with empty usageStats clears existing stats
        val emptyReplacePayload = JSONObject().apply {
            put("version", 1)
            put("app", "ZenithAnime")
            put("animeState", JSONArray())
            put("watchProgress", JSONArray())
        }.toString()
        repo.importBackupJson(emptyReplacePayload, mode = "REPLACE")
        assertFalse(repo.dailyUsageStats.first().any())
        assertFalse(repo.animeWatchStats.first().any())
    }
}
