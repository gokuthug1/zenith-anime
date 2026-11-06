package com.example.data.network

import com.example.data.model.Anime
import com.example.data.model.AnimeRelation
import com.example.data.model.CharacterVa
import com.example.data.model.MalReview
import com.example.data.util.SeasonLabeler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class AniListService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    companion object {
        private const val MEDIA_FIELDS = """
            id
            idMal
            title {
              romaji
              english
              native
            }
            synonyms
            coverImage {
              large
              extraLarge
            }
            bannerImage
            description
            averageScore
            status
            episodes
            seasonYear
            format
            isAdult
            genres
            studios(isMain: true) {
              nodes {
                name
              }
            }
            nextAiringEpisode {
              airingAt
              timeUntilAiring
              episode
            }
        """
    }

    suspend fun searchAnime(query: String): List<Anime> = withContext(Dispatchers.IO) {
        val graphQL = """
            query (${'$'}search: String) {
              Page(page: 1, perPage: 25) {
                media(search: ${'$'}search, type: ANIME, sort: POPULARITY_DESC) {
                  $MEDIA_FIELDS
                }
              }
            }
        """.trimIndent()

        val jsonBody = JSONObject().apply {
            put("query", graphQL)
            put("variables", JSONObject().apply {
                put("search", query)
            })
        }

        executeGraphqlQuery(jsonBody)
    }

    suspend fun searchAnimeFiltered(
        query: String? = null,
        format: String? = null,
        status: String? = null,
        season: String? = null,
        seasonYear: Int? = null,
        sort: String? = null,
        genre: String? = null,
        isAdult: Boolean? = false
    ): List<Anime> = withContext(Dispatchers.IO) {
        val graphQL = """
            query (
                ${'$'}search: String,
                ${'$'}format: MediaFormat,
                ${'$'}status: MediaStatus,
                ${'$'}season: MediaSeason,
                ${'$'}seasonYear: Int,
                ${'$'}sort: [MediaSort],
                ${'$'}genre: String,
                ${'$'}isAdult: Boolean
            ) {
              Page(page: 1, perPage: 30) {
                media(
                    search: ${'$'}search,
                    type: ANIME,
                    format: ${'$'}format,
                    status: ${'$'}status,
                    season: ${'$'}season,
                    seasonYear: ${'$'}seasonYear,
                    sort: ${'$'}sort,
                    genre: ${'$'}genre,
                    isAdult: ${'$'}isAdult
                ) {
                  $MEDIA_FIELDS
                }
              }
            }
        """.trimIndent()

        val variables = JSONObject().apply {
            if (!query.isNullOrBlank()) put("search", query)
            if (!format.isNullOrBlank() && format != "All") put("format", format.uppercase())
            if (!status.isNullOrBlank() && status != "All") {
                val s = when (status.lowercase()) {
                    "finished airing", "finished" -> "FINISHED"
                    "currently airing", "releasing" -> "RELEASING"
                    "not yet aired" -> "NOT_YET_RELEASED"
                    else -> status.uppercase()
                }
                put("status", s)
            }
            if (!season.isNullOrBlank() && season != "All") put("season", season.uppercase())
            if (seasonYear != null && seasonYear > 1900) put("seasonYear", seasonYear)

            val isExplicit18PlusChip = genre.equals("18+", ignoreCase = true)
            val isHentaiGenre = genre.equals("Hentai", ignoreCase = true)
            if (!genre.isNullOrBlank() && genre != "All" && !isExplicit18PlusChip) {
                put("genre", genre)
            }

            val effectiveIsAdult: Boolean? = when {
                isExplicit18PlusChip || isHentaiGenre -> true
                else -> isAdult
            }
            if (effectiveIsAdult != null) {
                put("isAdult", effectiveIsAdult)
            }

            val sortVal = when (sort?.lowercase()) {
                "popularity" -> "POPULARITY_DESC"
                "score" -> "SCORE_DESC"
                "recently added", "trending" -> "TRENDING_DESC"
                "newest", "release date" -> "START_DATE_DESC"
                "title a-z" -> "TITLE_ROMAJI"
                else -> "POPULARITY_DESC"
            }
            put("sort", org.json.JSONArray().apply { put(sortVal) })
        }

        val jsonBody = JSONObject().apply {
            put("query", graphQL)
            put("variables", variables)
        }

        executeGraphqlQuery(jsonBody)
    }

    suspend fun getRandomAnime(): Anime? = withContext(Dispatchers.IO) {
        val randomPage = Random.nextInt(1, 30)
        val graphQL = """
            query (${'$'}page: Int) {
              Page(page: ${'$'}page, perPage: 10) {
                media(type: ANIME, sort: POPULARITY_DESC) {
                  $MEDIA_FIELDS
                }
              }
            }
        """.trimIndent()

        val jsonBody = JSONObject().apply {
            put("query", graphQL)
            put("variables", JSONObject().apply {
                put("page", randomPage)
            })
        }

        val list = executeGraphqlQuery(jsonBody)
        if (list.isNotEmpty()) list.random() else null
    }

    suspend fun getTrendingAnime(): List<Anime> = withContext(Dispatchers.IO) {
        val graphQL = """
            query {
              Page(page: 1, perPage: 20) {
                media(type: ANIME, sort: TRENDING_DESC) {
                  $MEDIA_FIELDS
                }
              }
            }
        """.trimIndent()

        executeGraphqlQuery(JSONObject().apply { put("query", graphQL) })
    }

    suspend fun getPopularAnime(): List<Anime> = withContext(Dispatchers.IO) {
        val graphQL = """
            query {
              Page(page: 1, perPage: 20) {
                media(type: ANIME, sort: POPULARITY_DESC) {
                  $MEDIA_FIELDS
                }
              }
            }
        """.trimIndent()

        executeGraphqlQuery(JSONObject().apply { put("query", graphQL) })
    }

    suspend fun getTopRatedAnime(): List<Anime> = withContext(Dispatchers.IO) {
        val graphQL = """
            query {
              Page(page: 1, perPage: 20) {
                media(type: ANIME, sort: SCORE_DESC) {
                  $MEDIA_FIELDS
                }
              }
            }
        """.trimIndent()

        executeGraphqlQuery(JSONObject().apply { put("query", graphQL) })
    }

    suspend fun getAnimeByGenre(genre: String): List<Anime> = withContext(Dispatchers.IO) {
        val isExplicit18Plus = genre.equals("18+", ignoreCase = true)
        val isHentai = genre.equals("Hentai", ignoreCase = true)
        val graphQL = """
            query (${'$'}genre: String, ${'$'}isAdult: Boolean) {
              Page(page: 1, perPage: 20) {
                media(type: ANIME, genre: ${'$'}genre, isAdult: ${'$'}isAdult, sort: POPULARITY_DESC) {
                  $MEDIA_FIELDS
                }
              }
            }
        """.trimIndent()

        val jsonBody = JSONObject().apply {
            put("query", graphQL)
            put("variables", JSONObject().apply {
                if (!isExplicit18Plus) put("genre", genre)
                put("isAdult", isExplicit18Plus || isHentai)
            })
        }

        executeGraphqlQuery(jsonBody)
    }

    suspend fun getAiringSchedule(startSec: Long, endSec: Long): List<com.example.data.model.AiringScheduleItem> = withContext(Dispatchers.IO) {
        val graphQL = """
            query (${'$'}start: Int, ${'$'}end: Int) {
              Page(page: 1, perPage: 50) {
                airingSchedules(airingAt_greater: ${'$'}start, airingAt_lesser: ${'$'}end, sort: TIME) {
                  id
                  airingAt
                  timeUntilAiring
                  episode
                  media {
                    $MEDIA_FIELDS
                  }
                }
              }
            }
        """.trimIndent()

        val variables = JSONObject().apply {
            put("start", startSec)
            put("end", endSec)
        }

        val jsonBody = JSONObject().apply {
            put("query", graphQL)
            put("variables", variables)
        }

        try {
            val request = Request.Builder()
                .url("https://graphql.anilist.co")
                .post(jsonBody.toString().toRequestBody(jsonMediaType))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .build()

            val bodyString = client.newCall(request).execute().use { response ->
                response.body?.string()
            } ?: return@withContext emptyList()
            val root = JSONObject(bodyString)
            val data = root.optJSONObject("data") ?: return@withContext emptyList()
            val page = data.optJSONObject("Page") ?: return@withContext emptyList()
            val schedulesArray = page.optJSONArray("airingSchedules") ?: return@withContext emptyList()

            val results = mutableListOf<com.example.data.model.AiringScheduleItem>()
            for (i in 0 until schedulesArray.length()) {
                val item = schedulesArray.getJSONObject(i)
                val id = item.optInt("id", 0)
                val airingAt = item.optLong("airingAt", 0L)
                val timeUntil = item.optLong("timeUntilAiring", 0L)
                val episode = item.optInt("episode", 1)
                val mediaObj = item.optJSONObject("media") ?: continue
                val anime = parseMediaItem(mediaObj) ?: continue
                results.add(
                    com.example.data.model.AiringScheduleItem(
                        id = id,
                        airingAt = airingAt,
                        timeUntilAiring = timeUntil,
                        episode = episode,
                        anime = anime
                    )
                )
            }
            results
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun getAnimeDetailsWithRelations(animeId: String): Pair<Anime?, List<AnimeRelation>> = withContext(Dispatchers.IO) {
        val isMalIdLookup = animeId.startsWith("mal_")
        val numericId = if (isMalIdLookup) animeId.removePrefix("mal_").toIntOrNull() else animeId.toIntOrNull()
        if (numericId == null) return@withContext Pair(null, emptyList())

        val filterArg = if (isMalIdLookup) "idMal: ${'$'}id" else "id: ${'$'}id"
        val graphQL = """
            query (${'$'}id: Int) {
              Media($filterArg, type: ANIME) {
                $MEDIA_FIELDS
                relations {
                  edges {
                    relationType
                    node {
                      id
                      title {
                        romaji
                        english
                      }
                      coverImage {
                        large
                      }
                      format
                    }
                  }
                }
              }
            }
        """.trimIndent()

        try {
            val jsonBody = JSONObject().apply {
                put("query", graphQL)
                put("variables", JSONObject().apply {
                    put("id", numericId)
                })
            }

            val request = Request.Builder()
                .url("https://graphql.anilist.co")
                .post(jsonBody.toString().toRequestBody(jsonMediaType))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .build()

            val bodyString = client.newCall(request).execute().use { response ->
                response.body?.string()
            } ?: return@withContext Pair(null, emptyList())
            val root = JSONObject(bodyString)
            val data = root.optJSONObject("data") ?: return@withContext Pair(null, emptyList())
            val mediaObj = data.optJSONObject("Media") ?: return@withContext Pair(null, emptyList())

            val parsed = parseMediaItem(mediaObj)
            val anime = if (isMalIdLookup && parsed != null) {
                parsed.copy(id = animeId)
            } else parsed
            val relations = mutableListOf<AnimeRelation>()

            val relationsObj = mediaObj.optJSONObject("relations")
            val edges = relationsObj?.optJSONArray("edges")
            if (edges != null) {
                for (i in 0 until edges.length()) {
                    val edge = edges.getJSONObject(i)
                    val relType = edge.optString("relationType", "RELATED").replace("_", " ")
                    val node = edge.optJSONObject("node") ?: continue
                    val relId = node.getInt("id").toString()
                    val relTitleObj = node.optJSONObject("title")
                    val relTitle = relTitleObj?.optString("english")?.takeIf { it.isNotEmpty() && it != "null" }
                        ?: relTitleObj?.optString("romaji")?.takeIf { it.isNotEmpty() && it != "null" }
                        ?: "Related Anime"
                    val relCover = node.optJSONObject("coverImage")?.optString("large", "") ?: ""
                    val relFormat = node.optString("format", "TV")

                    relations.add(
                        AnimeRelation(
                            id = relId,
                            title = relTitle,
                            coverImage = relCover,
                            relationType = relType,
                            format = relFormat,
                            isCurrent = relId == animeId
                        )
                    )
                }
            }

            // Consumer-friendly wording: "Season 2" / "Part 3" instead of raw AniList words such
            // as ADAPTATION or SEQUEL. The UI can override the style from the user's preference.
            val labelled = relations.map { relation ->
                relation.copy(
                    seasonNumber = SeasonLabeler.seasonNumberFromTitle(relation.title),
                    friendlyLabel = SeasonLabeler.seasonLabel(
                        relation.title,
                        relation.relationType,
                        relation.format,
                        "Season"
                    )
                )
            }

            Pair(anime, labelled)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Pair(null, emptyList())
        }
    }

    suspend fun getAnimeCharacters(animeId: String): List<CharacterVa> = withContext(Dispatchers.IO) {
        try {
            val isMalIdLookup = animeId.startsWith("mal_")
            val aniId = if (isMalIdLookup) animeId.removePrefix("mal_").toIntOrNull() else animeId.toIntOrNull()
            if (aniId == null) return@withContext emptyList()
            val filterArg = if (isMalIdLookup) "idMal: ${'$'}id" else "id: ${'$'}id"
            val graphQL = """
                query (${'$'}id: Int) {
                  Media($filterArg, type: ANIME) {
                    characters(page: 1, perPage: 16, sort: [ROLE, RELEVANCE]) {
                      edges {
                        role
                        node {
                          id
                          name {
                            full
                          }
                          image {
                            large
                          }
                        }
                        voiceActors(language: JAPANESE) {
                          name {
                            full
                          }
                          image {
                            large
                          }
                          languageV2
                        }
                      }
                    }
                  }
                }
            """.trimIndent()

            val jsonBody = JSONObject().apply {
                put("query", graphQL)
                put("variables", JSONObject().apply {
                    put("id", aniId)
                })
            }

            val request = Request.Builder()
                .url("https://graphql.anilist.co")
                .post(jsonBody.toString().toRequestBody(jsonMediaType))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .build()

            val body = client.newCall(request).execute().use { response ->
                response.body?.string()
            } ?: return@withContext emptyList()
            val root = JSONObject(body)
            val data = root.optJSONObject("data") ?: return@withContext emptyList()
            val media = data.optJSONObject("Media") ?: return@withContext emptyList()
            val charactersObj = media.optJSONObject("characters") ?: return@withContext emptyList()
            val edges = charactersObj.optJSONArray("edges") ?: return@withContext emptyList()

            val list = mutableListOf<CharacterVa>()
            for (i in 0 until edges.length()) {
                val edge = edges.optJSONObject(i) ?: continue
                val role = edge.optString("role", "MAIN")
                val node = edge.optJSONObject("node") ?: continue
                val charId = node.optInt("id", i).toString()
                val charName = node.optJSONObject("name")?.optString("full", "Character") ?: "Character"
                val charImage = node.optJSONObject("image")?.optString("large", "") ?: ""

                val vas = edge.optJSONArray("voiceActors")
                val primaryVa = if (vas != null && vas.length() > 0) vas.optJSONObject(0) else null
                val vaName = primaryVa?.optJSONObject("name")?.optString("full", "") ?: ""
                val vaImage = primaryVa?.optJSONObject("image")?.optString("large", "") ?: ""
                val vaLang = primaryVa?.optString("languageV2", "Japanese") ?: "Japanese"

                list.add(
                    CharacterVa(
                        id = charId,
                        characterName = charName,
                        characterImage = charImage,
                        role = role,
                        vaName = vaName,
                        vaImage = vaImage,
                        vaLanguage = vaLang
                    )
                )
            }
            list
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Fetches MyAnimeList user reviews for the given anime (by MAL ID, or resolves the MAL ID via
     * AniList first). Falls back to AniList community reviews if Jikan is rate-limited or has no
     * entries for the show.
     */
    suspend fun getMalReviews(animeId: String, malId: String): List<MalReview> = withContext(Dispatchers.IO) {
        var resolvedMalId = malId.trim().takeIf { it.isNotEmpty() && it != "0" }
        if (resolvedMalId == null && animeId.startsWith("mal_")) {
            resolvedMalId = animeId.removePrefix("mal_").trim().takeIf { it.isNotEmpty() && it != "0" }
        }
        val jikanReviews = if (resolvedMalId != null) fetchJikanReviews(resolvedMalId) else emptyList()
        val aniListReviews = fetchAniListReviewsFallback(animeId, resolvedMalId)
        val combined = (jikanReviews + aniListReviews).distinctBy { it.id }
        combined.ifEmpty { aniListReviews }
    }

    private fun decodeReviewHtmlEntities(input: String): String {
        return input
            .replace("\\r\\n", "\n")
            .replace("\\n", "\n")
            .replace("\r\n", "\n")
            .replace("&quot;", "\"")
            .replace("&#039;", "'")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&rsquo;", "’")
            .replace("&lsquo;", "‘")
            .replace("&rdquo;", "”")
            .replace("&ldquo;", "“")
            .replace("&mdash;", "—")
            .replace("&ndash;", "–")
            .replace("&hellip;", "…")
            .replace("&nbsp;", " ")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
    }

    internal fun parseJikanReviewsJson(jsonString: String): List<MalReview> {
        return try {
            val root = JSONObject(jsonString)
            val data = root.optJSONArray("data") ?: return emptyList()
            val out = mutableListOf<MalReview>()
            for (i in 0 until data.length()) {
                val item = data.optJSONObject(i) ?: continue
                val id = item.optInt("mal_id", i + 1).toString()
                val userObj = item.optJSONObject("user")
                val author = userObj?.optString("username")?.takeIf { it.isNotBlank() } ?: "MAL Reviewer"
                val avatar = userObj?.optJSONObject("images")
                    ?.optJSONObject("jpg")
                    ?.optString("image_url")
                    .orEmpty()
                val score = item.optInt("score", 8).coerceIn(1, 10)
                val rawReview = item.optString("review", "").trim()
                if (rawReview.isBlank()) continue
                val cleanBody = decodeReviewHtmlEntities(rawReview).trim()
                val plainSummarySource = cleanBody
                    .replace(Regex("\\[/?[a-zA-Z0-9= _\"'-]+\\]"), "")
                    .replace(Regex("img\\d*%?\\([^)]*\\)", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("!\\[[^\\]]*\\]\\([^)]*\\)"), "")
                    .replace("~~~", "")
                val firstSentence = plainSummarySource.lineSequence().firstOrNull { it.isNotBlank() }
                    ?.trim()
                    ?.take(120)
                    .orEmpty()
                val rawDate = item.optString("date", "")
                val date = if (rawDate.length >= 10) rawDate.substring(0, 10) else rawDate
                val isSpoiler = item.optBoolean("is_spoiler", false)
                val isPreliminary = item.optBoolean("is_preliminary", false)
                val episodesWatched = item.optInt("episodes_watched", 0)
                val reactions = item.optJSONObject("reactions")
                val helpful = reactions?.optInt("overall", 0)
                    ?: reactions?.optInt("nice", 0)
                    ?: 0

                out += MalReview(
                    id = "mal_rev_$id",
                    author = author,
                    avatarUrl = avatar,
                    score = score,
                    summary = firstSentence,
                    body = cleanBody,
                    date = date,
                    isSpoiler = isSpoiler,
                    isPreliminary = isPreliminary,
                    episodesWatched = episodesWatched,
                    helpfulCount = helpful,
                    source = "MyAnimeList"
                )
            }
            out
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun fetchJikanReviews(malId: String): List<MalReview> {
        return try {
            val request = Request.Builder()
                .url("https://api.jikan.moe/v4/anime/$malId/reviews?preliminary=true&spoilers=true")
                .header("Accept", "application/json")
                .header("User-Agent", "ZenithAnime/2.0")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val body = response.body?.string() ?: return emptyList()
                parseJikanReviewsJson(body)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun fetchAniListReviewsFallback(animeId: String, malId: String?): List<MalReview> {
        return try {
            val isMalLookup = animeId.startsWith("mal_") || (animeId.toIntOrNull() == null && !malId.isNullOrBlank())
            val numericId = if (isMalLookup) {
                (malId ?: animeId.removePrefix("mal_")).toIntOrNull()
            } else {
                animeId.toIntOrNull()
            } ?: return emptyList()

            val filterArg = if (isMalLookup) "idMal: ${'$'}id" else "id: ${'$'}id"
            val query = """
                query (${'$'}id: Int) {
                  Media($filterArg, type: ANIME) {
                    reviews(sort: RATING_DESC, perPage: 10) {
                      nodes {
                        id
                        score
                        rating
                        summary
                        body
                        createdAt
                        user {
                          name
                          avatar {
                            large
                          }
                        }
                      }
                    }
                  }
                }
            """.trimIndent()

            val jsonBody = JSONObject().apply {
                put("query", query)
                put("variables", JSONObject().apply { put("id", numericId) })
            }
            val request = Request.Builder()
                .url("https://graphql.anilist.co")
                .post(jsonBody.toString().toRequestBody(jsonMediaType))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: return emptyList()
                val nodes = JSONObject(body)
                    .optJSONObject("data")
                    ?.optJSONObject("Media")
                    ?.optJSONObject("reviews")
                    ?.optJSONArray("nodes")
                    ?: return emptyList()

                val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                val out = mutableListOf<MalReview>()
                for (i in 0 until nodes.length()) {
                    val node = nodes.optJSONObject(i) ?: continue
                    val id = node.optInt("id", i + 1).toString()
                    val user = node.optJSONObject("user")
                    val author = user?.optString("name")?.takeIf { it.isNotBlank() } ?: "Reviewer"
                    val avatar = user?.optJSONObject("avatar")?.optString("large").orEmpty()
                    val rawScore = node.optInt("score", 80)
                    val score10 = (rawScore / 10).coerceIn(1, 10)
                    val summary = decodeReviewHtmlEntities(node.optString("summary", "")).trim()
                    val rawBody = decodeReviewHtmlEntities(node.optString("body", ""))
                        .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
                        .replace(Regex("<hr\\s*/?>", RegexOption.IGNORE_CASE), "\n---\n")
                        .replace(Regex("</p>", RegexOption.IGNORE_CASE), "\n\n")
                        .replace(Regex("<img[^>]+src=[\"'](https?://[^\"']+)[\"'][^>]*>", RegexOption.IGNORE_CASE), "\n![]($1)\n")
                        .trim()
                    if (rawBody.isBlank() && summary.isBlank()) continue
                    val createdAtSec = node.optLong("createdAt", 0L)
                    val dateStr = if (createdAtSec > 0L) {
                        try { dateFormat.format(java.util.Date(createdAtSec * 1000L)) } catch (_: Exception) { "" }
                    } else ""
                    val rating = node.optInt("rating", 0)
                    out += MalReview(
                        id = "ani_rev_$id",
                        author = author,
                        avatarUrl = avatar,
                        score = score10,
                        summary = summary,
                        body = rawBody.ifBlank { summary },
                        date = dateStr,
                        isSpoiler = false,
                        isPreliminary = false,
                        episodesWatched = 0,
                        helpfulCount = rating,
                        source = "AniList"
                    )
                }
                out
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun executeGraphqlQuery(jsonBody: JSONObject): List<Anime> {
        return try {
            val request = Request.Builder()
                .url("https://graphql.anilist.co")
                .post(jsonBody.toString().toRequestBody(jsonMediaType))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .build()

            val bodyString = client.newCall(request).execute().use { response ->
                response.body?.string()
            } ?: return emptyList()
            parseAniListResponse(bodyString)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseAniListResponse(jsonStr: String): List<Anime> {
        val results = mutableListOf<Anime>()
        try {
            val root = JSONObject(jsonStr)
            val data = root.optJSONObject("data") ?: return emptyList()
            val page = data.optJSONObject("Page") ?: return emptyList()
            val mediaArray = page.optJSONArray("media") ?: return emptyList()

            for (i in 0 until mediaArray.length()) {
                val item = mediaArray.getJSONObject(i)
                parseMediaItem(item)?.let { results.add(it) }
            }
        } catch (_: Exception) {}
        return results
    }

    private fun parseMediaItem(item: JSONObject): Anime? {
        return try {
            val id = item.getInt("id").toString()
            val malId = item.optInt("idMal", 0).toString()

            val titleObj = item.optJSONObject("title")
            val englishRaw = titleObj?.optString("english")?.takeIf { it.isNotEmpty() && it != "null" }
            val romaji = titleObj?.optString("romaji")?.takeIf { it.isNotEmpty() && it != "null" }
            val native = titleObj?.optString("native")?.takeIf { it.isNotEmpty() && it != "null" } ?: ""

            val synonymsArray = item.optJSONArray("synonyms")
            val synonymsList = mutableListOf<String>()
            if (synonymsArray != null) {
                for (s in 0 until synonymsArray.length()) {
                    val syn = synonymsArray.optString(s, "").trim()
                    if (syn.isNotEmpty()) synonymsList.add(syn)
                }
            }

            // If english title is missing, or is identical/very close to romaji, prefer a distinct English synonym (e.g. "Heavenly Delusion")
            val english = if (englishRaw == null || (romaji != null && isSimilarToRomaji(englishRaw, romaji))) {
                val candidateSynonym = synonymsList.firstOrNull { syn ->
                    syn.all { it.code < 128 } && !isSimilarToRomaji(syn, romaji ?: "")
                }
                candidateSynonym ?: englishRaw
            } else {
                englishRaw
            }
            val mainTitle = english ?: romaji ?: "Anime Title"

            val coverObj = item.optJSONObject("coverImage")
            val cover = coverObj?.optString("extraLarge")?.takeIf { it.isNotEmpty() && it != "null" }
                ?: coverObj?.optString("large")?.takeIf { it.isNotEmpty() && it != "null" } ?: ""

            val banner = item.optString("bannerImage").takeIf { it.isNotEmpty() && it != "null" } ?: cover

            val rawDesc = item.optString("description", "")
            val cleanDesc = rawDesc.replace(Regex("<[^>]*>"), "")

            val score = item.optDouble("averageScore", 80.0) / 10.0
            val status = item.optString("status", "RELEASING")
            val hasEpisodesField = !item.isNull("episodes") && item.has("episodes")
            val rawEpCount = if (hasEpisodesField) item.optInt("episodes", 0) else null

            val year = item.optInt("seasonYear", 2024)
            val format = item.optString("format", "TV")
            val isAdult = item.optBoolean("isAdult", false)

            val genresArray = item.optJSONArray("genres")
            val genreList = mutableListOf<String>()
            if (genresArray != null) {
                for (g in 0 until genresArray.length()) {
                    genreList.add(genresArray.getString(g))
                }
            }
            if (isAdult && genreList.none { it.equals("18+", ignoreCase = true) }) {
                genreList.add("18+")
            }
            val genresStr = if (genreList.isNotEmpty()) genreList.joinToString(", ") else "Anime"

            val studiosObj = item.optJSONObject("studios")
            val studiosNodes = studiosObj?.optJSONArray("nodes")
            val studio = if (studiosNodes != null && studiosNodes.length() > 0) {
                studiosNodes.getJSONObject(0).optString("name", "Studio")
            } else "Studio"

            val nextAiringObj = item.optJSONObject("nextAiringEpisode")
            val nextEp = nextAiringObj?.optInt("episode")?.takeIf { it > 0 }
            val nextSec = nextAiringObj?.optLong("timeUntilAiring")?.takeIf { it > 0 }
            val nextAt = nextAiringObj?.optLong("airingAt")?.takeIf { it > 0 }

            val epCount = when {
                rawEpCount != null && rawEpCount > 0 -> rawEpCount
                nextEp != null -> (nextEp - 1).coerceAtLeast(0)
                status.equals("FINISHED", ignoreCase = true) -> 12
                else -> 0
            }

            Anime(
                id = id,
                malId = malId,
                title = mainTitle,
                englishTitle = english ?: "",
                romajiTitle = romaji ?: "",
                nativeTitle = native,
                coverImage = cover,
                bannerImage = banner,
                synopsis = cleanDesc.ifEmpty { "No synopsis available." },
                rating = if (score > 0) score else 8.0,
                ageRating = when {
                    isAdult || genresStr.contains("Hentai", ignoreCase = true) -> "18+"
                    genresStr.contains("Ecchi", ignoreCase = true) || genresStr.contains("Horror", ignoreCase = true) -> "R"
                    else -> "PG-13"
                },
                quality = "HD",
                year = year,
                season = "$year",
                status = status,
                type = format,
                totalEpisodes = epCount,
                genres = genresStr,
                studio = studio,
                scheduleDay = "Sun",
                scheduleTime = "20:00",
                isFavorite = false,
                isInWatchlist = false,
                isCompleted = false,
                nextAiringEpisode = nextEp,
                nextAiringTimeUntilSec = nextSec,
                nextAiringAtTimestamp = nextAt
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun isSimilarToRomaji(a: String, b: String): Boolean {
        if (a.isBlank() || b.isBlank()) return false
        val normA = a.lowercase().filter { it.isLetterOrDigit() }.replace("ou", "o").replace("oo", "o").replace("uu", "u")
        val normB = b.lowercase().filter { it.isLetterOrDigit() }.replace("ou", "o").replace("oo", "o").replace("uu", "u")
        return normA == normB
    }
}
