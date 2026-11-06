package com.example.ui.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.example.data.model.Anime
import com.example.data.model.WatchStatus
import com.example.data.util.ShareUtils
import com.example.ui.theme.AccentRed
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.LocalAppSettings
import com.example.ui.theme.LocalThemeAccent
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AnimeQuickActionDialog(
    anime: Anime,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleWatchlist: () -> Unit,
    onDownload: (() -> Unit)? = null,
    onToggleCompleted: (() -> Unit)? = null,
    onChangeWatchStatus: ((WatchStatus) -> Unit)? = null,
    onViewDetails: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val themeAccent = LocalThemeAccent.current
    val appSettings = LocalAppSettings.current
    val displayTitle = anime.displayTitle(appSettings.titleLanguagePreference)
    val activeStatus = WatchStatus.fromRaw(anime.watchStatus)
    val surfaceColor = MaterialTheme.colorScheme.surface
    val surfaceVarColor = MaterialTheme.colorScheme.surfaceVariant
    val bgColor = MaterialTheme.colorScheme.background

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = surfaceColor,
        modifier = modifier.clip(RoundedCornerShape(18.dp)),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                AsyncImage(
                    model = anime.coverImage,
                    contentDescription = displayTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(width = 50.dp, height = 70.dp)
                        .clip(RoundedCornerShape(8.dp))
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayTitle,
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "${anime.type} • ${anime.year} • ★ ${anime.rating}",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                androidx.compose.material3.HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)

                if (onChangeWatchStatus != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "WATCH STATUS",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        WatchStatus.values().forEach { statusOption ->
                            val isSelected = anime.isInWatchlist && activeStatus == statusOption
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isSelected) themeAccent else surfaceVarColor)
                                    .border(
                                        width = 0.5.dp,
                                        color = if (isSelected) themeAccent else DarkBorder,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable {
                                        onDismiss()
                                        onChangeWatchStatus(statusOption)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = statusOption.displayName,
                                    color = if (isSelected) bgColor else TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    androidx.compose.material3.HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                }

                QuickActionRow(
                    icon = Icons.Default.PlayArrow,
                    title = "Play Episode 1",
                    tint = themeAccent,
                    onClick = {
                        onDismiss()
                        onPlay()
                    }
                )

                if (onViewDetails != null) {
                    QuickActionRow(
                        icon = Icons.Default.Info,
                        title = "View Anime Details",
                        tint = TextPrimary,
                        onClick = {
                            onDismiss()
                            onViewDetails()
                        }
                    )
                }

                QuickActionRow(
                    icon = if (anime.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    title = if (anime.isFavorite) "Remove from Favorites" else "Add to Favorites",
                    tint = if (anime.isFavorite) AccentRed else TextPrimary,
                    onClick = {
                        onDismiss()
                        onToggleFavorite()
                    }
                )

                QuickActionRow(
                    icon = if (anime.isInWatchlist) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    title = if (anime.isInWatchlist) "Remove from Watchlist" else "Save to Watchlist",
                    tint = if (anime.isInWatchlist) themeAccent else TextPrimary,
                    onClick = {
                        onDismiss()
                        onToggleWatchlist()
                    }
                )

                if (onToggleCompleted != null) {
                    QuickActionRow(
                        icon = Icons.Default.CheckCircle,
                        title = if (anime.isCompleted) "Mark as Watching" else "Mark as Completed",
                        tint = if (anime.isCompleted) themeAccent else TextPrimary,
                        onClick = {
                            onDismiss()
                            onToggleCompleted()
                        }
                    )
                }

                if (onDownload != null) {
                    QuickActionRow(
                        icon = Icons.Default.Download,
                        title = "Download for Offline Play",
                        tint = TextPrimary,
                        onClick = {
                            onDismiss()
                            onDownload()
                        }
                    )
                }

                QuickActionRow(
                    icon = Icons.Default.Share,
                    title = "Share Anime",
                    tint = TextPrimary,
                    onClick = {
                        onDismiss()
                        if (onShare != null) {
                            onShare()
                        } else {
                            ShareUtils.shareAnime(
                                context = context,
                                anime = anime,
                                titleLanguagePreference = appSettings.titleLanguagePreference
                            )
                        }
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = themeAccent, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun QuickActionRow(
    icon: ImageVector,
    title: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
