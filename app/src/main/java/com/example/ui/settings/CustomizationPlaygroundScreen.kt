package com.example.ui.settings

import android.widget.Toast
import com.example.ui.player.ZenithVideoSeekBar
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppSettings
import com.example.data.repository.AnimeRepository
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.LocalThemeAccent
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

private data class PlayerControlDef(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val description: String
)

private val ALL_PLAYER_CONTROLS = listOf(
    PlayerControlDef("audio", "Audio / Dub", Icons.Default.Audiotrack, "Switch sub/dub track"),
    PlayerControlDef("subtitles", "Subtitles", Icons.Default.Subtitles, "Toggle & caption settings"),
    PlayerControlDef("speed", "Playback Speed", Icons.Default.Speed, "Change video speed"),
    PlayerControlDef("servers", "Server / Quality", Icons.Default.Dns, "Stream provider & resolution"),
    PlayerControlDef("fit", "Aspect Ratio", Icons.Default.AspectRatio, "Fit, Cover, Stretch modes"),
    PlayerControlDef("sleep", "Sleep Timer", Icons.Default.NightlightRound, "Auto-stop timer"),
    PlayerControlDef("episodes", "Episodes Sheet", Icons.Default.VideoLibrary, "Browse episode catalog"),
    PlayerControlDef("pip", "Picture in Picture", Icons.Default.PictureInPictureAlt, "Floating mini player"),
    PlayerControlDef("fullscreen", "Fullscreen", Icons.Default.Fullscreen, "Expand full landscape")
)

