package com.example.ui.schedule

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.ripple
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.AiringScheduleItem
import com.example.data.model.Anime
import com.example.data.repository.AnimeRepository
import com.example.ui.components.AnimeQuickActionDialog
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.LocalThemeAccent
import com.example.ui.theme.LocalAppSettings
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class CalendarDay(
    val dayName: String,
    val dayNumber: String,
    val fullDateStr: String,
    val isToday: Boolean,
    val startOfDaySec: Long,
    val endOfDaySec: Long
)

@Composable
fun ScheduleScreen(
    repository: AnimeRepository,
    onAnimeClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val themeAccent = LocalThemeAccent.current

    val days = remember {
        val list = mutableListOf<CalendarDay>()
        val dayFormat = SimpleDateFormat("EEE", Locale.ENGLISH)
        val numFormat = SimpleDateFormat("d", Locale.ENGLISH)
        val fullFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)

        // 7 days before today to 7 days after today (15 days total)
        for (offset in -7..7) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, offset)
            val dName = dayFormat.format(cal.time)
            val dNum = numFormat.format(cal.time)
            val isToday = offset == 0
            val fullStr = fullFormat.format(cal.time)

            val startCal = Calendar.getInstance().apply {
                time = cal.time
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val endCal = Calendar.getInstance().apply {
                time = cal.time
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }

            list.add(
                CalendarDay(
                    dayName = dName,
                    dayNumber = dNum,
                    fullDateStr = fullStr,
                    isToday = isToday,
                    startOfDaySec = startCal.timeInMillis / 1000L,
                    endOfDaySec = endCal.timeInMillis / 1000L
                )
            )
        }
        list
    }

    var selectedDayIndex by remember { mutableIntStateOf(7) }
    val selectedDay = days.getOrElse(selectedDayIndex) { days[0] }

    // In-memory cache for fast day switching
    val scheduleCache = remember { mutableStateMapOf<Long, List<AiringScheduleItem>>() }
    var isLoading by remember { mutableStateOf(false) }
    var currentItems by remember { mutableStateOf<List<AiringScheduleItem>>(emptyList()) }

    val allAnime by repository.allAnime.collectAsState(initial = emptyList())
    val settings by repository.settings.collectAsState(initial = repository.settingsManager.getSettings())
    var selectedQuickActionAnime by remember { mutableStateOf<Anime?>(null) }

    fun loadSchedule(day: CalendarDay, forceRefresh: Boolean = false) {
        if (!forceRefresh && scheduleCache.containsKey(day.startOfDaySec)) {
            currentItems = scheduleCache[day.startOfDaySec].orEmpty()
            return
        }
        scope.launch {
            isLoading = true
            try {
                val fetched = repository.getAiringSchedule(day.startOfDaySec, day.endOfDaySec)
                scheduleCache[day.startOfDaySec] = fetched
                currentItems = fetched
            } catch (_: Exception) {
                // If offline, fallback to local anime matches
                if (currentItems.isEmpty()) {
                    val local = repository.getAnimeByScheduleDay(day.dayName).first()
                    currentItems = (if (local.isNotEmpty()) local else allAnime.take(10)).mapIndexed { idx, a ->
                        AiringScheduleItem(
                            id = idx,
                            airingAt = day.startOfDaySec + (idx * 3600L + 7200L),
                            timeUntilAiring = 0L,
                            episode = (a.nextAiringEpisode ?: (a.availableEpisodes + 1)).coerceAtLeast(1),
                            anime = a
                        )
                    }
                }
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(selectedDay.startOfDaySec) {
        loadSchedule(selectedDay)
    }

    val dayListState = rememberLazyListState(initialFirstVisibleItemIndex = 4)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // ----- Header -----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_zenith_logo),
                contentDescription = "Zenith Anime Logo",
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "Schedule",
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.weight(1f))

            IconButton(
                onClick = { loadSchedule(selectedDay, forceRefresh = true) },
                modifier = Modifier.testTag("schedule_refresh_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh schedule",
                    tint = TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }

            IconButton(
                onClick = onSearchClick,
                modifier = Modifier.testTag("schedule_search_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // ----- Date Carousel (th3-anime style circular / pill items) -----
        LazyRow(
            state = dayListState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            itemsIndexed(days) { index, day ->
                val isSelected = index == selectedDayIndex
                val animatedBg by animateColorAsState(
                    targetValue = if (isSelected) themeAccent else MaterialTheme.colorScheme.surfaceVariant,
                    animationSpec = tween(150),
                    label = "day_bg_${day.fullDateStr}"
                )
                val animatedTextColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.background else TextPrimary,
                    animationSpec = tween(150),
                    label = "day_text_${day.fullDateStr}"
                )
                val dayScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.05f else 1.0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                    label = "day_scale_${day.fullDateStr}"
                )

                Box(
                    modifier = Modifier
                        .scale(dayScale)
                        .clip(RoundedCornerShape(26.dp))
                        .background(animatedBg)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            selectedDayIndex = index
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .testTag("schedule_day_${day.dayName.lowercase()}_${day.dayNumber}"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = day.dayName,
                            color = if (isSelected) MaterialTheme.colorScheme.background else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = day.dayNumber,
                            color = animatedTextColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (day.isToday && !isSelected) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .background(themeAccent, CircleShape)
                            )
                        }
                    }
                }
            }
        }

        // ----- Content: Timeline or Empty / Loading -----
        if (isLoading && currentItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = themeAccent, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Loading schedule...", color = TextSecondary, fontSize = 13.sp)
                }
            }
        } else if (currentItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "No anime scheduled for ${selectedDay.dayName} ${selectedDay.dayNumber}",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Check adjacent days or use Search to discover trending shows.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(currentItems, key = { "${it.id}_${it.airingAt}_${it.anime.id}" }) { item ->
                    val timeStr = remember(item.airingAt) {
                        try {
                            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(item.airingAt * 1000L))
                        } catch (_: Exception) {
                            "00:00"
                        }
                    }

                    AiringTimelineRow(
                        airingItem = item,
                        timeStr = timeStr,
                        onClick = { onAnimeClick(item.anime.id) },
                        onLongClick = { selectedQuickActionAnime = item.anime },
                        onToggleWatchlist = {
                            scope.launch {
                                val nextInList = !item.anime.isInWatchlist
                                repository.toggleWatchlist(item.anime.id, nextInList, item.anime)
                                // Optimistically update item
                                val updated = currentItems.map { cur ->
                                    if (cur.anime.id == item.anime.id) {
                                        cur.copy(anime = cur.anime.copy(isInWatchlist = nextInList))
                                    } else cur
                                }
                                currentItems = updated
                                scheduleCache[selectedDay.startOfDaySec] = updated
                                Toast.makeText(
                                    context,
                                    if (nextInList) "Added to My List" else "Removed from My List",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        repository = repository
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }

    if (selectedQuickActionAnime != null) {
        val target = selectedQuickActionAnime!!
        AnimeQuickActionDialog(
            anime = target,
            onDismiss = { selectedQuickActionAnime = null },
            onPlay = { onAnimeClick(target.id) },
            onViewDetails = { onAnimeClick(target.id) },
            onToggleFavorite = {
                scope.launch {
                    repository.toggleFavorite(target.id, !target.isFavorite)
                    Toast.makeText(context, if (!target.isFavorite) "Added to Favorites" else "Removed from Favorites", Toast.LENGTH_SHORT).show()
                }
            },
            onToggleWatchlist = {
                scope.launch {
                    repository.toggleWatchlist(target.id, !target.isInWatchlist, target)
                    Toast.makeText(context, if (!target.isInWatchlist) "Added to Watchlist" else "Removed from Watchlist", Toast.LENGTH_SHORT).show()
                }
            },
            onChangeWatchStatus = { status ->
                scope.launch {
                    repository.updateWatchStatus(target.id, status.name)
                    Toast.makeText(context, "Moved to ${status.displayName}", Toast.LENGTH_SHORT).show()
                }
            },
            onDownload = {
                scope.launch {
                    val ep = com.example.data.model.Episode(
                        id = "${target.id}_1",
                        animeId = target.id,
                        number = 1,
                        title = "Episode 1",
                        thumbnail = target.coverImage
                    )
                    repository.addDownload(target, ep)
                    Toast.makeText(context, "Saved Episode 1 for offline", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AiringTimelineRow(
    airingItem: AiringScheduleItem,
    timeStr: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onToggleWatchlist: () -> Unit,
    repository: AnimeRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val themeAccent = LocalThemeAccent.current
    val settings = LocalAppSettings.current
    val scope = rememberCoroutineScope()
    val rowInteractionSource = remember { MutableInteractionSource() }
    val isRowPressed by rowInteractionSource.collectIsPressedAsState()

    val scaleAnim by animateFloatAsState(
        targetValue = if (isRowPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "airing_item_scale"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        // Time Indicator Dash (matching th3-anime "- 07:26")
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(12.dp)
                    .height(3.dp)
                    .background(themeAccent, RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = timeStr,
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Card with continuous timeline line on left
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Left continuous timeline vertical guide
            Box(
                modifier = Modifier
                    .width(16.dp)
                    .padding(start = 5.dp, top = 4.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(86.dp)
                        .background(Color(0x33FFFFFF), RoundedCornerShape(1.dp))
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Main Card
            Row(
                modifier = Modifier
                    .weight(1f)
                    .scale(scaleAnim)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(0.5.dp, DarkBorder, RoundedCornerShape(12.dp))
                    .combinedClickable(
                        interactionSource = rowInteractionSource,
                        indication = ripple(bounded = true, color = themeAccent),
                        onClick = onClick,
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLongClick?.invoke()
                        }
                    )
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = airingItem.anime.bannerImage.ifEmpty { airingItem.anime.coverImage },
                    contentDescription = airingItem.anime.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(width = 110.dp, height = 74.dp)
                        .clip(RoundedCornerShape(8.dp))
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    val nowSec = remember { System.currentTimeMillis() / 1000L }
                    val diffSec = airingItem.airingAt - nowSec
                    val countdownText = remember(airingItem.airingAt, nowSec) {
                        when {
                            diffSec > 86400L -> "Airs in ${diffSec / 86400L}d ${(diffSec % 86400L) / 3600L}h"
                            diffSec > 3600L -> "Airs in ${diffSec / 3600L}h ${(diffSec % 3600L) / 60L}m"
                            diffSec > 0L -> "Airs in ${diffSec / 60L}m"
                            diffSec > -86400L -> "Aired ${(-diffSec) / 3600L}h ago"
                            else -> "Aired"
                        }
                    }
                    val isUpcoming = diffSec > 0L
                    val reminderActive = remember(settings.airingReminderIds, airingItem.anime.id) {
                        settings.airingReminderIds.split(",").map { it.trim() }.contains(airingItem.anime.id)
                    }

                    Text(
                        text = airingItem.anime.displayTitle(settings.titleLanguagePreference),
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        lineHeight = 17.sp,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Episode ${airingItem.episode}",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isUpcoming) themeAccent.copy(alpha = 0.16f) else Color(0x22FFFFFF))
                                .border(0.5.dp, if (isUpcoming) themeAccent.copy(alpha = 0.4f) else Color(0x33FFFFFF), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isUpcoming) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .background(themeAccent, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = countdownText,
                                    color = if (isUpcoming) themeAccent else TextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Pill Button: "+ My List" / "✓ In List"
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (airingItem.anime.isInWatchlist) themeAccent.copy(alpha = 0.2f) else Color(0x33FFFFFF))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true, color = themeAccent),
                                    onClick = onToggleWatchlist
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (airingItem.anime.isInWatchlist) Icons.Default.Check else Icons.Default.Add,
                                    contentDescription = null,
                                    tint = if (airingItem.anime.isInWatchlist) themeAccent else TextPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (airingItem.anime.isInWatchlist) "In List" else "My List",
                                    color = if (airingItem.anime.isInWatchlist) themeAccent else TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Airing Reminder Alert Pill
                        if (isUpcoming) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (reminderActive) themeAccent.copy(alpha = 0.2f) else Color(0x22FFFFFF))
                                    .clickable {
                                        val nowActive = repository.settingsManager.toggleAiringReminder(airingItem.anime.id)
                                        if (nowActive) {
                                            scope.launch {
                                                repository.addNotification(
                                                    title = "Reminder: ${airingItem.anime.displayTitle(settings.titleLanguagePreference)}",
                                                    message = "Episode ${airingItem.episode} is scheduled to air ($countdownText)!",
                                                    animeId = airingItem.anime.id,
                                                    animeTitle = airingItem.anime.displayTitle(settings.titleLanguagePreference),
                                                    coverImage = airingItem.anime.coverImage,
                                                    episodeNumber = airingItem.episode
                                                )
                                            }
                                        }
                                        Toast.makeText(
                                            context,
                                            if (nowActive) "Reminder set for ${airingItem.anime.displayTitle(settings.titleLanguagePreference)} Ep ${airingItem.episode}!"
                                            else "Airing reminder removed",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = "Airing alert",
                                        tint = if (reminderActive) themeAccent else TextSecondary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = if (reminderActive) "Alert On" else "Remind Me",
                                        color = if (reminderActive) themeAccent else TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
