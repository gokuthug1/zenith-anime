package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.model.Anime
import com.example.data.model.AppNotification
import com.example.data.model.DownloadItem
import com.example.data.model.Episode
import com.example.data.model.MalReview
import com.example.data.model.WatchProgress
import com.example.data.model.WatchStatus
import com.example.data.network.AniListService
import com.example.data.network.MalSyncService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import com.example.data.local.SettingsManager
import com.example.data.model.AppSettings
import kotlinx.coroutines.withContext
import com.example.data.download.AnimeDownloadManager
import com.example.data.resolver.StreamResolverRegistry
import com.example.data.resolver.SubtitleTrack
import org.json.JSONArray
import org.json.JSONObject

/** Result of asking the repository to start a download. */
data class DownloadStart(val started: Boolean, val error: String? = null)

class AnimeRepository(private val context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val animeDao = db.animeDao()
    private val watchProgressDao = db.watchProgressDao()
    private val downloadDao = db.downloadDao()
    private val notificationDao = db.notificationDao()
    private val aniListService = AniListService()
    val malSyncService = MalSyncService()
    val animeThemesService = com.example.data.network.AnimeThemesService()
    val trackerSyncManager = com.example.data.sync.TrackerSyncManager()
    val discordRpcService = com.example.data.sync.DiscordRpcService()
    val settingsManager = SettingsManager(context)
    val downloadManager = AnimeDownloadManager(context, db, settingsManager)
    val resolverRegistry = StreamResolverRegistry()

    val allAnime: Flow<List<Anime>> = animeDao.getAllAnime().flowOn(Dispatchers.IO)
    val favorites: Flow<List<Anime>> = animeDao.getFavorites().flowOn(Dispatchers.IO)
    val watchlist: Flow<List<Anime>> = animeDao.getWatchlist().flowOn(Dispatchers.IO)
    val completed: Flow<List<Anime>> = animeDao.getCompleted().flowOn(Dispatchers.IO)
    val continueWatching: Flow<List<WatchProgress>> = watchProgressDao.getContinueWatching().flowOn(Dispatchers.IO)

    /** Continue Watching collapsed to a single entry per anime. */
    val continueWatchingLatest: Flow<List<WatchProgress>> =
        watchProgressDao.getLatestProgressPerAnime().flowOn(Dispatchers.IO)
    val allHistory: Flow<List<WatchProgress>> = watchProgressDao.getAllProgress().flowOn(Dispatchers.IO)
    val allDownloads: Flow<List<DownloadItem>> = downloadDao.getAllDownloads().flowOn(Dispatchers.IO)
    val allNotifications: Flow<List<AppNotification>> = notificationDao.getAllNotifications().flowOn(Dispatchers.IO)
    val settings: Flow<AppSettings> = settingsManager.settings

    fun getAnimeById(id: String): Flow<Anime?> = animeDao.getAnimeById(id).flowOn(Dispatchers.IO)
    fun getAnimeByScheduleDay(day: String): Flow<List<Anime>> = animeDao.getAnimeByScheduleDay(day).flowOn(Dispatchers.IO)
    fun searchLocalAnime(query: String): Flow<List<Anime>> = animeDao.searchAnime(query).flowOn(Dispatchers.IO)
    fun getAnimeByWatchStatus(status: String): Flow<List<Anime>> = animeDao.getAnimeByWatchStatus(status).flowOn(Dispatchers.IO)
    fun getEpisodes(animeId: String): Flow<List<Episode>> = animeDao.getEpisodesForAnime(animeId).flowOn(Dispatchers.IO)
    fun observeLatestProgress(animeId: String): Flow<WatchProgress?> = watchProgressDao.observeLatestProgressForAnime(animeId).flowOn(Dispatchers.IO)

    suspend fun updateWatchStatus(id: String, status: String) = withContext(Dispatchers.IO) {
        val normalized = WatchStatus.fromRaw(status)
        animeDao.updateWatchStatusFull(
            id = id,
            status = normalized.name,
            isCompleted = normalized == WatchStatus.COMPLETED,
            inWatchlist = true,
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Inserts or updates an anime from an online/catalog source while preserving all user-owned
     * local state (`isFavorite`, `isInWatchlist`, `isCompleted`, `watchStatus`, `lastUpdated`).
     * This prevents opening Details, refreshing the catalog, or loading a schedule from wiping out
     * an anime the user saved to their Watchlist or Favorites.
     */
    private suspend fun upsertPreservingLocalState(incoming: Anime): Anime {
        val existing = animeDao.getAnimeByIdOnce(incoming.id)
            ?: incoming.malId.takeIf { it.isNotEmpty() && it != "0" }?.let { animeDao.getAnimeByMalIdOnce(it) }
        val merged = mergeWithExisting(incoming, existing)
        animeDao.insert(merged)
        return merged
    }

    private suspend fun upsertAllPreservingLocalState(incomingList: List<Anime>): List<Anime> {
        val mergedList = incomingList.map { incoming ->
            val existing = animeDao.getAnimeByIdOnce(incoming.id)
                ?: incoming.malId.takeIf { it.isNotEmpty() && it != "0" }?.let { animeDao.getAnimeByMalIdOnce(it) }
            mergeWithExisting(incoming, existing)
        }
        if (mergedList.isNotEmpty()) {
            animeDao.insertAll(mergedList)
        }
        return mergedList
    }

    internal fun mergeWithExisting(incoming: Anime, existing: Anime?): Anime {
        if (existing == null) return incoming
        // Keep the existing primary key if we matched by malId so Watchlist references don't break
        val effectiveId = existing.id
        val keepFavorite = existing.isFavorite || incoming.isFavorite
        val keepWatchlist = existing.isInWatchlist || incoming.isInWatchlist
        val keepCompleted = existing.isCompleted || incoming.isCompleted
        val keepStatus = when {
            incoming.isInWatchlist && incoming.watchStatus != "WATCHING" -> incoming.watchStatus
            existing.isInWatchlist || existing.isFavorite || existing.isCompleted -> existing.watchStatus
            else -> incoming.watchStatus
        }
        return incoming.copy(
            id = effectiveId,
            malId = incoming.malId.takeIf { it.isNotEmpty() && it != "0" } ?: existing.malId,
            isFavorite = keepFavorite,
            isInWatchlist = keepWatchlist,
            isCompleted = keepCompleted,
            watchStatus = keepStatus,
            lastUpdated = if (existing.isInWatchlist || existing.isFavorite) existing.lastUpdated else incoming.lastUpdated
        )
    }

    suspend fun searchOnlineFiltered(
        query: String? = null,
        format: String? = null,
        status: String? = null,
        season: String? = null,
        seasonYear: Int? = null,
        sort: String? = null,
        genre: String? = null
    ): List<Anime> = withContext(Dispatchers.IO) {
        val results = aniListService.searchAnimeFiltered(query, format, status, season, seasonYear, sort, genre)
        if (results.isNotEmpty()) {
            val merged = upsertAllPreservingLocalState(results)
            merged.forEach { ensureEpisodesGenerated(it) }
            return@withContext merged
        }
        results
    }

    suspend fun getRandomAnime(): Anime? = withContext(Dispatchers.IO) {
        val randomAnime = aniListService.getRandomAnime()
        if (randomAnime != null) {
            val merged = upsertPreservingLocalState(randomAnime)
            ensureEpisodesGenerated(merged)
            return@withContext merged
        }
        randomAnime
    }

    suspend fun getAnimeDetailsWithRelations(animeId: String): Pair<Anime?, List<com.example.data.model.AnimeRelation>> = withContext(Dispatchers.IO) {
        val existingPrior = animeDao.getAnimeByIdOnce(animeId)
            ?: animeDao.getAnimeByMalIdOnce(animeId.removePrefix("mal_"))

        val lookupId = if (animeId.startsWith("mal_")) {
            animeId
        } else if (existingPrior?.malId?.isNotBlank() == true && existingPrior.malId != "0" && existingPrior.id.startsWith("mal_")) {
            "mal_${existingPrior.malId}"
        } else {
            animeId
        }

        val pair = aniListService.getAnimeDetailsWithRelations(lookupId)
        if (pair.first != null) {
            val incoming = pair.first!!.copy(
                id = existingPrior?.id ?: animeId,
                malId = if (pair.first!!.malId.isNotBlank() && pair.first!!.malId != "0") pair.first!!.malId else (existingPrior?.malId ?: "")
            )
            val merged = mergeWithExisting(incoming, existingPrior)
            animeDao.insert(merged)
            ensureEpisodesGenerated(merged)
            return@withContext Pair(merged, pair.second)
        } else if (existingPrior != null) {
            ensureEpisodesGenerated(existingPrior)
            return@withContext Pair(existingPrior, emptyList())
        }
        pair
    }

    suspend fun getMalReviews(animeId: String, malId: String = ""): List<MalReview> = withContext(Dispatchers.IO) {
        val effectiveMalId = malId.ifBlank { animeDao.getAnimeByIdOnce(animeId)?.malId.orEmpty() }
        aniListService.getMalReviews(animeId, effectiveMalId)
    }

    suspend fun getAnimeCharacters(animeId: String): List<com.example.data.model.CharacterVa> = withContext(Dispatchers.IO) {
        aniListService.getAnimeCharacters(animeId)
    }

    suspend fun getAnimeThemes(anilistId: String?, title: String): List<com.example.data.model.AnimeTheme> = withContext(Dispatchers.IO) {
        animeThemesService.getAnimeThemes(anilistId, title)
    }

    suspend fun importWatchlist(animeList: List<Anime>, eraseExisting: Boolean): Int = withContext(Dispatchers.IO) {
        if (eraseExisting) {
            animeDao.clearWatchlist()
        }
        if (animeList.isNotEmpty()) {
            val normalizedList = animeList.map { item ->
                val statusEnum = WatchStatus.fromRaw(item.watchStatus)
                val existing = animeDao.getAnimeByIdOnce(item.id)
                    ?: item.malId.takeIf { it.isNotEmpty() && it != "0" }?.let { animeDao.getAnimeByMalIdOnce(it) }
                if (existing != null) {
                    existing.copy(
                        malId = item.malId.ifBlank { existing.malId },
                        isInWatchlist = true,
                        isCompleted = statusEnum == WatchStatus.COMPLETED || existing.isCompleted,
                        watchStatus = statusEnum.name,
                        lastUpdated = System.currentTimeMillis()
                    )
                } else {
                    item.copy(
                        isInWatchlist = true,
                        isCompleted = statusEnum == WatchStatus.COMPLETED,
                        watchStatus = statusEnum.name,
                        lastUpdated = System.currentTimeMillis()
                    )
                }
            }
            animeDao.insertAll(normalizedList)
            normalizedList.forEach { ensureEpisodesGenerated(it) }
        }
        animeList.size
    }

    suspend fun searchOnlineAnime(query: String): List<Anime> = withContext(Dispatchers.IO) {
        val results = aniListService.searchAnime(query)
        if (results.isNotEmpty()) {
            val merged = upsertAllPreservingLocalState(results)
            merged.forEach { ensureEpisodesGenerated(it) }
            return@withContext merged
        }
        results
    }

    suspend fun fetchTrendingOnline(): List<Anime> = withContext(Dispatchers.IO) {
        val results = aniListService.getTrendingAnime()
        if (results.isNotEmpty()) {
            val merged = upsertAllPreservingLocalState(results)
            merged.forEach { ensureEpisodesGenerated(it) }
            return@withContext merged
        }
        results
    }

    suspend fun fetchPopularOnline(): List<Anime> = withContext(Dispatchers.IO) {
        val results = aniListService.getPopularAnime()
        if (results.isNotEmpty()) {
            val merged = upsertAllPreservingLocalState(results)
            merged.forEach { ensureEpisodesGenerated(it) }
            return@withContext merged
        }
        results
    }

    suspend fun fetchTopRatedOnline(): List<Anime> = withContext(Dispatchers.IO) {
        val results = aniListService.getTopRatedAnime()
        if (results.isNotEmpty()) {
            val merged = upsertAllPreservingLocalState(results)
            merged.forEach { ensureEpisodesGenerated(it) }
            return@withContext merged
        }
        results
    }

    suspend fun fetchByGenreOnline(genre: String): List<Anime> = withContext(Dispatchers.IO) {
        val results = aniListService.getAnimeByGenre(genre)
        if (results.isNotEmpty()) {
            val merged = upsertAllPreservingLocalState(results)
            merged.forEach { ensureEpisodesGenerated(it) }
            return@withContext merged
        }
        results
    }

    suspend fun ensureCatalogPopulated() = withContext(Dispatchers.IO) {
        selfHealWatchProgress()
        val current = animeDao.getAllAnime().first()
        if (current.isEmpty()) {
            val trending = aniListService.getTrendingAnime()
            val popular = aniListService.getPopularAnime()
            val combined = (trending + popular).distinctBy { it.id }
            if (combined.isNotEmpty()) {
                val merged = upsertAllPreservingLocalState(combined)
                merged.forEach { ensureEpisodesGenerated(it) }
            }
        } else {
            // Re-sync existing database anime so unreleased phantom episodes are pruned
            current.forEach { ensureEpisodesGenerated(it) }
        }
    }

    private val dismissedContinueWatchingAnimeIds = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private val dismissedProgressIds = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private val scrobbledEpisodes = java.util.concurrent.ConcurrentHashMap<String, Boolean>()
    private val lastDiscordRpcAt = java.util.concurrent.ConcurrentHashMap<String, Long>()

    suspend fun ensureEpisodesGenerated(anime: Anime) = withContext(Dispatchers.IO) {
        val availableCount = anime.availableEpisodes
        if (availableCount <= 0) {
            // Anime has not aired yet or has 0 available episodes! Delete phantom episodes.
            animeDao.deleteEpisodesForAnime(anime.id)
            return@withContext
        }
        // Clean up any phantom episodes beyond availableCount
        animeDao.deleteEpisodesAboveNumber(anime.id, availableCount)

        val existing = animeDao.getEpisodesForAnimeSync(anime.id)
        if (existing.size == availableCount) {
            return@withContext
        }
        val existingByNumber = existing.associateBy { it.number }

        val episodes = (1..availableCount).map { num ->
            existingByNumber[num] ?: Episode(
                id = "${anime.id}_$num",
                animeId = anime.id,
                number = num,
                title = "Episode $num",
                thumbnail = anime.bannerImage.ifEmpty { anime.coverImage },
                durationSec = 1440L,
                hasSub = true,
                hasDub = true,
                isOffline = false
            )
        }
        animeDao.insertEpisodes(episodes)
    }

    suspend fun getProgressForEpisode(animeId: String, episodeNumber: Int): WatchProgress? = withContext(Dispatchers.IO) {
        watchProgressDao.getProgressById("${animeId}_$episodeNumber")
    }

    suspend fun toggleFavorite(animeId: String, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        animeDao.updateFavorite(animeId, isFavorite)
    }

    suspend fun toggleWatchlist(animeId: String, inWatchlist: Boolean, animeFallback: Anime? = null) = withContext(Dispatchers.IO) {
        val existing = animeDao.getAnimeByIdOnce(animeId)
            ?: animeFallback?.malId?.takeIf { it.isNotBlank() && it != "0" }?.let { animeDao.getAnimeByMalIdOnce(it) }

        if (existing != null) {
            val currentStatus = existing.watchStatus
            val validStatus = if (currentStatus.isBlank()) WatchStatus.PLAN_TO_WATCH.name else WatchStatus.fromRaw(currentStatus).name
            animeDao.updateWatchStatusFull(
                id = existing.id,
                status = validStatus,
                isCompleted = validStatus == WatchStatus.COMPLETED.name || existing.isCompleted,
                inWatchlist = inWatchlist,
                updatedAt = System.currentTimeMillis()
            )
        } else if (animeFallback != null) {
            val initialStatus = if (inWatchlist) WatchStatus.PLAN_TO_WATCH.name else animeFallback.watchStatus
            val newAnime = animeFallback.copy(
                id = animeId,
                isInWatchlist = inWatchlist,
                watchStatus = initialStatus,
                lastUpdated = System.currentTimeMillis()
            )
            animeDao.insert(newAnime)
            ensureEpisodesGenerated(newAnime)
        } else {
            animeDao.updateWatchlist(animeId, inWatchlist)
        }
    }

    suspend fun markCompleted(animeId: String, isCompleted: Boolean) = withContext(Dispatchers.IO) {
        animeDao.updateCompleted(animeId, isCompleted)
        if (isCompleted) {
            animeDao.updateWatchStatusFull(
                id = animeId,
                status = WatchStatus.COMPLETED.name,
                isCompleted = true,
                inWatchlist = true,
                updatedAt = System.currentTimeMillis()
            )
        } else {
            animeDao.updateWatchStatus(animeId, WatchStatus.WATCHING.name)
        }
    }

    suspend fun getAiringSchedule(startSec: Long, endSec: Long): List<com.example.data.model.AiringScheduleItem> = withContext(Dispatchers.IO) {
        val items = aniListService.getAiringSchedule(startSec, endSec)
        if (items.isNotEmpty()) {
            val animeList = items.map { it.anime }.distinctBy { it.id }
            val merged = upsertAllPreservingLocalState(animeList)
            merged.forEach { ensureEpisodesGenerated(it) }
            val mergedById = merged.associateBy { it.id }
            return@withContext items.map { item ->
                item.copy(anime = mergedById[item.anime.id] ?: item.anime)
            }
        }
        items
    }

    suspend fun selfHealWatchProgress() = withContext(Dispatchers.IO) {
        if (!settingsManager.getSettings().continueWatchingShowNextEpisode) return@withContext
        try {
            val allProg = watchProgressDao.getAllProgress().first()
            val byAnime = allProg.groupBy { it.animeId }
            for ((animeId, list) in byAnime) {
                if (animeId in dismissedContinueWatchingAnimeIds) continue
                val hasUnfinished = list.any { !it.completed }
                if (!hasUnfinished && list.isNotEmpty()) {
                    val anime = animeDao.getAnimeByIdOnce(animeId) ?: continue
                    if (anime.isCompleted || WatchStatus.fromRaw(anime.watchStatus) != WatchStatus.WATCHING) {
                        continue
                    }
                    val maxEp = list.maxOfOrNull { it.episodeNumber } ?: 0
                    val availableEps = anime.availableEpisodes
                    if (maxEp in 1 until availableEps) {
                        val nextEpNum = maxEp + 1
                        val nextId = "${animeId}_$nextEpNum"
                        if (nextId in dismissedProgressIds) continue
                        val existing = watchProgressDao.getProgressById(nextId)
                        if (existing == null) {
                            val sample = list.first()
                            val nextProg = WatchProgress(
                                id = nextId,
                                animeId = animeId,
                                episodeNumber = nextEpNum,
                                episodeTitle = "Episode $nextEpNum",
                                animeTitle = anime.title.ifBlank { sample.animeTitle },
                                coverImage = anime.coverImage.ifBlank { sample.coverImage },
                                currentTimeSec = 0L,
                                durationSec = 1440L,
                                language = sample.language,
                                completed = false,
                                lastWatchedAt = list.maxOf { it.lastWatchedAt } + 5L
                            )
                            watchProgressDao.saveProgress(nextProg)
                        }
                    }
                }
            }
        } catch (_: Exception) {}
    }

    suspend fun saveWatchProgress(
        anime: Anime,
        episode: Episode,
        currentTimeSec: Long,
        durationSec: Long,
        language: String,
        completed: Boolean
    ) = withContext(Dispatchers.IO) {
        val currentSettings = settingsManager.getSettings()
        if (currentSettings.incognitoMode) return@withContext
        val now = System.currentTimeMillis()
        val effectiveDuration = if (durationSec > 0) durationSec else 1440L
        val epKey = "${anime.id}_${episode.number}"
        dismissedContinueWatchingAnimeIds.remove(anime.id)
        dismissedProgressIds.remove(epKey)

        val progress = WatchProgress(
            id = epKey,
            animeId = anime.id,
            episodeNumber = episode.number,
            episodeTitle = episode.title,
            animeTitle = anime.title,
            coverImage = anime.coverImage,
            currentTimeSec = currentTimeSec,
            durationSec = effectiveDuration,
            language = language,
            completed = completed,
            lastWatchedAt = now
        )
        watchProgressDao.saveProgress(progress)

        // Ensure anime is saved in local DB without clobbering watchlist/favorite flags
        try {
            val savedAnime = upsertPreservingLocalState(anime)
            if (currentSettings.autoAddToMyListOnWatch && !savedAnime.isInWatchlist) {
                animeDao.updateWatchStatusFull(
                    id = savedAnime.id,
                    status = WatchStatus.WATCHING.name,
                    isCompleted = savedAnime.isCompleted,
                    inWatchlist = true,
                    updatedAt = now
                )
            }
        } catch (_: Exception) {}

        val isSeriesCompleted = completed &&
            !anime.isAiring &&
            !anime.isUpcoming &&
            anime.totalEpisodes > 0 &&
            episode.number >= anime.totalEpisodes

        if (completed) {
            val availableEps = anime.availableEpisodes
            if (episode.number < availableEps) {
                if (currentSettings.continueWatchingShowNextEpisode) {
                    val nextEpNum = episode.number + 1
                    val nextKey = "${anime.id}_$nextEpNum"
                    dismissedProgressIds.remove(nextKey)
                    val existingNext = watchProgressDao.getProgressById(nextKey)
                    if (existingNext == null) {
                        val nextProgress = WatchProgress(
                            id = nextKey,
                            animeId = anime.id,
                            episodeNumber = nextEpNum,
                            episodeTitle = "Episode $nextEpNum",
                            animeTitle = anime.title,
                            coverImage = anime.coverImage,
                            currentTimeSec = 0L,
                            durationSec = effectiveDuration,
                            language = language,
                            completed = false,
                            lastWatchedAt = now + 10L
                        )
                        watchProgressDao.saveProgress(nextProgress)
                    }
                }
            } else if (isSeriesCompleted) {
                animeDao.updateCompleted(anime.id, true)
                animeDao.updateWatchStatus(anime.id, WatchStatus.COMPLETED.name)
            }
        }

        // Optional housekeeping: remove the offline file once the episode has been finished.
        if (completed && currentSettings.downloadAutoDeleteWatched) {
            downloadManager.deleteByEpisode(anime.id, episode.number)
        }

        // 2-Way Tracker Sync & Auto-Scrobbler (AniList & MAL) - fire once per (anime, episode, seriesCompleted)
        val percentWatched = if (effectiveDuration > 0) ((currentTimeSec.toFloat() / effectiveDuration.toFloat()) * 100f).toInt() else 0
        if (completed || percentWatched >= currentSettings.scrobbleCompletionPercent) {
            val scrobbleKey = "${anime.id}:${episode.number}:$isSeriesCompleted"
            if (scrobbledEpisodes.putIfAbsent(scrobbleKey, true) == null) {
                try {
                    val ok = trackerSyncManager.autoScrobble(
                        anime = anime,
                        episodeNumber = episode.number,
                        isCompleted = isSeriesCompleted,
                        settings = currentSettings
                    )
                    if (!ok) {
                        scrobbledEpisodes.remove(scrobbleKey)
                    }
                } catch (e: CancellationException) {
                    scrobbledEpisodes.remove(scrobbleKey)
                    throw e
                } catch (_: Exception) {
                    scrobbledEpisodes.remove(scrobbleKey)
                }
            }
        }

        // Discord Rich Presence status update - rate-limited to at most once per 30s per episode
        if (currentSettings.discordRpcEnabled) {
            val rpcKey = "${anime.id}:${episode.number}"
            val lastSent = lastDiscordRpcAt[rpcKey] ?: 0L
            if (completed || now - lastSent >= 30_000L) {
                lastDiscordRpcAt[rpcKey] = now
                try {
                    discordRpcService.updatePresence(
                        animeTitle = anime.title,
                        episodeNumber = episode.number,
                        currentTimeSec = currentTimeSec,
                        durationSec = effectiveDuration,
                        coverImage = anime.coverImage,
                        settings = currentSettings
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {}
            }
        }
    }

    suspend fun deleteProgress(progressId: String) = withContext(Dispatchers.IO) {
        dismissedProgressIds.add(progressId)
        val existing = watchProgressDao.getProgressById(progressId)
        if (existing != null) {
            dismissedContinueWatchingAnimeIds.add(existing.animeId)
        } else if (progressId.contains("_")) {
            dismissedContinueWatchingAnimeIds.add(progressId.substringBeforeLast("_"))
        }
        watchProgressDao.deleteProgress(progressId)
    }

    suspend fun deleteProgressByAnime(animeId: String) = withContext(Dispatchers.IO) {
        dismissedContinueWatchingAnimeIds.add(animeId)
        watchProgressDao.deleteProgressByAnimeId(animeId)
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        dismissedContinueWatchingAnimeIds.clear()
        dismissedProgressIds.clear()
        watchProgressDao.clearAllHistory()
    }

    /**
     * Starts an episode download.
     *
     * When no [streamUrl] is supplied (user downloaded from Home, Details, My List, Search or
     * Schedule) the episode is resolved through [resolverRegistry] first. If nothing playable can be
     * found the download fails with a real explanation instead of saving a placeholder video.
     */
    suspend fun addDownload(
        anime: Anime,
        episode: Episode,
        quality: String = "",
        streamUrl: String? = null,
        headers: Map<String, String> = emptyMap(),
        subtitles: List<SubtitleTrack> = emptyList()
    ): DownloadStart = withContext(Dispatchers.IO) {
        val settings = settingsManager.getSettings()
        val effectiveQuality = quality.ifBlank { settings.downloadQuality }
        var url = streamUrl
        var requestHeaders = headers
        var tracks = subtitles

        if (url.isNullOrBlank() || !url.startsWith("http")) {
            val result = resolverRegistry.resolve(
                anime = anime,
                episode = episode,
                language = settings.defaultLanguage,
                preferredServerId = settings.defaultServer,
                autoFailover = settings.autoFailover,
                maxAttempts = settings.failoverServerCount.coerceAtLeast(1)
            )
            val best = StreamResolverRegistry.pickForDownload(result.sources, effectiveQuality)
            if (best == null) {
                return@withContext DownloadStart(false, result.failureSummary())
            }
            url = best.url
            requestHeaders = best.headers
            if (tracks.isEmpty()) tracks = best.subtitles
        }

        if (tracks.isNotEmpty()) {
            AnimeDownloadManager.rememberSubtitle(anime.id, episode.number, tracks.first())
        }
        downloadManager.enqueueDownload(anime, episode, effectiveQuality, url, requestHeaders, tracks)
        DownloadStart(true, null)
    }

    suspend fun deleteDownload(id: String) = withContext(Dispatchers.IO) {
        downloadManager.deleteDownload(id)
    }

    /** Absolute path of a finished offline episode, when it exists. */
    suspend fun localPlaybackPath(animeId: String, episodeNumber: Int): String? =
        withContext(Dispatchers.IO) { downloadManager.localFileFor(animeId, episodeNumber) }

    /** Subtitle sidecar saved next to a finished download, so offline playback keeps captions. */
    suspend fun localSubtitlePath(animeId: String, episodeNumber: Int): String? =
        withContext(Dispatchers.IO) { downloadManager.localSubtitleFor(animeId, episodeNumber) }

    /** Copies a downloaded episode into the folder chosen with the system folder picker. */
    suspend fun exportDownloadToFolder(downloadId: String, treeUri: String): Boolean = withContext(Dispatchers.IO) {
        val item = downloadDao.getDownloadById(downloadId) ?: return@withContext false
        if (item.filePath.isBlank()) return@withContext false
        val file = java.io.File(item.filePath)
        if (!file.exists()) return@withContext false
        downloadManager.copyToSafFolder(file, treeUri)
    }

    suspend fun clearDownloads() = withContext(Dispatchers.IO) {
        downloadManager.clearAllDownloadFiles()
        downloadDao.clearDownloads()
    }

    suspend fun addNotification(notification: AppNotification) = withContext(Dispatchers.IO) {
        notificationDao.insert(notification)
    }

    suspend fun addNotification(
        title: String,
        message: String,
        animeThumbnail: String = "",
        tag: String = "AIRING",
        animeId: String = "",
        animeTitle: String = "",
        coverImage: String = "",
        episodeNumber: Int = 1
    ) = withContext(Dispatchers.IO) {
        val idPrefix = if (animeId.isNotBlank()) "notif_aid_${animeId}_" else "notif_"
        val notif = AppNotification(
            id = "${idPrefix}${System.currentTimeMillis()}_${java.util.UUID.randomUUID().toString().take(6)}",
            title = title,
            message = message,
            timestamp = "Just now",
            animeThumbnail = animeThumbnail.ifEmpty { coverImage },
            tag = tag,
            createdAt = System.currentTimeMillis(),
            isUnread = true
        )
        notificationDao.insert(notif)
    }

    suspend fun deleteNotification(id: String) = withContext(Dispatchers.IO) {
        notificationDao.deleteNotification(id)
    }

    suspend fun markNotificationRead(id: String) = withContext(Dispatchers.IO) {
        notificationDao.markRead(id)
    }

    suspend fun markAllNotificationsRead() = withContext(Dispatchers.IO) {
        notificationDao.markAllRead()
    }

    suspend fun clearNotifications() = withContext(Dispatchers.IO) {
        notificationDao.clearAll()
    }

    suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        val animeList = animeDao.getAllAnime().first()
        val progressList = watchProgressDao.getAllProgress().first()

        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("app", "ZenithAnime")

        val animeArray = JSONArray()
        animeList.forEach { a ->
            val obj = JSONObject()
            obj.put("id", a.id)
            obj.put("malId", a.malId)
            obj.put("title", a.title)
            obj.put("englishTitle", a.englishTitle)
            obj.put("romajiTitle", a.romajiTitle)
            obj.put("nativeTitle", a.nativeTitle)
            obj.put("coverImage", a.coverImage)
            obj.put("bannerImage", a.bannerImage)
            obj.put("synopsis", a.synopsis)
            obj.put("rating", a.rating)
            obj.put("year", a.year)
            obj.put("status", a.status)
            obj.put("type", a.type)
            obj.put("totalEpisodes", a.totalEpisodes)
            obj.put("genres", a.genres)
            obj.put("studio", a.studio)
            obj.put("isFavorite", a.isFavorite)
            obj.put("isInWatchlist", a.isInWatchlist)
            obj.put("isCompleted", a.isCompleted)
            obj.put("watchStatus", a.watchStatus)
            obj.put("lastUpdated", a.lastUpdated)
            animeArray.put(obj)
        }
        root.put("animeState", animeArray)

        val progressArray = JSONArray()
        progressList.forEach { p ->
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("animeId", p.animeId)
            obj.put("episodeNumber", p.episodeNumber)
            obj.put("episodeTitle", p.episodeTitle)
            obj.put("animeTitle", p.animeTitle)
            obj.put("coverImage", p.coverImage)
            obj.put("currentTimeSec", p.currentTimeSec)
            obj.put("durationSec", p.durationSec)
            obj.put("language", p.language)
            obj.put("completed", p.completed)
            obj.put("lastWatchedAt", p.lastWatchedAt)
            progressArray.put(obj)
        }
        root.put("watchProgress", progressArray)
        root.put("settingsJson", settingsManager.exportJson())

        val s = settingsManager.getSettings()
        val sObj = JSONObject()
        sObj.put("themeAccent", s.themeAccent)
        sObj.put("defaultServer", s.defaultServer)
        sObj.put("defaultLanguage", s.defaultLanguage)
        sObj.put("autoplay", s.autoplay)
        sObj.put("autoSkipIntro", s.autoSkipIntro)
        sObj.put("autoNextEpisode", s.autoNextEpisode)
        sObj.put("seekDurationSec", s.seekDurationSec)
        sObj.put("defaultPlaybackSpeed", s.defaultPlaybackSpeed.toDouble())
        sObj.put("amoledMode", s.amoledMode)
        sObj.put("incognitoMode", s.incognitoMode)
        root.put("settings", sObj)

        root.toString(2)
    }

    suspend fun importBackupJson(jsonString: String, mode: String = "MERGE"): String = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            val progressArray = root.optJSONArray("watchProgress") ?: JSONArray()
            val animeArray = root.optJSONArray("animeState") ?: JSONArray()

            val fullSettingsJson = root.optString("settingsJson", "")
            if (fullSettingsJson.isNotBlank()) {
                settingsManager.importJson(fullSettingsJson)
            } else {
                val settingsObj = root.optJSONObject("settings")
                if (settingsObj != null) {
                    if (settingsObj.has("themeAccent")) settingsManager.updateThemeAccent(settingsObj.getString("themeAccent"))
                    if (settingsObj.has("defaultServer")) settingsManager.updateDefaultServer(settingsObj.getString("defaultServer"))
                    if (settingsObj.has("defaultLanguage")) settingsManager.updateDefaultLanguage(settingsObj.getString("defaultLanguage"))
                    if (settingsObj.has("autoplay")) settingsManager.updateAutoplay(settingsObj.getBoolean("autoplay"))
                    if (settingsObj.has("autoSkipIntro")) settingsManager.updateAutoSkipIntro(settingsObj.getBoolean("autoSkipIntro"))
                    if (settingsObj.has("autoNextEpisode")) settingsManager.updateAutoNext(settingsObj.getBoolean("autoNextEpisode"))
                    if (settingsObj.has("seekDurationSec")) settingsManager.updateSeekInterval(settingsObj.getInt("seekDurationSec"))
                    if (settingsObj.has("defaultPlaybackSpeed")) settingsManager.updateDefaultSpeed(settingsObj.getDouble("defaultPlaybackSpeed").toFloat())
                    if (settingsObj.has("amoledMode")) settingsManager.updateAmoledMode(settingsObj.getBoolean("amoledMode"))
                    if (settingsObj.has("incognitoMode")) settingsManager.updateIncognitoMode(settingsObj.getBoolean("incognitoMode"))
                }
            }

            var importedCount = 0
            var mergedCount = 0

            if (mode == "REPLACE") {
                watchProgressDao.clearAllHistory()
                animeDao.clearWatchlist()
            }

            for (i in 0 until animeArray.length()) {
                val obj = animeArray.getJSONObject(i)
                val id = obj.getString("id")
                val isFav = obj.optBoolean("isFavorite", false)
                val inWatchlist = obj.optBoolean("isInWatchlist", false)
                val isComp = obj.optBoolean("isCompleted", false)
                val watchStatusRaw = obj.optString("watchStatus", if (isComp) "COMPLETED" else "WATCHING")
                val normalizedStatus = WatchStatus.fromRaw(watchStatusRaw).name

                val existing = animeDao.getAnimeByIdOnce(id)
                if (existing != null) {
                    animeDao.insert(
                        existing.copy(
                            isFavorite = if (mode == "REPLACE") isFav else (existing.isFavorite || isFav),
                            isInWatchlist = if (mode == "REPLACE") inWatchlist else (existing.isInWatchlist || inWatchlist),
                            isCompleted = if (mode == "REPLACE") isComp else (existing.isCompleted || isComp),
                            watchStatus = if (inWatchlist || isComp) normalizedStatus else existing.watchStatus,
                            lastUpdated = obj.optLong("lastUpdated", existing.lastUpdated)
                        )
                    )
                    mergedCount++
                } else if (isFav || inWatchlist || isComp) {
                    val inserted = Anime(
                        id = id,
                        malId = obj.optString("malId", ""),
                        title = obj.optString("title", "Anime"),
                        englishTitle = obj.optString("englishTitle", obj.optString("title", "")),
                        romajiTitle = obj.optString("romajiTitle", obj.optString("nativeTitle", "")),
                        nativeTitle = obj.optString("nativeTitle", ""),
                        coverImage = obj.optString("coverImage", ""),
                        bannerImage = obj.optString("bannerImage", ""),
                        synopsis = obj.optString("synopsis", "Restored from backup"),
                        rating = obj.optDouble("rating", 8.0),
                        year = obj.optInt("year", 2024),
                        status = obj.optString("status", "RELEASING"),
                        type = obj.optString("type", "TV"),
                        totalEpisodes = obj.optInt("totalEpisodes", 12),
                        genres = obj.optString("genres", "Anime"),
                        studio = obj.optString("studio", "Studio"),
                        isFavorite = isFav,
                        isInWatchlist = inWatchlist,
                        isCompleted = isComp,
                        watchStatus = normalizedStatus,
                        lastUpdated = obj.optLong("lastUpdated", System.currentTimeMillis())
                    )
                    animeDao.insert(inserted)
                    ensureEpisodesGenerated(inserted)
                    mergedCount++
                }
            }

            for (i in 0 until progressArray.length()) {
                val obj = progressArray.getJSONObject(i)
                val id = obj.getString("id")
                val animeId = obj.getString("animeId")
                val epNum = obj.getInt("episodeNumber")
                val epTitle = obj.optString("episodeTitle", "Episode $epNum")
                val animeTitle = obj.optString("animeTitle", "Anime")
                val coverImage = obj.optString("coverImage", "")
                val curTime = obj.optLong("currentTimeSec", 0L)
                val dur = obj.optLong("durationSec", 1440L)
                val lang = obj.optString("language", "sub")
                val comp = obj.optBoolean("completed", false)
                val lastWatched = obj.optLong("lastWatchedAt", System.currentTimeMillis())

                val existingProgress = watchProgressDao.getProgressById(id)
                if (existingProgress == null || lastWatched >= existingProgress.lastWatchedAt) {
                    val p = WatchProgress(
                        id = id,
                        animeId = animeId,
                        episodeNumber = epNum,
                        episodeTitle = epTitle,
                        animeTitle = animeTitle,
                        coverImage = coverImage,
                        currentTimeSec = curTime,
                        durationSec = dur,
                        language = lang,
                        completed = comp,
                        lastWatchedAt = lastWatched
                    )
                    watchProgressDao.saveProgress(p)
                    importedCount++
                }
            }

            "Successfully processed $importedCount records (Merged: $mergedCount)"
        } catch (e: Exception) {
            "Import failed: ${e.localizedMessage ?: "Invalid JSON format"}"
        }
    }

    suspend fun resetAllData() = withContext(Dispatchers.IO) {
        dismissedContinueWatchingAnimeIds.clear()
        dismissedProgressIds.clear()
        scrobbledEpisodes.clear()
        lastDiscordRpcAt.clear()
        downloadManager.clearAllDownloadFiles()
        watchProgressDao.clearAllHistory()
        downloadDao.clearDownloads()
        notificationDao.clearAll()
        val all = animeDao.getAllAnime().first()
        all.forEach {
            animeDao.updateFavorite(it.id, false)
            animeDao.updateWatchlist(it.id, false)
            animeDao.updateCompleted(it.id, false)
        }
        settingsManager.resetSettings()
    }

    suspend fun freshStartWipe() = withContext(Dispatchers.IO) {
        ensureCatalogPopulated()
    }
}