@Composable
fun CustomizationPlaygroundScreen(
    repository: AnimeRepository,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val savedSettings by repository.settings.collectAsState(initial = repository.settingsManager.getSettings())

    var selectedTab by remember { mutableStateOf(0) } // 0: Video Player, 1: App UI & Themes

    // Draft player layout state
    var draftTopBarOrder by remember(savedSettings.playerTopBarOrder) {
        mutableStateOf(
            savedSettings.playerTopBarOrder.split(",")
                .map { it.trim().lowercase() }
                .filter { it.isNotEmpty() }
        )
    }
    var draftHiddenControls by remember(savedSettings.playerHiddenControls) {
        mutableStateOf(
            savedSettings.playerHiddenControls.split(",")
                .map { it.trim().lowercase() }
                .filter { it.isNotEmpty() }
                .toSet()
        )
    }
    var draftTransportPosition by remember(savedSettings.playerTransportPosition) {
        mutableStateOf(savedSettings.playerTransportPosition)
    }
    var draftControlSize by remember(savedSettings.playerControlSize) {
        mutableStateOf(savedSettings.playerControlSize)
    }
    var draftAccentOverride by remember(savedSettings.playerAccentOverride) {
        mutableStateOf(savedSettings.playerAccentOverride)
    }
    var draftSeekbarStyle by remember(savedSettings.playerSeekbarStyle) {
        mutableStateOf(savedSettings.playerSeekbarStyle)
    }
    var draftLockSide by remember(savedSettings.lockButtonSide) {
        mutableStateOf(savedSettings.lockButtonSide)
    }
    var draftOverlayDim by remember(savedSettings.playerOverlayDimAlpha) {
        mutableFloatStateOf(savedSettings.playerOverlayDimAlpha)
    }
    var draftLegacyLayout by remember(savedSettings.useLegacyPlayerLayout) {
        mutableStateOf(savedSettings.useLegacyPlayerLayout)
    }

    // Draft general UI state
    var draftThemeAccent by remember(savedSettings.themeAccent) {
        mutableStateOf(savedSettings.themeAccent)
    }
    var draftEpisodeLayout by remember(savedSettings.episodeLayout) {
        mutableStateOf(savedSettings.episodeLayout)
    }
    var draftAmoled by remember(savedSettings.amoledMode) {
        mutableStateOf(savedSettings.amoledMode)
    }
    var draftReduceAnimations by remember(savedSettings.reduceAnimations) {
        mutableStateOf(savedSettings.reduceAnimations)
    }
    var draftContinueWatchingOneCard by remember(savedSettings.continueWatchingOneCard) {
        mutableStateOf(savedSettings.continueWatchingOneCard)
    }
    var draftContinueWatchingLimit by remember(savedSettings.continueWatchingLimit) {
        mutableStateOf(savedSettings.continueWatchingLimit)
    }
    var draftNavBarStyle by remember(savedSettings.navBarStyle) {
        mutableStateOf(savedSettings.navBarStyle)
    }
    var draftGridColumns by remember(savedSettings.gridColumnsCount) {
        mutableIntStateOf(savedSettings.gridColumnsCount)
    }
    var draftCardCornerRadius by remember(savedSettings.cardCornerRadiusDp) {
        mutableIntStateOf(savedSettings.cardCornerRadiusDp)
    }
    var draftFontScale by remember(savedSettings.fontScaleMultiplier) {
        mutableFloatStateOf(savedSettings.fontScaleMultiplier)
    }
    var draftShowRatingBadges by remember(savedSettings.showRatingBadgesOnCards) {
        mutableStateOf(savedSettings.showRatingBadgesOnCards)
    }
    var draftShowEpisodeBadges by remember(savedSettings.showEpisodeBadgesOnCards) {
        mutableStateOf(savedSettings.showEpisodeBadgesOnCards)
    }
    var draftShowQualityBadges by remember(savedSettings.showQualityBadgesOnCards) {
        mutableStateOf(savedSettings.showQualityBadgesOnCards)
    }

    val activeAccentColor = when (draftAccentOverride) {
        "Crimson" -> Color(0xFFFF5252)
        "Cyan" -> Color(0xFF00E5FF)
        "Emerald" -> Color(0xFF06C149)
        "Sakura" -> Color(0xFFFF4081)
        "Amber" -> Color(0xFFFFB300)
        "Violet" -> Color(0xFFB388FF)
        "Indigo" -> Color(0xFF536DFE)
        "Gold" -> Color(0xFFFFD54F)
        else -> when (draftThemeAccent) {
            "Cyan" -> Color(0xFF00E5FF)
            "Sakura" -> Color(0xFFFF4081)
            "Violet" -> Color(0xFFB388FF)
            "Amber" -> Color(0xFFFFB300)
            "Crimson" -> Color(0xFFFF5252)
            "Indigo" -> Color(0xFF536DFE)
            "Gold" -> Color(0xFFFFD54F)
            else -> Color(0xFF06C149)
        }
    }

    fun applyChanges() {
        scope.launch {
            repository.settingsManager.update { s ->
                s.copy(
                    playerTopBarOrder = draftTopBarOrder.joinToString(","),
                    playerHiddenControls = draftHiddenControls.joinToString(","),
                    playerTransportPosition = draftTransportPosition,
                    playerControlSize = draftControlSize,
                    playerAccentOverride = draftAccentOverride,
                    playerSeekbarStyle = draftSeekbarStyle,
                    lockButtonSide = draftLockSide,
                    playerOverlayDimAlpha = draftOverlayDim,
                    useLegacyPlayerLayout = draftLegacyLayout,
                    themeAccent = draftThemeAccent,
                    episodeLayout = draftEpisodeLayout,
                    amoledMode = draftAmoled,
                    reduceAnimations = draftReduceAnimations,
                    continueWatchingOneCard = draftContinueWatchingOneCard,
                    continueWatchingLimit = draftContinueWatchingLimit,
                    navBarStyle = draftNavBarStyle,
                    gridColumnsCount = draftGridColumns,
                    cardCornerRadiusDp = draftCardCornerRadius,
                    fontScaleMultiplier = draftFontScale,
                    showRatingBadgesOnCards = draftShowRatingBadges,
                    showEpisodeBadgesOnCards = draftShowEpisodeBadges,
                    showQualityBadgesOnCards = draftShowQualityBadges
                )
            }
            Toast.makeText(context, "Customization settings applied successfully!", Toast.LENGTH_SHORT).show()
        }
    }

    fun resetToDefaults() {
        val def = AppSettings()
        if (selectedTab == 0) {
            draftTopBarOrder = def.playerTopBarOrder.split(",").map { it.trim() }
            draftHiddenControls = emptySet()
            draftTransportPosition = def.playerTransportPosition
            draftControlSize = def.playerControlSize
            draftAccentOverride = def.playerAccentOverride
            draftSeekbarStyle = def.playerSeekbarStyle
            draftLockSide = def.lockButtonSide
            draftOverlayDim = def.playerOverlayDimAlpha
            draftLegacyLayout = false
            Toast.makeText(context, "Player layout reset to defaults", Toast.LENGTH_SHORT).show()
        } else {
            draftThemeAccent = def.themeAccent
            draftEpisodeLayout = def.episodeLayout
            draftAmoled = def.amoledMode
            draftReduceAnimations = def.reduceAnimations
            draftContinueWatchingOneCard = def.continueWatchingOneCard
            draftContinueWatchingLimit = def.continueWatchingLimit
            draftNavBarStyle = def.navBarStyle
            draftGridColumns = def.gridColumnsCount
            draftCardCornerRadius = def.cardCornerRadiusDp
            draftFontScale = def.fontScaleMultiplier
            draftShowRatingBadges = def.showRatingBadgesOnCards
            draftShowEpisodeBadges = def.showEpisodeBadgesOnCards
            draftShowQualityBadges = def.showQualityBadgesOnCards
            Toast.makeText(context, "UI options reset to defaults", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .testTag("customization_playground_screen")
    ) {
        // Top app bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.testTag("playground_back_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "UI & Player Playground",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Live preview before applying customizations",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = activeAccentColor,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = activeAccentColor
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Video Player Layout", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("App UI & Themes", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
        }

        // Live preview container at top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF0B0E14))
                .border(1.dp, activeAccentColor.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                .padding(8.dp)
        ) {
            if (selectedTab == 0) {
                PlayerLivePreview(
                    topBarOrder = draftTopBarOrder,
                    hiddenControls = draftHiddenControls,
                    transportPosition = draftTransportPosition,
                    controlSize = draftControlSize,
                    accentColor = activeAccentColor,
                    seekbarStyle = draftSeekbarStyle,
                    lockSide = draftLockSide,
                    overlayDim = draftOverlayDim,
                    isLegacy = draftLegacyLayout
                )
            } else {
                GeneralUiLivePreview(
                    themeAccent = activeAccentColor,
                    episodeLayout = draftEpisodeLayout,
                    isAmoled = draftAmoled,
                    oneCardContinue = draftContinueWatchingOneCard
                )
            }
        }

        // Controls editor (scrollable)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (selectedTab == 0) {
                Text("CONTROLS VISIBILITY & REORDERING", color = activeAccentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Reorder player buttons or toggle visibility:", color = TextSecondary, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(8.dp))

                draftTopBarOrder.forEachIndexed { index, controlId ->
                    val def = ALL_PLAYER_CONTROLS.firstOrNull { it.id == controlId } ?: PlayerControlDef(controlId, controlId.uppercase(), Icons.Default.Star, "")
                    val isHidden = controlId in draftHiddenControls
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isHidden) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant)
                            .border(0.5.dp, if (!isHidden) activeAccentColor.copy(alpha = 0.4f) else DarkBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(def.icon, contentDescription = null, tint = if (isHidden) TextSecondary else activeAccentColor, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(def.name, color = if (isHidden) TextSecondary else TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(def.description, color = TextSecondary, fontSize = 10.sp)
                        }
                        IconButton(
                            onClick = {
                                if (index > 0) {
                                    val list = draftTopBarOrder.toMutableList()
                                    val temp = list[index]
                                    list[index] = list[index - 1]
                                    list[index - 1] = temp
                                    draftTopBarOrder = list
                                }
                            },
                            enabled = index > 0,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move Left", tint = if (index > 0) Color.White else Color.Gray, modifier = Modifier.size(18.dp))
                        }
                        IconButton(
                            onClick = {
                                if (index < draftTopBarOrder.size - 1) {
                                    val list = draftTopBarOrder.toMutableList()
                                    val temp = list[index]
                                    list[index] = list[index + 1]
                                    list[index + 1] = temp
                                    draftTopBarOrder = list
                                }
                            },
                            enabled = index < draftTopBarOrder.size - 1,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move Right", tint = if (index < draftTopBarOrder.size - 1) Color.White else Color.Gray, modifier = Modifier.size(18.dp))
                        }
                        IconButton(
                            onClick = {
                                draftHiddenControls = if (isHidden) draftHiddenControls - controlId else draftHiddenControls + controlId
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (isHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle visibility",
                                tint = if (isHidden) Color.Gray else activeAccentColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(14.dp))

                Text("TRANSPORT CONTROLS POSITION", color = activeAccentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Center", "Bottom", "Split").forEach { pos ->
                        val isSel = draftTransportPosition.equals(pos, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) activeAccentColor else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { draftTransportPosition = pos }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(pos, color = if (isSel) MaterialTheme.colorScheme.background else TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text("PLAYER CONTROL SIZE", color = activeAccentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Compact", "Standard", "Large").forEach { sizeOpt ->
                        val isSel = draftControlSize.equals(sizeOpt, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) activeAccentColor else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { draftControlSize = sizeOpt }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(sizeOpt, color = if (isSel) MaterialTheme.colorScheme.background else TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text("SEEKBAR TIMELINE STYLE", color = activeAccentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Modern", "Minimal", "Thick", "Neon", "Wave").forEach { styleOpt ->
                        val isSel = draftSeekbarStyle.equals(styleOpt, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) activeAccentColor else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { draftSeekbarStyle = styleOpt }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(styleOpt, color = if (isSel) MaterialTheme.colorScheme.background else TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text("LETTERBOX LOCK BUTTON SIDE", color = activeAccentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Left", "Right").forEach { sideOpt ->
                        val isSel = draftLockSide.equals(sideOpt, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) activeAccentColor else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { draftLockSide = sideOpt }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(sideOpt, color = if (isSel) MaterialTheme.colorScheme.background else TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text("PLAYER ACCENT COLOR OVERRIDE", color = activeAccentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Theme", "Emerald", "Cyan", "Sakura", "Violet", "Amber", "Crimson", "Indigo", "Gold").forEach { colName ->
                        val isSel = draftAccentOverride.equals(colName, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) activeAccentColor else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { draftAccentOverride = colName }
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(colName, color = if (isSel) MaterialTheme.colorScheme.background else TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Legacy Video Player Layout", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Switch to classic layout with center lock control", color = TextSecondary, fontSize = 11.sp)
                    }
                    Switch(
                        checked = draftLegacyLayout,
                        onCheckedChange = { draftLegacyLayout = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.background, checkedTrackColor = activeAccentColor)
                    )
                }
            } else {
                // General UI Tab
                Text("THEME ACCENT COLOR", color = activeAccentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Emerald", "Cyan", "Sakura", "Violet", "Amber", "Crimson", "Indigo", "Gold").forEach { colorName ->
                        val isSel = draftThemeAccent.equals(colorName, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) activeAccentColor else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { draftThemeAccent = colorName }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(colorName, color = if (isSel) MaterialTheme.colorScheme.background else TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("NAVIGATION BAR STYLE", color = activeAccentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Standard", "Compact", "Floating").forEach { style ->
                        val isSel = draftNavBarStyle.equals(style, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) activeAccentColor else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { draftNavBarStyle = style }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(style, color = if (isSel) MaterialTheme.colorScheme.background else TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("GRID COLUMNS COUNT", color = activeAccentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(2 to "2 Columns", 3 to "3 Columns").forEach { (cols, label) ->
                        val isSel = draftGridColumns == cols
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) activeAccentColor else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { draftGridColumns = cols }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, color = if (isSel) MaterialTheme.colorScheme.background else TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("CARD CORNER RADIUS: ${draftCardCornerRadius}dp", color = activeAccentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                }
                Slider(
                    value = draftCardCornerRadius.toFloat(),
                    onValueChange = { draftCardCornerRadius = it.toInt() },
                    valueRange = 4f..24f,
                    steps = 10,
                    colors = SliderDefaults.colors(thumbColor = activeAccentColor, activeTrackColor = activeAccentColor)
                )

                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("FONT SCALE MULTIPLIER: ${String.format(java.util.Locale.US, "%.2fx", draftFontScale)}", color = activeAccentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                }
                Slider(
                    value = draftFontScale,
                    onValueChange = { draftFontScale = it },
                    valueRange = 0.85f..1.25f,
                    steps = 8,
                    colors = SliderDefaults.colors(thumbColor = activeAccentColor, activeTrackColor = activeAccentColor)
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text("SERIES EPISODES PRESENTATION", color = activeAccentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Cards", "Numbers", "Grid").forEach { epLayout ->
                        val isSel = draftEpisodeLayout.equals(epLayout, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) activeAccentColor else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { draftEpisodeLayout = epLayout }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(epLayout, color = if (isSel) MaterialTheme.colorScheme.background else TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("AMOLED Deep Black Background", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Use absolute black (#000000) for OLED battery savings", color = TextSecondary, fontSize = 11.sp)
                    }
                    Switch(
                        checked = draftAmoled,
                        onCheckedChange = { draftAmoled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.background, checkedTrackColor = activeAccentColor)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Show Rating Badges on Cards", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Display score pill in top corner of cards", color = TextSecondary, fontSize = 11.sp)
                    }
                    Switch(
                        checked = draftShowRatingBadges,
                        onCheckedChange = { draftShowRatingBadges = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.background, checkedTrackColor = activeAccentColor)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Show Episode Badges on Cards", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Display total available episode count on posters", color = TextSecondary, fontSize = 11.sp)
                    }
                    Switch(
                        checked = draftShowEpisodeBadges,
                        onCheckedChange = { draftShowEpisodeBadges = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.background, checkedTrackColor = activeAccentColor)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Show Quality Badges on Cards", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Display HD / Quality badge on posters", color = TextSecondary, fontSize = 11.sp)
                    }
                    Switch(
                        checked = draftShowQualityBadges,
                        onCheckedChange = { draftShowQualityBadges = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.background, checkedTrackColor = activeAccentColor)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Continue Watching Single Card", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Collapse continue watching to a single card per anime", color = TextSecondary, fontSize = 11.sp)
                    }
                    Switch(
                        checked = draftContinueWatchingOneCard,
                        onCheckedChange = { draftContinueWatchingOneCard = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.background, checkedTrackColor = activeAccentColor)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Reduce Animations", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Prefer immediate transitions over animated motion", color = TextSecondary, fontSize = 11.sp)
                    }
                    Switch(
                        checked = draftReduceAnimations,
                        onCheckedChange = { draftReduceAnimations = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.background, checkedTrackColor = activeAccentColor)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Bottom action bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { resetToDefaults() },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, DarkBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset", fontSize = 12.sp, maxLines = 1)
                }

                OutlinedButton(
                    onClick = onBackClick,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, DarkBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel", fontSize = 12.sp, maxLines = 1)
                }

                Button(
                    onClick = { applyChanges() },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = activeAccentColor, contentColor = MaterialTheme.colorScheme.background),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1.2f).testTag("playground_apply_button")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Apply", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun PlayerLivePreview(
    topBarOrder: List<String>,
    hiddenControls: Set<String>,
    transportPosition: String,
    controlSize: String,
    accentColor: Color,
    seekbarStyle: String,
    lockSide: String,
    overlayDim: Float,
    isLegacy: Boolean
) {
    val scaleMult = when (controlSize) {
        "Compact" -> 0.82f
        "Large" -> 1.15f
        else -> 1f
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black)
    ) {
        // Scrim overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = overlayDim.coerceIn(0.2f, 0.85f)))
        )

        // Lock button in letterbox
        if (!isLegacy) {
            val align = if (lockSide.equals("Right", ignoreCase = true)) Alignment.CenterEnd else Alignment.CenterStart
            Box(
                modifier = Modifier
                    .align(align)
                    .padding(horizontal = 6.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0xCC0D111A))
                    .border(1.dp, accentColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.LockOpen, contentDescription = null, tint = accentColor, modifier = Modifier.size(12.dp))
            }
        }

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Zenith Player Preview", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1)

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isLegacy) {
                    Icon(Icons.Default.Speed, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                    Icon(Icons.Default.Subtitles, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                    Icon(Icons.Default.LockOpen, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                    Icon(Icons.Default.Fullscreen, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                } else {
                    topBarOrder.forEach { id ->
                        if (id !in hiddenControls) {
                            val icon = when (id) {
                                "audio" -> Icons.Default.Audiotrack
                                "subtitles" -> Icons.Default.Subtitles
                                "speed" -> Icons.Default.Speed
                                "servers" -> Icons.Default.Dns
                                "fit" -> Icons.Default.AspectRatio
                                "sleep" -> Icons.Default.NightlightRound
                                "episodes" -> Icons.Default.VideoLibrary
                                "pip" -> Icons.Default.PictureInPictureAlt
                                else -> Icons.Default.Fullscreen
                            }
                            Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size((13f * scaleMult).dp).padding(horizontal = 1.dp))
                        }
                    }
                }
            }
        }

        // Center transport controls (if Center or Split)
        if (transportPosition != "Bottom") {
            Row(
                modifier = Modifier.align(Alignment.Center),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.FastRewind, contentDescription = null, tint = Color.White, modifier = Modifier.size((18f * scaleMult).dp))
                Box(
                    modifier = Modifier
                        .size((34f * scaleMult).dp)
                        .background(accentColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = DarkBackground, modifier = Modifier.size((20f * scaleMult).dp))
                }
                Icon(Icons.Default.FastForward, contentDescription = null, tint = Color.White, modifier = Modifier.size((18f * scaleMult).dp))
            }
        }

        // Bottom timeline bar
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            if (transportPosition == "Bottom") {
                Row(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.FastRewind, contentDescription = null, tint = Color.White, modifier = Modifier.size((15f * scaleMult).dp))
                    Box(
                        modifier = Modifier
                            .size((26f * scaleMult).dp)
                            .background(accentColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = DarkBackground, modifier = Modifier.size((15f * scaleMult).dp))
                    }
                    Icon(Icons.Default.FastForward, contentDescription = null, tint = Color.White, modifier = Modifier.size((15f * scaleMult).dp))
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Interactive Seekbar representation
            var simTimeSec by remember { androidx.compose.runtime.mutableLongStateOf(540L) }
            var simScrubbing by remember { mutableStateOf(false) }
            var simScrubSec by remember { androidx.compose.runtime.mutableLongStateOf(540L) }
            fun formatSimTime(sec: Long): String {
                val m = sec / 60L
                val s = sec % 60L
                return "%02d:%02d".format(m, s)
            }
            val shownSec = if (simScrubbing) simScrubSec else simTimeSec
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(formatSimTime(shownSec), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                ZenithVideoSeekBar(
                    currentTimeSec = simTimeSec,
                    durationSec = 1440L,
                    bufferedSec = (simTimeSec + 280L).coerceAtMost(1440L),
                    isScrubbing = simScrubbing,
                    scrubPositionSec = simScrubSec,
                    seekbarStyle = seekbarStyle,
                    accentColor = accentColor,
                    introRangeSec = 85L..175L,
                    outroRangeSec = 1320L..1440L,
                    enabled = true,
                    onScrubStart = { pos ->
                        simScrubSec = pos
                        simScrubbing = true
                    },
                    onScrubChange = { pos ->
                        simScrubSec = pos
                        simScrubbing = true
                    },
                    onScrubFinished = { pos ->
                        simTimeSec = pos
                        simScrubSec = pos
                        simScrubbing = false
                    },
                    formatTime = ::formatSimTime,
                    modifier = Modifier.weight(1f)
                )
                Text("24:00", color = Color.White.copy(alpha = 0.7f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun GeneralUiLivePreview(
    themeAccent: Color,
    episodeLayout: String,
    isAmoled: Boolean,
    oneCardContinue: Boolean
) {
    val bgColor = if (isAmoled) Color.Black else DarkBackground
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(themeAccent, RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Zenith Anime Home UI", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "EPISODES: $episodeLayout",
                color = themeAccent,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Anime card mock
            Box(
                modifier = Modifier
                    .width(75.dp)
                    .height(105.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, themeAccent.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(6.dp)
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(55.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF1E2433))
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Anime Card", color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text("★ 8.9", color = themeAccent, fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Continue watching mock
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (oneCardContinue) "Continue Watching (1 Card)" else "Continue Watching (Carousel)",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(0.5.dp, DarkBorder, RoundedCornerShape(8.dp))
                        .padding(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(themeAccent.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = themeAccent, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("Episode 4", color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("Progress 65%", color = TextSecondary, fontSize = 8.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(16.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(themeAccent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("EP ${it + 1}", color = themeAccent, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
