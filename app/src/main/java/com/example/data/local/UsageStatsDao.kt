package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.AnimeWatchStats
import com.example.data.model.DailyUsageStats
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageStatsDao {
    @Query("SELECT * FROM daily_usage_stats ORDER BY dateKey DESC")
    fun observeAllDailyUsage(): Flow<List<DailyUsageStats>>

    @Query("SELECT * FROM daily_usage_stats ORDER BY dateKey DESC")
    suspend fun getAllDailyUsageOnce(): List<DailyUsageStats>

    @Query("SELECT * FROM daily_usage_stats WHERE dateKey = :dateKey")
    suspend fun getDailyUsageByDate(dateKey: String): DailyUsageStats?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailyUsage(item: DailyUsageStats)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAllDailyUsage(items: List<DailyUsageStats>)

    @Query("SELECT * FROM anime_watch_stats ORDER BY watchTimeSec DESC, lastWatchedAt DESC")
    fun observeAllAnimeWatchStats(): Flow<List<AnimeWatchStats>>

    @Query("SELECT * FROM anime_watch_stats ORDER BY watchTimeSec DESC, lastWatchedAt DESC")
    suspend fun getAllAnimeWatchStatsOnce(): List<AnimeWatchStats>

    @Query("SELECT * FROM anime_watch_stats WHERE animeId = :animeId")
    suspend fun getAnimeWatchStatsById(animeId: String): AnimeWatchStats?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAnimeWatchStats(item: AnimeWatchStats)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAllAnimeWatchStats(items: List<AnimeWatchStats>)

    @Query("DELETE FROM daily_usage_stats")
    suspend fun clearDailyUsage()

    @Query("DELETE FROM anime_watch_stats")
    suspend fun clearAnimeWatchStats()
}
