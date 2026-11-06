package com.example.ui.details

import android.widget.Toast
import com.example.data.util.ShareUtils
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.ripple
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Episode
import com.example.data.repository.AnimeRepository
import com.example.ui.components.AgeRatingBadge
import com.example.ui.components.QualityBadge
import com.example.ui.components.NextAiringBadge
import com.example.ui.components.CurrentSeasonBadge
import com.example.ui.components.AnimeCard
import com.example.ui.components.AnimeQuickActionDialog
import com.example.ui.components.EpisodeQuickActionDialog
import com.example.ui.components.OfflineBadge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.LaunchedEffect
import com.example.data.model.Anime
import com.example.data.model.AnimeRelation
import com.example.data.model.AppSettings
import com.example.data.model.CharacterVa
import com.example.data.model.MalReview
import com.example.data.model.WatchStatus
import androidx.compose.material.icons.filled.GridView
import androidx.compose.runtime.mutableIntStateOf
import com.example.data.util.SeasonLabeler
import com.example.ui.theme.LocalThemeAccent
import com.example.ui.theme.LocalAppSettings
import com.example.ui.theme.AccentYellow
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

/**
 * Wording shown under a related entry.
 * Defaults to consumer spring ("Season 2") and can never surface the raw word "Adaptation".
 */
private fun seasonLabelFor(relation: AnimeRelation, settings: AppSettings): String {
    return when {
        settings.friendlySeasonLabels -> SeasonLabeler.seasonLabel(
            relation.title,
            relation.relationType,
            relation.format,
            settings.seasonLabelStyle
        ).ifBlank { relation.friendlyLabel }
        settings.hideAdaptationLabel -> SeasonLabeler.friendlyRelationLabel(relation.relationType)
        else -> relation.relationType
    }
}

