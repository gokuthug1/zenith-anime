package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Aggregated per-day usage and watch-time metrics stored locally in Room.
 * [dateKey] uses the device's local calendar date formatted as `yyyy-MM-dd`.
 */
@Entity(tableName = "daily_usage_stats")
data class DailyUsageStats(
    @PrimaryKey val dateKey: String,
    val appTimeSec: Long = 0L,
    val watchTimeSec: Long = 0L,
    val episodesCompleted: Int = 0,
    val sessionsCount: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Aggregated per-anime cumulative watch-time and episode completion metrics.
 */
@Entity(tableName = "anime_watch_stats")
data class AnimeWatchStats(
    @PrimaryKey val animeId: String,
    val animeTitle: String = "",
    val coverImage: String = "",
    val genres: String = "",
    val watchTimeSec: Long = 0L,
    val episodesWatchedCount: Int = 0,
    val lastWatchedAt: Long = System.currentTimeMillis()
)

enum class UsageStatsRange(val label: String, val days: Int?) {
    TODAY("Today", 1),
    WEEK_7("7 Days", 7),
    MONTH_30("30 Days", 30),
    ALL_TIME("All Time", null)
}

data class DailyActivityPoint(
    val dateKey: String,
    val shortLabel: String,
    val watchTimeSec: Long,
    val appTimeSec: Long,
    val episodesCompleted: Int
)

data class GenreWatchStat(
    val genre: String,
    val watchTimeSec: Long,
    val animeCount: Int
)

data class UsageStatsSummary(
    val range: UsageStatsRange,
    val totalWatchTimeSec: Long,
    val totalAppTimeSec: Long,
    val episodesCompleted: Int,
    val sessionsCount: Int,
    val activeDays: Int,
    val avgDailyWatchTimeSec: Long,
    val currentStreakDays: Int,
    val longestStreakDays: Int,
    val chartDays: List<DailyActivityPoint>,
    val topAnime: List<AnimeWatchStats>,
    val topGenres: List<GenreWatchStat>
)

object UsageStatsCalculator {
    private val dateKeyPattern = Regex("""^\d{4}-\d{2}-\d{2}$""")

    fun localDateKey(epochMillis: Long = System.currentTimeMillis()): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return fmt.format(Date(epochMillis))
    }

    fun isValidDateKey(dateKey: String): Boolean = dateKeyPattern.matches(dateKey)

    fun shiftDateKey(baseDateKey: String, deltaDays: Int): String {
        val cal = parseCalendar(baseDateKey) ?: Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, deltaDays)
        return formatCalendarKey(cal)
    }

    fun shortDayLabel(dateKey: String): String {
        val cal = parseCalendar(dateKey) ?: return dateKey.takeLast(5)
        return SimpleDateFormat("EEE", Locale.US).format(cal.time)
    }

    fun formatDuration(totalSec: Long): String {
        val safe = totalSec.coerceAtLeast(0L)
        if (safe < 60L) return "${safe}s"
        val totalMinutes = safe / 60L
        val hours = totalMinutes / 60L
        val minutes = totalMinutes % 60L
        return when {
            hours >= 24L -> {
                val days = hours / 24L
                val remHours = hours % 24L
                if (remHours > 0L) "${days}d ${remHours}h" else "${days}d"
            }
            hours > 0L -> if (minutes > 0L) "${hours}h ${minutes}m" else "${hours}h"
            else -> "${minutes}m"
        }
    }

    fun computeSummary(
        dailyList: List<DailyUsageStats>,
        animeList: List<AnimeWatchStats>,
        range: UsageStatsRange,
        todayKey: String = localDateKey(),
        topAnimeLimit: Int = 8,
        topGenresLimit: Int = 6
    ): UsageStatsSummary {
        val dailyByKey = dailyList.filter { isValidDateKey(it.dateKey) }.associateBy { it.dateKey }
        val cutoffKey = range.days?.let { shiftDateKey(todayKey, -(it - 1)) }

        val filteredDaily = dailyByKey.values.filter { entry ->
            cutoffKey == null || (entry.dateKey >= cutoffKey && entry.dateKey <= todayKey)
        }

        val totalWatch = filteredDaily.sumOf { it.watchTimeSec.coerceAtLeast(0L) }
        val totalApp = filteredDaily.sumOf { it.appTimeSec.coerceAtLeast(0L) }
        val totalEps = filteredDaily.sumOf { it.episodesCompleted.coerceAtLeast(0) }
        val totalSessions = filteredDaily.sumOf { it.sessionsCount.coerceAtLeast(0) }
        val activeDays = filteredDaily.count { it.watchTimeSec > 0L || it.appTimeSec > 0L || it.episodesCompleted > 0 }
        val divisor = when (range) {
            UsageStatsRange.TODAY -> 1L
            UsageStatsRange.WEEK_7 -> 7L
            UsageStatsRange.MONTH_30 -> 30L
            UsageStatsRange.ALL_TIME -> activeDays.coerceAtLeast(1).toLong()
        }
        val avgDaily = totalWatch / divisor

        val (currentStreak, longestStreak) = computeStreaks(dailyByKey, todayKey)

        val chartWindowDays = when (range) {
            UsageStatsRange.TODAY -> 7
            UsageStatsRange.WEEK_7 -> 7
            UsageStatsRange.MONTH_30 -> 14
            UsageStatsRange.ALL_TIME -> 14
        }
        val chartDays = (-(chartWindowDays - 1)..0).map { offset ->
            val key = shiftDateKey(todayKey, offset)
            val row = dailyByKey[key]
            DailyActivityPoint(
                dateKey = key,
                shortLabel = if (offset == 0) "Today" else shortDayLabel(key),
                watchTimeSec = row?.watchTimeSec?.coerceAtLeast(0L) ?: 0L,
                appTimeSec = row?.appTimeSec?.coerceAtLeast(0L) ?: 0L,
                episodesCompleted = row?.episodesCompleted?.coerceAtLeast(0) ?: 0
            )
        }

        val sortedAnime = animeList
            .filter { it.watchTimeSec > 0L || it.episodesWatchedCount > 0 }
            .sortedWith(
                compareByDescending<AnimeWatchStats> { it.watchTimeSec }
                    .thenByDescending { it.episodesWatchedCount }
                    .thenByDescending { it.lastWatchedAt }
            )
            .take(topAnimeLimit)

        val genreTotals = linkedMapOf<String, Pair<Long, MutableSet<String>>>()
        animeList.forEach { stat ->
            if (stat.watchTimeSec <= 0L && stat.episodesWatchedCount <= 0) return@forEach
            val parsedGenres = stat.genres.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.equals("Anime", ignoreCase = true) }
                .distinct()
            parsedGenres.forEach { genre ->
                val cur = genreTotals[genre]
                if (cur == null) {
                    genreTotals[genre] = stat.watchTimeSec.coerceAtLeast(0L) to mutableSetOf(stat.animeId)
                } else {
                    cur.second.add(stat.animeId)
                    genreTotals[genre] = (cur.first + stat.watchTimeSec.coerceAtLeast(0L)) to cur.second
                }
            }
        }
        val topGenres = genreTotals.entries
            .map { (genre, pair) ->
                GenreWatchStat(
                    genre = genre,
                    watchTimeSec = pair.first,
                    animeCount = pair.second.size
                )
            }
            .sortedWith(compareByDescending<GenreWatchStat> { it.watchTimeSec }.thenByDescending { it.animeCount })
            .take(topGenresLimit)

        return UsageStatsSummary(
            range = range,
            totalWatchTimeSec = totalWatch,
            totalAppTimeSec = totalApp,
            episodesCompleted = totalEps,
            sessionsCount = totalSessions,
            activeDays = activeDays,
            avgDailyWatchTimeSec = avgDaily,
            currentStreakDays = currentStreak,
            longestStreakDays = longestStreak,
            chartDays = chartDays,
            topAnime = sortedAnime,
            topGenres = topGenres
        )
    }

    private fun computeStreaks(
        dailyByKey: Map<String, DailyUsageStats>,
        todayKey: String
    ): Pair<Int, Int> {
        val activeKeys = dailyByKey.values
            .filter { it.watchTimeSec > 0L || it.appTimeSec > 0L || it.episodesCompleted > 0 }
            .map { it.dateKey }
            .toSortedSet()

        if (activeKeys.isEmpty()) return 0 to 0

        // Current streak counts consecutive days ending today, or ending yesterday if nothing recorded yet today.
        var cursor = if (todayKey in activeKeys) todayKey else shiftDateKey(todayKey, -1)
        var currentStreak = 0
        while (cursor in activeKeys) {
            currentStreak++
            cursor = shiftDateKey(cursor, -1)
        }

        // Longest streak across all recorded active days.
        var longestStreak = 0
        var run = 0
        var prevKey: String? = null
        for (key in activeKeys) {
            if (prevKey != null && shiftDateKey(prevKey, 1) == key) {
                run++
            } else {
                run = 1
            }
            if (run > longestStreak) longestStreak = run
            prevKey = key
        }

        return currentStreak to longestStreak
    }

    private fun parseCalendar(dateKey: String): Calendar? {
        if (!isValidDateKey(dateKey)) return null
        return try {
            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
            val parsed = fmt.parse(dateKey) ?: return null
            Calendar.getInstance().apply {
                time = parsed
                set(Calendar.HOUR_OF_DAY, 12)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun formatCalendarKey(cal: Calendar): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }
}
