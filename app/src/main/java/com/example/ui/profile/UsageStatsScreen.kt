package com.example.ui.profile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.AnimeWatchStats
import com.example.data.model.DailyActivityPoint
import com.example.data.model.GenreWatchStat
import com.example.data.model.UsageStatsCalculator
import com.example.data.model.UsageStatsRange
import com.example.data.repository.AnimeRepository
import com.example.ui.theme.AccentRed
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.LocalThemeAccent
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun UsageStatsScreen(
    repository: AnimeRepository,
    onBackClick: () -> Unit,
    onAnimeClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val themeAccent = LocalThemeAccent.current
    val settings by repository.settings.collectAsState(initial = repository.settingsManager.getSettings())
    val dailyStats by repository.dailyUsageStats.collectAsState(initial = emptyList())
    val animeStats by repository.animeWatchStats.collectAsState(initial = emptyList())

    var selectedRange by remember { mutableStateOf(UsageStatsRange.WEEK_7) }
    var showResetConfirm by remember { mutableStateOf(false) }

    val summary = remember(dailyStats, animeStats, selectedRange) {
        UsageStatsCalculator.computeSummary(
            dailyList = dailyStats,
            animeList = animeStats,
            range = selectedRange
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Watch Time & Usage",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (settings.incognitoMode) {
                        "Incognito active • Recording paused"
                    } else {
                        "Local-first device statistics"
                    },
                    color = if (settings.incognitoMode) themeAccent else TextSecondary,
                    fontSize = 11.sp
                )
            }
            IconButton(
                onClick = {
                    if (settings.confirmDestructiveActions) {
                        showResetConfirm = true
                    } else {
                        scope.launch {
                            repository.clearUsageStats()
                            Toast.makeText(context, "Usage statistics cleared", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Reset Usage Statistics",
                    tint = TextSecondary
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Time Range Selector Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                UsageStatsRange.entries.forEach { range ->
                    val selected = selectedRange == range
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (selected) themeAccent else MaterialTheme.colorScheme.surface
                            )
                            .border(
                                width = 1.dp,
                                color = if (selected) themeAccent else DarkBorder,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { selectedRange = range }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = range.label,
                            color = if (selected) MaterialTheme.colorScheme.background else TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 2x2 KPI Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KpiMetricCard(
                    icon = Icons.Default.PlayCircleOutline,
                    label = "Watch Time",
                    value = UsageStatsCalculator.formatDuration(summary.totalWatchTimeSec),
                    sublabel = "Avg ${UsageStatsCalculator.formatDuration(summary.avgDailyWatchTimeSec)}/day",
                    accent = themeAccent,
                    modifier = Modifier.weight(1f)
                )
                KpiMetricCard(
                    icon = Icons.Default.AccessTime,
                    label = "App Time",
                    value = UsageStatsCalculator.formatDuration(summary.totalAppTimeSec),
                    sublabel = "${summary.sessionsCount} session${if (summary.sessionsCount == 1) "" else "s"}",
                    accent = themeAccent,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KpiMetricCard(
                    icon = Icons.Default.CheckCircle,
                    label = "Episodes Finished",
                    value = "${summary.episodesCompleted}",
                    sublabel = "${summary.activeDays} active day${if (summary.activeDays == 1) "" else "s"}",
                    accent = themeAccent,
                    modifier = Modifier.weight(1f)
                )
                KpiMetricCard(
                    icon = Icons.Default.LocalFireDepartment,
                    label = "Daily Streak",
                    value = "${summary.currentStreakDays}d",
                    sublabel = "Best ${summary.longestStreakDays}d",
                    accent = themeAccent,
                    modifier = Modifier.weight(1f)
                )
            }

            // Recent Daily Activity Chart
            DailyActivityChartCard(
                days = summary.chartDays,
                accent = themeAccent
            )

            // Top Watched Anime
            TopAnimeStatsCard(
                items = summary.topAnime,
                accent = themeAccent,
                onAnimeClick = onAnimeClick
            )

            // Top Genres
            if (summary.topGenres.isNotEmpty()) {
                TopGenresStatsCard(
                    genres = summary.topGenres,
                    accent = themeAccent
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text("Reset Usage Statistics?", color = AccentRed, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "This clears your recorded watch time, daily app time, streaks, and per-anime counters. Your watchlist and episode resume positions are kept.",
                    color = TextPrimary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            repository.clearUsageStats()
                            showResetConfirm = false
                            Toast.makeText(context, "Usage statistics cleared", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed, contentColor = TextPrimary)
                ) {
                    Text("Reset Stats", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun KpiMetricCard(
    icon: ImageVector,
    label: String,
    value: String,
    sublabel: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = sublabel,
            color = TextSecondary,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DailyActivityChartCard(
    days: List<DailyActivityPoint>,
    accent: Color
) {
    val maxSec = remember(days) {
        days.maxOfOrNull { maxOf(it.watchTimeSec, it.appTimeSec) }?.coerceAtLeast(60L) ?: 60L
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Recent Daily Activity (${days.size}d)",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LegendChip(label = "Watch", color = accent)
                LegendChip(label = "App", color = TextSecondary.copy(alpha = 0.55f))
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(126.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            days.forEach { day ->
                val watchRatio = (day.watchTimeSec.toFloat() / maxSec.toFloat()).coerceIn(0f, 1f)
                val appRatio = (day.appTimeSec.toFloat() / maxSec.toFloat()).coerceIn(0f, 1f)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        text = if (day.watchTimeSec > 0L) UsageStatsCalculator.formatDuration(day.watchTimeSec) else "",
                        color = accent,
                        fontSize = 9.sp,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Box(
                            modifier = Modifier
                                .width(if (days.size > 7) 5.dp else 10.dp)
                                .fillMaxHeight(watchRatio.coerceAtLeast(if (day.watchTimeSec > 0L) 0.06f else 0.02f))
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(if (day.watchTimeSec > 0L) accent else DarkBorder)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Box(
                            modifier = Modifier
                                .width(if (days.size > 7) 4.dp else 8.dp)
                                .fillMaxHeight(appRatio.coerceAtLeast(if (day.appTimeSec > 0L) 0.06f else 0.02f))
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(if (day.appTimeSec > 0L) TextSecondary.copy(alpha = 0.5f) else DarkBorder.copy(alpha = 0.6f))
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (days.size > 7 && day.shortLabel != "Today") day.dateKey.takeLast(2) else day.shortLabel.take(3),
                        color = if (day.shortLabel == "Today") accent else TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = if (day.shortLabel == "Today") FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendChip(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, color = TextSecondary, fontSize = 10.sp)
    }
}

@Composable
private fun TopAnimeStatsCard(
    items: List<AnimeWatchStats>,
    accent: Color,
    onAnimeClick: (String) -> Unit
) {
    val maxWatch = remember(items) { items.maxOfOrNull { it.watchTimeSec }?.coerceAtLeast(1L) ?: 1L }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Top Watched Anime",
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        if (items.isEmpty()) {
            Text(
                text = "Start watching an episode to build your per-series watch time.",
                color = TextSecondary,
                fontSize = 12.sp
            )
        } else {
            items.forEachIndexed { index, item ->
                val ratio = (item.watchTimeSec.toFloat() / maxWatch.toFloat()).coerceIn(0.04f, 1f)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onAnimeClick(item.animeId) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "#${index + 1}",
                        color = if (index == 0) accent else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(26.dp)
                    )
                    if (item.coverImage.isNotBlank()) {
                        AsyncImage(
                            model = item.coverImage,
                            contentDescription = item.animeTitle,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(width = 38.dp, height = 52.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.animeTitle.ifBlank { "Anime #${item.animeId}" },
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "${UsageStatsCalculator.formatDuration(item.watchTimeSec)} • ${item.episodesWatchedCount} ep${if (item.episodesWatchedCount == 1) "" else "s"}",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(DarkBorder)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(ratio)
                                    .fillMaxHeight()
                                    .background(accent)
                            )
                        }
                    }
                }
                if (index < items.lastIndex) {
                    HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
private fun TopGenresStatsCard(
    genres: List<GenreWatchStat>,
    accent: Color
) {
    val maxSec = remember(genres) { genres.maxOfOrNull { it.watchTimeSec }?.coerceAtLeast(1L) ?: 1L }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Top Genres",
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        genres.forEach { g ->
            val ratio = (g.watchTimeSec.toFloat() / maxSec.toFloat()).coerceIn(0.05f, 1f)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = g.genre,
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${UsageStatsCalculator.formatDuration(g.watchTimeSec)} (${g.animeCount} series)",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(DarkBorder)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(ratio)
                            .fillMaxHeight()
                            .background(accent)
                    )
                }
            }
        }
    }
}