/** Episode presentation styles; these strings match [AppSettings.episodeLayout]. */
private const val EPISODE_LAYOUT_CARDS = "Cards"
private const val EPISODE_LAYOUT_NUMBERS = "Numbers"
private const val EPISODE_LAYOUT_GRID = "Grid"
private val EPISODE_LAYOUTS =
    listOf(EPISODE_LAYOUT_CARDS, EPISODE_LAYOUT_NUMBERS, EPISODE_LAYOUT_GRID)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AnimeDetailsScreen(
    animeId: String,
    repository: AnimeRepository,
    onBackClick: () -> Unit,
    onPlayEpisode: (String, Int, String) -> Unit,
    onRelatedAnimeClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val anime by repository.getAnimeById(animeId).collectAsState(initial = null)
    val episodes by repository.getEpisodes(animeId).collectAsState(initial = emptyList())
    val allAnime by repository.allAnime.collectAsState(initial = emptyList())
    val latestProgress by repository.observeLatestProgress(animeId).collectAsState(initial = null)
    val allProgress by repository.allHistory.collectAsState(initial = emptyList())
    val themeAccent = LocalThemeAccent.current
    val detailsSettings by repository.settings.collectAsState(initial = repository.settingsManager.getSettings())

    var selectedAudioTrack by remember(detailsSettings.defaultLanguage) { mutableStateOf(detailsSettings.defaultLanguage.ifBlank { "sub" }) }
    var isExpandedSynopsis by remember { mutableStateOf(false) }
    var activeEpisodeForActions by remember { mutableStateOf<Episode?>(null) }
    var longPressedAnimeForActions by remember { mutableStateOf<com.example.data.model.Anime?>(null) }
    var episodeSearchQuery by remember { mutableStateOf("") }
    var selectedRangeIndex by remember(episodeSearchQuery) { mutableIntStateOf(0) }

    // Episode presentation is a persisted preference, so the choice survives visits and stays in
    // sync with the setting shown in Library & Series settings.
    val episodeLayout = detailsSettings.episodeLayout
    fun setEpisodeLayout(next: String) {
        repository.settingsManager.update { it.copy(episodeLayout = next) }
    }

    var selectedDetailTabIndex by remember { mutableIntStateOf(0) }
    var animeRelations by remember { mutableStateOf<List<AnimeRelation>>(emptyList()) }
    var malReviews by remember { mutableStateOf<List<MalReview>>(emptyList()) }
    var charactersList by remember { mutableStateOf<List<CharacterVa>>(emptyList()) }
    var themesList by remember { mutableStateOf<List<com.example.data.model.AnimeTheme>>(emptyList()) }
    var isLoadingReviews by remember { mutableStateOf(false) }
    var isLoadingCharacters by remember { mutableStateOf(false) }
    var isLoadingThemes by remember { mutableStateOf(false) }
    var activeThemeToPlay by remember { mutableStateOf<com.example.data.model.AnimeTheme?>(null) }
    var showBatchDownloadDialog by remember { mutableStateOf(false) }
    var reviewsExpanded by remember { mutableStateOf(true) }
    var showWatchStatusMenu by remember { mutableStateOf(false) }

    LaunchedEffect(animeId) {
        isLoadingReviews = true
        isLoadingCharacters = true
        isLoadingThemes = true
        val rels = repository.getAnimeDetailsWithRelations(animeId)
        if (rels.second.isNotEmpty()) {
            animeRelations = rels.second
        }
        val targetMalId = rels.first?.malId ?: anime?.malId.orEmpty()
        malReviews = repository.getMalReviews(animeId, targetMalId)
        isLoadingReviews = false
        charactersList = repository.getAnimeCharacters(animeId)
        isLoadingCharacters = false
        try {
            themesList = repository.getAnimeThemes(animeId, anime?.title ?: "")
        } catch (_: Exception) {}
        isLoadingThemes = false
    }

    val displayedEpisodes = remember(episodes, episodeSearchQuery) {
        if (episodeSearchQuery.isBlank()) episodes
        else {
            val q = episodeSearchQuery.trim()
            episodes.filter {
                it.number.toString().contains(q) || it.title.contains(q, ignoreCase = true)
            }
        }
    }

    val pageSize = 25
    val episodeChunks = remember(displayedEpisodes) {
        if (displayedEpisodes.isEmpty()) emptyList() else displayedEpisodes.chunked(pageSize)
    }
    val currentChunkEpisodes = remember(episodeChunks, selectedRangeIndex, displayedEpisodes) {
        if (episodeChunks.isNotEmpty() && selectedRangeIndex in episodeChunks.indices) {
            episodeChunks[selectedRangeIndex]
        } else displayedEpisodes
    }

    val currentGenres = anime?.genreList?.map { it.lowercase() }?.toSet().orEmpty()
    val relatedAnime = remember(allAnime, anime) {
        allAnime.filter { it.id != animeId }
            .sortedWith(
                compareByDescending<Anime> { it.genreList.count { g -> currentGenres.contains(g.lowercase()) } }
                    .thenByDescending { it.rating }
            )
            .take(8)
    }

    if (anime == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Text("Loading anime details...", color = TextSecondary)
        }
        return
    }

    val currentAnime = anime!!

    if (activeEpisodeForActions != null) {
        val ep = activeEpisodeForActions!!
        EpisodeQuickActionDialog(
            episode = ep,
            onDismiss = { activeEpisodeForActions = null },
            onPlay = {
                onPlayEpisode(currentAnime.id, ep.number, selectedAudioTrack)
            },
            onDownload = {
                scope.launch {
                    repository.addDownload(currentAnime, ep)
                    Toast.makeText(context, "Episode ${ep.number} saved to offline downloads", Toast.LENGTH_SHORT).show()
                }
            },
            onMarkWatched = {
                scope.launch {
                    repository.saveWatchProgress(
                        anime = currentAnime,
                        episode = ep,
                        currentTimeSec = 1440L,
                        durationSec = 1440L,
                        language = selectedAudioTrack,
                        completed = true
                    )
                    Toast.makeText(context, "Episode ${ep.number} marked as watched", Toast.LENGTH_SHORT).show()
                }
            },
            onShare = {
                ShareUtils.shareAnime(
                    context = context,
                    anime = currentAnime,
                    titleLanguagePreference = detailsSettings.titleLanguagePreference,
                    episodeNumber = ep.number
                )
            }
        )
    }

    if (longPressedAnimeForActions != null) {
        val item = longPressedAnimeForActions!!
        AnimeQuickActionDialog(
            anime = item,
            onDismiss = { longPressedAnimeForActions = null },
            onPlay = {
                onPlayEpisode(item.id, 1, detailsSettings.defaultLanguage.ifBlank { "sub" })
            },
            onToggleFavorite = {
                scope.launch {
                    repository.toggleFavorite(item.id, !item.isFavorite)
                }
            },
            onToggleWatchlist = {
                scope.launch {
                    repository.toggleWatchlist(item.id, !item.isInWatchlist, item)
                }
            },
            onToggleCompleted = {
                scope.launch {
                    repository.markCompleted(item.id, !item.isCompleted)
                }
            },
            onChangeWatchStatus = { status ->
                scope.launch {
                    repository.updateWatchStatus(item.id, status.name)
                    Toast.makeText(context, "Moved to ${status.displayName}", Toast.LENGTH_SHORT).show()
                }
            },
            onShare = {
                ShareUtils.shareAnime(
                    context = context,
                    anime = item,
                    titleLanguagePreference = detailsSettings.titleLanguagePreference
                )
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                AsyncImage(
                    model = currentAnime.bannerImage.ifEmpty { currentAnime.coverImage },
                    contentDescription = currentAnime.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x99000000),
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.background
                                )
                            )
                        )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .background(Color(0x66000000), CircleShape)
                            .testTag("details_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0x88000000))
                            .border(0.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                            .clickable {
                                ShareUtils.shareAnime(
                                    context = context,
                                    anime = currentAnime,
                                    titleLanguagePreference = detailsSettings.titleLanguagePreference
                                )
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("details_share_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share Anime",
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Share",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentAnime.displayTitle(detailsSettings.titleLanguagePreference),
                        color = TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )

                    Box {
                        val activeStatus = WatchStatus.fromRaw(currentAnime.watchStatus)
                        if (currentAnime.isInWatchlist) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(themeAccent.copy(alpha = 0.16f))
                                    .border(1.dp, themeAccent.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                                    .clickable { showWatchStatusMenu = true }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("details_watch_status_badge")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Bookmark,
                                        contentDescription = null,
                                        tint = themeAccent,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = activeStatus.displayName,
                                        color = themeAccent,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Icon(
                                        imageVector = Icons.Default.ExpandMore,
                                        contentDescription = "Change status",
                                        tint = themeAccent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        repository.toggleWatchlist(currentAnime.id, true, currentAnime)
                                        Toast.makeText(
                                            context,
                                            "Saved to My List (${activeStatus.displayName})",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                modifier = Modifier.testTag("details_bookmark_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BookmarkBorder,
                                    contentDescription = "Bookmark",
                                    tint = Color.White
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showWatchStatusMenu,
                            onDismissRequest = { showWatchStatusMenu = false }
                        ) {
                            WatchStatus.values().forEach { statusOption ->
                                val isCurrentStatus = WatchStatus.fromRaw(currentAnime.watchStatus) == statusOption && currentAnime.isInWatchlist
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = statusOption.displayName,
                                            color = if (isCurrentStatus) themeAccent else TextPrimary,
                                            fontWeight = if (isCurrentStatus) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 13.sp
                                        )
                                    },
                                    onClick = {
                                        showWatchStatusMenu = false
                                        scope.launch {
                                            repository.updateWatchStatus(currentAnime.id, statusOption.name)
                                            Toast.makeText(
                                                context,
                                                "Status updated to ${statusOption.displayName}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                )
                            }
                            HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "Remove from My List",
                                        color = com.example.ui.theme.AccentRed,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                },
                                onClick = {
                                    showWatchStatusMenu = false
                                    scope.launch {
                                        repository.toggleWatchlist(currentAnime.id, false, currentAnime)
                                        Toast.makeText(context, "Removed from My List", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = AccentYellow,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${currentAnime.rating}",
                        color = AccentYellow,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "•  ${currentAnime.year}",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    AgeRatingBadge(ageRating = currentAnime.ageRating)
                    QualityBadge(quality = currentAnime.quality)

                    Box(
                        modifier = Modifier
                            .background(Color(0xFF1E2436), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("SUB", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF1E2436), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("DUB", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (currentAnime.nextAiringEpisode != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    NextAiringBadge(
                        episode = currentAnime.nextAiringEpisode!!,
                        timeUntilSec = currentAnime.nextAiringTimeUntilSec,
                        atTimestamp = currentAnime.nextAiringAtTimestamp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val hasAired = currentAnime.availableEpisodes > 0
                    val nextEpNum = latestProgress?.episodeNumber?.coerceAtMost(currentAnime.availableEpisodes) ?: 1
                    Button(
                        onClick = {
                            if (hasAired) {
                                onPlayEpisode(currentAnime.id, nextEpNum, selectedAudioTrack)
                            } else {
                                Toast.makeText(context, "This anime has not aired yet.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasAired) themeAccent else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (hasAired) MaterialTheme.colorScheme.background else TextSecondary
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("details_play_button")
                    ) {
                        Icon(
                            imageVector = if (hasAired) Icons.Default.PlayArrow else Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when {
                                !hasAired -> "Not Yet Aired"
                                latestProgress != null -> "Resume Ep $nextEpNum"
                                else -> "Play"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            if (!hasAired) {
                                Toast.makeText(context, "Episodes are not available for download yet.", Toast.LENGTH_SHORT).show()
                                return@OutlinedButton
                            }
                            showBatchDownloadDialog = true
                        },
                        enabled = hasAired,
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (hasAired) themeAccent else TextSecondary
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (hasAired) themeAccent else DarkBorder),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("details_download_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Download", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Segmented Horizontal Pager Tabs
                val detailTabs = listOf("Episodes", "Overview", "Characters", "Themes", "Reviews", "Related")
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .border(0.5.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(detailTabs.indices.toList()) { index ->
                        val isSelected = selectedDetailTabIndex == index
                        val tabTitle = detailTabs[index]
                        val badgeText = when (index) {
                            0 -> if (displayedEpisodes.isNotEmpty()) "${displayedEpisodes.size}" else null
                            2 -> if (charactersList.isNotEmpty()) "${charactersList.size}" else null
                            3 -> if (themesList.isNotEmpty()) "${themesList.size}" else null
                            4 -> if (malReviews.isNotEmpty()) "${malReviews.size}" else null
                            5 -> if (animeRelations.isNotEmpty()) "${animeRelations.size}" else null
                            else -> null
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) themeAccent else Color.Transparent)
                                .clickable { selectedDetailTabIndex = index }
                                .padding(horizontal = 14.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = tabTitle,
                                    color = if (isSelected) MaterialTheme.colorScheme.background else TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                                if (badgeText != null) {
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) MaterialTheme.colorScheme.background.copy(alpha = 0.2f) else themeAccent.copy(alpha = 0.2f))
                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = badgeText,
                                            color = if (isSelected) MaterialTheme.colorScheme.background else themeAccent,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }
        }

        // ---------------- TAB 0: EPISODES ----------------
        if (selectedDetailTabIndex == 0) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (currentAnime.isUpcoming) "Episodes (TBA)" else "Episodes (${displayedEpisodes.size})",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        if (episodes.isNotEmpty()) {
                            // View Switcher (Detailed Cards vs Grid vs Numbers)
                            val nextMode = when (episodeLayout) {
                                "Cards" -> "Grid"
                                "Grid" -> "Numbers"
                                else -> "Cards"
                            }
                            IconButton(
                                onClick = { setEpisodeLayout(nextMode) },
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .testTag("episode_layout_toggle")
                            ) {
                                Icon(
                                    imageVector = when (episodeLayout) {
                                        "Grid" -> Icons.Default.GridView
                                        "Numbers" -> Icons.AutoMirrored.Filled.List
                                        else -> Icons.AutoMirrored.Filled.ViewList
                                    },
                                    contentDescription = "Switch layout (current: $episodeLayout)",
                                    tint = themeAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // SUB / DUB Toggle
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (selectedAudioTrack == "sub") themeAccent else Color.Transparent)
                                        .clickable { selectedAudioTrack = "sub" }
                                        .padding(horizontal = 9.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        "SUB",
                                        color = if (selectedAudioTrack == "sub") MaterialTheme.colorScheme.background else TextSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (selectedAudioTrack == "dub") themeAccent else Color.Transparent)
                                        .clickable { selectedAudioTrack = "dub" }
                                        .padding(horizontal = 9.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        "DUB",
                                        color = if (selectedAudioTrack == "dub") MaterialTheme.colorScheme.background else TextSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    if (episodes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))

                        // Episode Search
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(0.5.dp, if (episodeSearchQuery.isNotEmpty()) themeAccent.copy(alpha = 0.6f) else Color.Transparent, RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                if (episodeSearchQuery.isEmpty()) {
                                    Text(
                                        "Search episode (e.g. 12)...",
                                        color = TextSecondary,
                                        fontSize = 12.5.sp
                                    )
                                }
                                androidx.compose.foundation.text.BasicTextField(
                                    value = episodeSearchQuery,
                                    onValueChange = { episodeSearchQuery = it },
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        color = TextPrimary,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Normal
                                    ),
                                    cursorBrush = androidx.compose.ui.graphics.SolidColor(themeAccent),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            if (episodeSearchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { episodeSearchQuery = "" },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // Episode Range Selector Carousel (1-25, 26-50, etc.)
                        if (episodeChunks.size > 1) {
                            Spacer(modifier = Modifier.height(10.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(episodeChunks.indices.toList()) { idx ->
                                    val chunk = episodeChunks[idx]
                                    val label = "${chunk.first().number}-${chunk.last().number}"
                                    val isSel = selectedRangeIndex == idx
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(if (isSel) themeAccent else MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable { selectedRangeIndex = idx }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (isSel) MaterialTheme.colorScheme.background else TextSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            item {
                if (displayedEpisodes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = themeAccent,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (episodeSearchQuery.isNotBlank()) "No Matching Episodes" else "No Released Episodes Yet",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Episodes will appear once officially released.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    if (detailsSettings.showEpisodeLayoutHint) {
                        val hintLabel = when (episodeLayout) {
                            "Grid" -> "Thumbnail Grid"
                            "Numbers" -> "Compact Numbers"
                            else -> "Detailed Cards"
                        }
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 2.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(0.5.dp, DarkBorder, RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = hintLabel,
                                color = TextSecondary,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    if (episodeLayout == "Numbers") {
                        // Compact 5-Column Numbers Grid Mode
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            currentChunkEpisodes.chunked(5).forEach { rowEpisodes ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowEpisodes.forEach { ep ->
                                        val epProgress = allProgress.firstOrNull { it.id == "${currentAnime.id}_${ep.number}" }
                                        EpisodeNumberChip(
                                            episode = ep,
                                            progress = epProgress,
                                            themeAccent = themeAccent,
                                            onClick = { onPlayEpisode(currentAnime.id, ep.number, selectedAudioTrack) },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                activeEpisodeForActions = ep
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    repeat(5 - rowEpisodes.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    } else if (episodeLayout == "Grid") {
                        // 2-Column Grid Mode with Thumbnails
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            currentChunkEpisodes.chunked(2).forEach { rowEpisodes ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    rowEpisodes.forEach { ep ->
                                        val epProgress = allProgress.firstOrNull { it.id == "${currentAnime.id}_${ep.number}" }
                                        Box(modifier = Modifier.weight(1f)) {
                                            EpisodeThumbnailCard(
                                                episode = ep,
                                                onClick = { onPlayEpisode(currentAnime.id, ep.number, selectedAudioTrack) },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    activeEpisodeForActions = ep
                                                },
                                                progress = epProgress,
                                                themeAccent = themeAccent,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                    repeat(2 - rowEpisodes.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    } else {
                        // Detailed List Mode: Rich 16:9 thumbnails, episode titles, duration, progress bar
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            currentChunkEpisodes.forEach { ep ->
                                val epProgress = allProgress.firstOrNull { it.id == "${currentAnime.id}_${ep.number}" }
                                EpisodeDetailedListItem(
                                    episode = ep,
                                    progress = epProgress,
                                    themeAccent = themeAccent,
                                    onClick = { onPlayEpisode(currentAnime.id, ep.number, selectedAudioTrack) },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        activeEpisodeForActions = ep
                                    },
                                    onDownload = {
                                        scope.launch {
                                            repository.addDownload(currentAnime, ep)
                                            Toast.makeText(context, "Saved Episode ${ep.number} for offline", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // ---------------- TAB 1: OVERVIEW & DETAILS ----------------
        if (selectedDetailTabIndex == 1) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "Synopsis",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(modifier = Modifier.animateContentSize()) {
                        Text(
                            text = currentAnime.synopsis.ifBlank { "No synopsis available." },
                            color = TextPrimary.copy(alpha = 0.88f),
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            maxLines = if (isExpandedSynopsis) Int.MAX_VALUE else 4,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isExpandedSynopsis) "View Less" else "... View More",
                            color = themeAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { isExpandedSynopsis = !isExpandedSynopsis }
                                .padding(vertical = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "Series Details",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Specs & Metadata Grid Card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(0.5.dp, DarkBorder, RoundedCornerShape(14.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Format", color = TextSecondary, fontSize = 11.sp)
                                Text(currentAnime.type, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Status", color = TextSecondary, fontSize = 11.sp)
                                Text(currentAnime.status, color = themeAccent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        androidx.compose.material3.HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Studio", color = TextSecondary, fontSize = 11.sp)
                                Text(currentAnime.studio, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Season & Year", color = TextSecondary, fontSize = 11.sp)
                                Text("${currentAnime.season} ${currentAnime.year}", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        androidx.compose.material3.HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Episodes", color = TextSecondary, fontSize = 11.sp)
                                Text("${currentAnime.totalEpisodes} eps", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Quality & Rating", color = TextSecondary, fontSize = 11.sp)
                                Text("${currentAnime.quality} • ${currentAnime.ageRating}", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "Genres",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val genresList = remember(currentAnime.genres) {
                        currentAnime.genres.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(genresList) { genre ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(0.5.dp, DarkBorder, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(genre, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // ---------------- TAB 2: CHARACTERS & VOICE ACTORS ----------------
        if (selectedDetailTabIndex == 2) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "Characters & Voice Actors",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    when {
                        isLoadingCharacters -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(20.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(color = themeAccent, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Loading character cast & VAs…", color = TextSecondary, fontSize = 13.sp)
                            }
                        }
                        charactersList.isEmpty() -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(0.5.dp, DarkBorder, RoundedCornerShape(14.dp))
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No character or voice actor records found for this title.", color = TextSecondary, fontSize = 12.sp)
                            }
                        }
                        else -> {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                charactersList.forEach { charVa ->
                                    CharacterVaCard(character = charVa, themeAccent = themeAccent)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // ---------------- TAB 3: THEMES & MUSIC (JUKEBOX) ----------------
        if (selectedDetailTabIndex == 3) {
            item {
                ThemesSection(
                    themes = themesList,
                    isLoading = isLoadingThemes,
                    themeAccent = themeAccent,
                    onPlayTheme = { activeThemeToPlay = it }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // ---------------- TAB 4: REVIEWS & DISCUSSIONS ----------------
        if (selectedDetailTabIndex == 4) {
            item {
                MalReviewsSection(
                    reviews = malReviews,
                    isLoading = isLoadingReviews,
                    expanded = reviewsExpanded,
                    onToggleExpanded = { reviewsExpanded = !reviewsExpanded },
                    themeAccent = themeAccent
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // ---------------- TAB 5: RELATED & RECOMMENDATIONS ----------------
        if (selectedDetailTabIndex == 5) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    if (animeRelations.isNotEmpty()) {
                        Text(
                            text = "Franchise Watch Order & Seasons",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(animeRelations) { rel ->
                                Column(
                                    modifier = Modifier
                                        .width(105.dp)
                                        .clickable { onRelatedAnimeClick(rel.id) }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(145.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        AsyncImage(
                                            model = rel.coverImage,
                                            contentDescription = rel.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        if (rel.isCurrent) {
                                            CurrentSeasonBadge(
                                                modifier = Modifier
                                                    .align(Alignment.TopStart)
                                                    .padding(4.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = rel.title,
                                        color = TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 2,
                                        lineHeight = 16.sp,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = seasonLabelFor(rel, detailsSettings),
                                        color = themeAccent,
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    if (relatedAnime.isNotEmpty()) {
                        Text(
                            text = "More Like This",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(relatedAnime, key = { it.id }) { rel ->
                                AnimeCard(
                                    anime = rel,
                                    cardWidth = 130f,
                                    titlePreference = detailsSettings.titleLanguagePreference,
                                    onClick = { onRelatedAnimeClick(rel.id) },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        longPressedAnimeForActions = rel
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
        }

        if (activeThemeToPlay != null) {
            ThemeVideoPlayerDialog(
                theme = activeThemeToPlay!!,
                themeAccent = themeAccent,
                onDismiss = { activeThemeToPlay = null }
            )
        }

        if (showBatchDownloadDialog) {
            val currentSettings by repository.settings.collectAsState(initial = repository.settingsManager.getSettings())
            BatchDownloadDialog(
                anime = currentAnime,
                episodes = episodes,
                autoDeleteWatched = currentSettings.downloadAutoDeleteWatched,
                themeAccent = themeAccent,
                onDownloadCount = { count ->
                    showBatchDownloadDialog = false
                    val watchedEpNumbers = allProgress.filter { it.completed }.map { it.episodeNumber }.toSet()
                    val unwatched = episodes.filter { !watchedEpNumbers.contains(it.number) }.ifEmpty { episodes }
                    val toDownload = unwatched.take(count)
                    scope.launch {
                        toDownload.forEach { ep ->
                            repository.addDownload(currentAnime, ep)
                        }
                        Toast.makeText(context, "Queued ${toDownload.size} episode(s) for background download", Toast.LENGTH_SHORT).show()
                    }
                },
                onToggleAutoDelete = { enabled ->
                    scope.launch {
                        repository.settingsManager.update { it.copy(downloadAutoDeleteWatched = enabled) }
                        Toast.makeText(context, if (enabled) "Auto-delete after watching enabled" else "Auto-delete disabled", Toast.LENGTH_SHORT).show()
                    }
                },
                onDismiss = { showBatchDownloadDialog = false }
            )
        }
    }
}

@Composable
private fun MalReviewsSection(
    reviews: List<MalReview>,
    isLoading: Boolean,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    themeAccent: Color
) {
    var showAllReviews by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .animateContentSize()
            .testTag("details_mal_reviews_section")
    ) {
        androidx.compose.material3.HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpanded() }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.RateReview,
                contentDescription = null,
                tint = themeAccent,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "MAL & Community Reviews",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            if (reviews.isNotEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(themeAccent.copy(alpha = 0.16f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${reviews.size}",
                        color = themeAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse reviews" else "Expand reviews",
                tint = TextSecondary
            )
        }

        if (expanded) {
            Spacer(modifier = Modifier.height(10.dp))
            when {
                isLoading -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = themeAccent,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Fetching MyAnimeList & AniList user reviews…",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
                reviews.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(0.5.dp, DarkBorder, RoundedCornerShape(14.dp))
                            .padding(18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No user reviews available for this anime yet.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                else -> {
                    val displayedReviews = if (showAllReviews) reviews else reviews.take(8)
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        displayedReviews.forEach { review ->
                            MalReviewCard(review = review, themeAccent = themeAccent)
                        }
                        if (reviews.size > 8) {
                            OutlinedButton(
                                onClick = { showAllReviews = !showAllReviews },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (showAllReviews) "Show Fewer Reviews" else "Show All ${reviews.size} Reviews",
                                    color = themeAccent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MalReviewCard(
    review: MalReview,
    themeAccent: Color
) {
    var isExpanded by remember { mutableStateOf(false) }
    var revealSpoilers by remember { mutableStateOf(!review.isSpoiler) }
    val isAniListSource = review.source.contains("AniList", ignoreCase = true) || review.id.startsWith("ani_")
    val imageCount = remember(review.body) { ReviewRichParser.extractAllImageUrls(review.body).size }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(0.5.dp, DarkBorder, RoundedCornerShape(14.dp))
            .clickable { isExpanded = !isExpanded }
            .padding(14.dp)
            .animateContentSize()
            .testTag("mal_review_card_${review.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (review.avatarUrl.isNotBlank()) {
                    AsyncImage(
                        model = review.avatarUrl,
                        contentDescription = review.author,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = themeAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = review.author,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (isAniListSource) Color(0xFF02A9FF).copy(alpha = 0.2f)
                                else Color(0xFF2E51A2).copy(alpha = 0.25f)
                            )
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = if (isAniListSource) "AniList" else "MAL",
                            color = if (isAniListSource) Color(0xFF38BDF8) else Color(0xFF7CA5FF),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    if (imageCount > 0) {
                        Spacer(modifier = Modifier.width(5.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(themeAccent.copy(alpha = 0.16f))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "$imageCount img",
                                color = themeAccent,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                val metaParts = buildList {
                    if (review.date.isNotBlank()) add(review.date)
                    if (review.episodesWatched > 0) add("Watched ${review.episodesWatched} ep(s)")
                    if (review.isPreliminary) add("Preliminary")
                }
                if (metaParts.isNotEmpty()) {
                    Text(
                        text = metaParts.joinToString(" • "),
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(AccentYellow.copy(alpha = 0.16f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = AccentYellow,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "${review.score}/10",
                    color = AccentYellow,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (review.isSpoiler && !revealSpoilers) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { revealSpoilers = true }
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Contains Spoilers — Tap to reveal review",
                    color = AccentYellow,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            if (isExpanded && review.summary.isNotBlank() && !review.body.trimStart().startsWith(review.summary.take(30))) {
                Text(
                    text = review.summary,
                    color = themeAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            ReviewRichContent(
                rawBody = review.body,
                isExpanded = isExpanded,
                themeAccent = themeAccent
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (review.helpfulCount > 0) {
                    Icon(
                        imageVector = Icons.Default.ThumbUp,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${review.helpfulCount} found helpful",
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = if (isExpanded) "Show less" else "Read full review",
                    color = themeAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EpisodeNumberChip(
    episode: Episode,
    progress: com.example.data.model.WatchProgress?,
    themeAccent: Color = LocalThemeAccent.current,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isWatched = progress?.completed == true
    val inProgress = progress != null && progress.currentTimeSec > 0 && !isWatched
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isWatched) themeAccent.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = if (isWatched) 1.2.dp else 0.5.dp,
                color = if (isWatched) themeAccent else DarkBorder,
                shape = RoundedCornerShape(10.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("episode_chip_${episode.number}"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "${episode.number}",
            color = if (isWatched) themeAccent else TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        if (inProgress && progress != null && progress.durationSec > 0) {
            val frac = (progress.currentTimeSec.toFloat() / progress.durationSec.toFloat()).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { frac },
                color = themeAccent,
                trackColor = Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .align(Alignment.BottomCenter)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EpisodeThumbnailCard(
    episode: Episode,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    progress: com.example.data.model.WatchProgress? = null,
    themeAccent: Color = LocalThemeAccent.current,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isWatched = progress?.completed == true
    val appSettings = LocalAppSettings.current
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "ep_scale"
    )

    Column(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(10.dp))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = themeAccent),
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("episode_card_${episode.number}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = episode.thumbnail,
                contentDescription = episode.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (appSettings.spoilerBlurCovers && !isWatched) Modifier.blur(12.dp) else Modifier)
            )

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(0x99000000), CircleShape)
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = themeAccent,
                    modifier = Modifier.size(20.dp)
                )
            }

            if (progress?.completed == true) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(Color(0xCC000000), CircleShape)
                        .padding(2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Completed",
                        tint = themeAccent,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (progress != null && progress.durationSec > 0) {
                val fraction = (progress.currentTimeSec.toFloat() / progress.durationSec.toFloat()).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { fraction },
                    color = themeAccent,
                    trackColor = Color(0x66000000),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.BottomCenter)
                )
            }

            if (episode.isOffline) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                ) {
                    OfflineBadge()
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Ep ${episode.number}: ${episode.title}",
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Thumbnail tile used by the three-column "Grid" episode layout.
 *
 * Deliberately separate from [EpisodeThumbnailCard], which hard-codes a 160dp width: these tiles
 * share the row width instead so exactly three fit, whatever the screen size.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EpisodeGridTile(
    episode: Episode,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    progress: com.example.data.model.WatchProgress? = null,
    themeAccent: Color = LocalThemeAccent.current,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("episode_tile_${episode.number}")
    ) {
        val appSettings = LocalAppSettings.current
        AsyncImage(
            model = episode.thumbnail,
            contentDescription = episode.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .then(if (appSettings.spoilerBlurCovers && progress?.completed != true) Modifier.blur(12.dp) else Modifier)
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(4.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xCC000000))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = "${episode.number}",
                color = if (progress?.completed == true) themeAccent else Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (progress?.completed == true) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .background(Color(0xCC000000), CircleShape)
                    .padding(2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Completed",
                    tint = themeAccent,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        if (progress != null && progress.durationSec > 0) {
            val fraction =
                (progress.currentTimeSec.toFloat() / progress.durationSec.toFloat()).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { fraction },
                color = themeAccent,
                trackColor = Color(0x66000000),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.BottomCenter)
            )
        }

        if (episode.isOffline) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
            ) {
                OfflineBadge()
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EpisodeDetailedListItem(
    episode: Episode,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onDownload: (() -> Unit)? = null,
    progress: com.example.data.model.WatchProgress? = null,
    themeAccent: Color = LocalThemeAccent.current,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isWatched = progress?.completed == true

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isWatched) MaterialTheme.colorScheme.surface.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surface)
            .border(0.5.dp, if (isWatched) themeAccent.copy(alpha = 0.4f) else DarkBorder, RoundedCornerShape(12.dp))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = themeAccent),
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 16:9 Thumbnail
        Box(
            modifier = Modifier
                .width(112.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = episode.thumbnail,
                contentDescription = episode.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (LocalAppSettings.current.spoilerBlurCovers && !isWatched) Modifier.blur(12.dp) else Modifier)
            )

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(Color(0x99000000), CircleShape)
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = themeAccent,
                    modifier = Modifier.size(18.dp)
                )
            }

            if (episode.durationSec > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xCC000000))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "${episode.durationSec / 60}m",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (progress != null && progress.durationSec > 0) {
                val frac = (progress.currentTimeSec.toFloat() / progress.durationSec.toFloat()).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { frac },
                    color = themeAccent,
                    trackColor = Color(0x66000000),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.BottomCenter)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Episode ${episode.number}",
                    color = if (isWatched) themeAccent else TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                if (isWatched) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Watched",
                        tint = themeAccent,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            if (episode.title.isNotBlank() && episode.title != "Episode ${episode.number}") {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = episode.title,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (progress != null && progress.currentTimeSec > 0L && !isWatched) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${progress.currentTimeSec / 60}m watched",
                    color = themeAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        if (onDownload != null) {
            IconButton(
                onClick = onDownload,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Download episode",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun CharacterVaCard(
    character: com.example.data.model.CharacterVa,
    themeAccent: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(0.5.dp, DarkBorder, RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = character.characterImage,
            contentDescription = character.characterName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = character.characterName,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = character.role,
                color = themeAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }

        if (character.vaName.isNotBlank()) {
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text(
                    text = character.vaName,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = character.vaLanguage,
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            }

            if (character.vaImage.isNotBlank()) {
                AsyncImage(
                    model = character.vaImage,
                    contentDescription = character.vaName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }
        }
    }
}

@Composable
private fun ThemesSection(
    themes: List<com.example.data.model.AnimeTheme>,
    isLoading: Boolean,
    themeAccent: Color,
    onPlayTheme: (com.example.data.model.AnimeTheme) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Audiotrack,
                contentDescription = null,
                tint = themeAccent,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Anime Themes & Music",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            if (themes.isNotEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(themeAccent.copy(alpha = 0.16f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${themes.size}",
                        color = themeAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when {
            isLoading -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = themeAccent,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Loading creditless Openings & Endings…",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
            themes.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(0.5.dp, DarkBorder, RoundedCornerShape(14.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No creditless themes found for this title.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
            else -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    themes.forEach { theme ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(0.5.dp, DarkBorder, RoundedCornerShape(12.dp))
                                .clickable { onPlayTheme(theme) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(themeAccent.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${theme.type} ${theme.sequence}",
                                    color = themeAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = theme.songTitle,
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (theme.artist.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = theme.artist,
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0x22FFFFFF))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = theme.resolution,
                                    color = TextSecondary,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(themeAccent)
                                    .clickable { onPlayTheme(theme) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play theme",
                                    tint = MaterialTheme.colorScheme.background,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeVideoPlayerDialog(
    theme: com.example.data.model.AnimeTheme,
    themeAccent: Color,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val exoPlayer = remember {
        androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
            setMediaItem(androidx.media3.common.MediaItem.fromUri(theme.videoUrl))
            prepare()
            playWhenReady = true
        }
    }

    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Column {
                Text(
                    text = "${theme.type} ${theme.sequence} — ${theme.songTitle}",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (theme.artist.isNotBlank()) {
                    Text(
                        text = theme.artist,
                        color = themeAccent,
                        fontSize = 12.sp
                    )
                }
            }
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = { ctx ->
                        androidx.media3.ui.PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = true
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = themeAccent)
            ) {
                Text("Close", color = MaterialTheme.colorScheme.background, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun BatchDownloadDialog(
    anime: Anime,
    episodes: List<Episode>,
    autoDeleteWatched: Boolean,
    themeAccent: Color,
    onDownloadCount: (Int) -> Unit,
    onToggleAutoDelete: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text("Download Episodes", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Save episodes offline for smooth, data-free viewing:", color = TextSecondary, fontSize = 12.sp)

                Button(
                    onClick = { onDownloadCount(1) },
                    colors = ButtonDefaults.buttonColors(containerColor = themeAccent),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Download Next Unwatched Episode", color = MaterialTheme.colorScheme.background, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                if (episodes.size >= 5) {
                    Button(
                        onClick = { onDownloadCount(5) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Download Next 5 Unwatched", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }

                Button(
                    onClick = { onDownloadCount(episodes.size) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Download All Available (${episodes.size} eps)", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }

                androidx.compose.material3.HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-Delete Watched", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Deletes file once finished to save storage", color = TextSecondary, fontSize = 10.sp)
                    }
                    androidx.compose.material3.Switch(
                        checked = autoDeleteWatched,
                        onCheckedChange = onToggleAutoDelete,
                        colors = androidx.compose.material3.SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.background,
                            checkedTrackColor = themeAccent
                        )
                    )
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}


