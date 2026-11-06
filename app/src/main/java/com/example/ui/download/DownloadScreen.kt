package com.example.ui.download

import android.widget.Toast
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.ripple
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.DeleteOutline
import android.os.Environment
import android.os.StatFs
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.DownloadItem
import com.example.data.model.Episode
import com.example.data.repository.AnimeRepository
import com.example.ui.theme.AccentRed
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.LocalThemeAccent
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun DownloadScreen(
    repository: AnimeRepository,
    onPlayClick: (String, Int) -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val themeAccent = LocalThemeAccent.current
    val bgColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface
    val downloads by repository.allDownloads.collectAsState(initial = emptyList())
    val allAnime by repository.allAnime.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var selectedItemForAction by remember { mutableStateOf<DownloadItem?>(null) }
    // Both the trash button and the long-press sheet route deletions through one confirmation, so
    // a file is never removed by a stray tap.
    var pendingDelete by remember { mutableStateOf<DownloadItem?>(null) }
    var pendingDeleteWatched by remember { mutableStateOf<Boolean?>(null) }

    val settings by repository.settings.collectAsState(initial = repository.settingsManager.getSettings())
    val animeById = remember(allAnime) { allAnime.associateBy { it.id } }

    fun displayTitleFor(item: DownloadItem): String {
        return animeById[item.animeId]?.displayTitle(settings.titleLanguagePreference)
            ?.ifBlank { item.animeTitle }
            ?: item.animeTitle
    }

    val shouldConfirmDelete = settings.confirmDestructiveActions || settings.confirmBeforeDeletingDownloads

    // Knowing whether the episode was finished decides how strongly to word the warning.
    LaunchedEffect(pendingDelete) {
        val target = pendingDelete
        pendingDeleteWatched = if (target == null) null
        else repository.getProgressForEpisode(target.animeId, target.episodeNumber)?.completed == true
    }

    val performDelete: (DownloadItem) -> Unit = { target ->
        scope.launch {
            repository.deleteDownload(target.id)
            Toast.makeText(
                context,
                "Deleted ${displayTitleFor(target)} Episode ${target.episodeNumber} from offline storage",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val handleItemClick: (DownloadItem) -> Unit = { item ->
        when (item.status) {
            "COMPLETED" -> onPlayClick(item.animeId, item.episodeNumber)
            "FAILED" -> {
                val matchedAnime = animeById[item.animeId]
                if (matchedAnime != null) {
                    scope.launch {
                        Toast.makeText(
                            context,
                            "Retrying Episode ${item.episodeNumber} download...",
                            Toast.LENGTH_SHORT
                        ).show()
                        val ep = Episode(
                            id = "${item.animeId}_${item.episodeNumber}",
                            animeId = item.animeId,
                            number = item.episodeNumber,
                            title = "Episode ${item.episodeNumber}",
                            thumbnail = item.thumbnail
                        )
                        val res = repository.addDownload(matchedAnime, ep, item.quality)
                        if (!res.started) {
                            Toast.makeText(
                                context,
                                res.error ?: "Download retry failed. Try again from the episode page.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {
                    Toast.makeText(
                        context,
                        "Download failed for ${displayTitleFor(item)} Ep ${item.episodeNumber}. Long-press to delete or retry.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            "DOWNLOADING", "PENDING", "QUEUED" -> {
                Toast.makeText(
                    context,
                    "Downloading ${displayTitleFor(item)} Ep ${item.episodeNumber} (${item.progress}%)...",
                    Toast.LENGTH_SHORT
                ).show()
            }
            else -> {
                Toast.makeText(
                    context,
                    "Status for ${displayTitleFor(item)} Ep ${item.episodeNumber}: ${item.status}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_zenith_logo),
                contentDescription = "Zenith Anime Emblem",
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "Download",
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.weight(1f))

            IconButton(
                onClick = onSearchClick,
                modifier = Modifier.testTag("download_search_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // Device Storage Meter Card (recalculated dynamically whenever downloads change)
        val storageTrigger = remember(downloads) {
            downloads.map { "${it.id}:${it.status}:${it.fileSize}:${it.progress}" }
        }
        val (totalGb, freeGb, usedPercent) = remember(storageTrigger) {
            try {
                val stat = StatFs(Environment.getDataDirectory().path)
                val total = stat.totalBytes.toDouble() / (1024 * 1024 * 1024)
                val free = stat.availableBytes.toDouble() / (1024 * 1024 * 1024)
                val used = (total - free).coerceAtLeast(0.0)
                val pct = if (total > 0) ((used / total) * 100).toInt() else 0
                Triple(total, free, pct)
            } catch (_: Exception) {
                Triple(64.0, 32.0, 50)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(surfaceColor)
                .border(0.5.dp, DarkBorder, RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Device Storage",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "${String.format("%.1f", freeGb)} GB Available of ${String.format("%.1f", totalGb)} GB",
                        color = themeAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { (usedPercent / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = themeAccent,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row {
                    Text(
                        text = "$usedPercent% Used",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "${100 - usedPercent}% Free",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        if (downloads.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No offline downloads yet",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Download anime episodes from series pages to watch anytime without internet.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onSearchClick,
                        colors = ButtonDefaults.buttonColors(containerColor = themeAccent, contentColor = bgColor),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("Explore Anime", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
            ) {
                items(downloads, key = { it.id }) { item ->
                    DownloadCardItem(
                        item = item,
                        displayTitle = displayTitleFor(item),
                        onPlayClick = { handleItemClick(item) },
                        onLongClick = { selectedItemForAction = item },
                        onDeleteClick = {
                            if (shouldConfirmDelete) pendingDelete = item else performDelete(item)
                        }
                    )
                }
            }
        }
    }

    if (selectedItemForAction != null) {
        val target = selectedItemForAction!!
        AlertDialog(
            onDismissRequest = { selectedItemForAction = null },
            containerColor = surfaceColor,
            title = {
                Text(
                    text = "${displayTitleFor(target)} - Ep ${target.episodeNumber}",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = "Quality: ${target.quality} • File Size: ${target.fileSize} • Status: ${target.status}",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedItemForAction = null
                        handleItemClick(target)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeAccent, contentColor = bgColor)
                ) {
                    val btnLabel = when (target.status) {
                        "COMPLETED" -> "Play Offline"
                        "FAILED" -> "Retry Download"
                        else -> "Check Progress"
                    }
                    Text(btnLabel, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        selectedItemForAction = null
                        if (shouldConfirmDelete) pendingDelete = target else performDelete(target)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed, contentColor = TextPrimary)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    pendingDelete?.let { target ->
        if (shouldConfirmDelete) {
            AlertDialog(
                onDismissRequest = { pendingDelete = null },
                containerColor = surfaceColor,
                title = { Text("Delete This Download?", color = AccentRed, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(
                            text = displayTitleFor(target) + " Episode " + target.episodeNumber +
                                " (" + target.quality + ", " + target.fileSize +
                                ") will be removed from this device. You can download it again later.",
                            color = TextPrimary,
                            fontSize = 13.sp
                        )
                        if (target.status == "DOWNLOADING" || target.status == "PENDING") {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "This download is still in progress, so it will be cancelled.",
                                color = AccentRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (settings.confirmBeforeDeletingDownloads && pendingDeleteWatched == false) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "You have not finished watching this episode yet.",
                                color = AccentRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            performDelete(target)
                            pendingDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentRed, contentColor = TextPrimary)
                    ) {
                        Text("Delete", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDelete = null }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DownloadCardItem(
    item: DownloadItem,
    displayTitle: String = item.animeTitle,
    onPlayClick: () -> Unit,
    onLongClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val themeAccent = LocalThemeAccent.current
    val surfaceColor = MaterialTheme.colorScheme.surface
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scaleAnim by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "download_item_scale"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .scale(scaleAnim)
            .clip(RoundedCornerShape(12.dp))
            .background(surfaceColor)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = themeAccent),
                onClick = onPlayClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
            .padding(10.dp)
            .testTag("download_item_${item.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 110.dp, height = 75.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            AsyncImage(
                model = item.thumbnail,
                contentDescription = displayTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(Color(0x99000000), CircleShape)
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                if (item.status == "DOWNLOADING" || item.status == "PENDING") {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Downloading",
                        tint = themeAccent,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play offline",
                        tint = if (item.status == "FAILED") Color(0xFFEF4444) else themeAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = displayTitle,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "Episode ${item.episodeNumber}",
                color = TextSecondary,
                fontSize = 12.sp
            )

            if (item.status == "DOWNLOADING" || item.status == "PENDING" || item.status == "QUEUED") {
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { (item.progress / 100f).coerceIn(0f, 1f) },
                    color = themeAccent,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (item.status == "PENDING" || item.status == "QUEUED") "Queued..." else "Downloading ${item.progress}%",
                        color = themeAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = item.fileSize,
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(themeAccent, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.quality,
                            color = Color.Black,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = if (item.status == "FAILED") "Download Failed • Tap to Retry" else item.fileSize,
                        color = if (item.status == "FAILED") Color(0xFFEF4444) else TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        IconButton(
            onClick = onDeleteClick,
            modifier = Modifier.testTag("delete_download_${item.id}")
        ) {
            Icon(
                imageVector = Icons.Outlined.DeleteOutline,
                contentDescription = "Delete download",
                tint = themeAccent,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
