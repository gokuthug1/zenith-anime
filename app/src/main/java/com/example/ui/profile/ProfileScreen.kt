package com.example.ui.profile

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.ripple
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.provider.ProviderManager
import com.example.data.repository.AnimeRepository
import com.example.ui.theme.AccentRed
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.LocalThemeAccent
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.io.File
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.VideoLibrary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AvatarPreset(
    val name: String,
    val url: String,
    val type: String = "Image"
)

val AVATAR_PRESETS = listOf(
    AvatarPreset("Cyberpunk", "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=300&auto=format&fit=crop&q=80", "JPG"),
    AvatarPreset("Sakura Spirit", "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=300&auto=format&fit=crop&q=80", "JPG"),
    AvatarPreset("Night Walker", "https://images.unsplash.com/photo-1563089145-599997674d42?w=300&auto=format&fit=crop&q=80", "JPG"),
    AvatarPreset("Synth Wave", "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=300&auto=format&fit=crop&q=80", "JPG"),
    AvatarPreset("Pixel Cat", "https://media.giphy.com/media/JIX9t2j0ZTN9S/giphy.gif", "GIF"),
    AvatarPreset("Pikachu Vector", "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/dream-world/25.svg", "SVG"),
    AvatarPreset("Eevee Vector", "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/dream-world/133.svg", "SVG")
)

fun sanitizeSvgText(content: String): Boolean {
    val lower = content.lowercase(java.util.Locale.US)
    val prohibited = listOf("<script", "javascript:", "onload", "onerror", "onclick", "onmouseover", "<!entity")
    return prohibited.none { lower.contains(it) }
}

fun validateAvatarUrl(url: String): Pair<Boolean, String?> {
    val trimmed = url.trim()
    if (trimmed.isEmpty()) return true to null
    if (!trimmed.startsWith("http://", ignoreCase = true) && !trimmed.startsWith("https://", ignoreCase = true)) {
        return false to "URL must begin with http:// or https://"
    }
    return try {
        val uri = java.net.URI(trimmed)
        if (uri.host.isNullOrBlank()) {
            false to "Invalid domain host"
        } else {
            true to null
        }
    } catch (_: Throwable) {
        false to "Malformed URL format"
    }
}

fun getEffectiveAvatarModel(avatarPath: String?, avatarUrl: String? = null, avatarSvgContent: String? = null): Any {
    val path = avatarPath?.trim().orEmpty()
    val url = avatarUrl?.trim().orEmpty()
    val svg = avatarSvgContent?.trim().orEmpty()
    return when {
        path.isNotEmpty() -> {
            if (path.startsWith("http://", ignoreCase = true) || path.startsWith("https://", ignoreCase = true)) {
                path
            } else if (path.startsWith("content://", ignoreCase = true)) {
                android.net.Uri.parse(path)
            } else {
                File(path)
            }
        }
        url.isNotEmpty() -> url
        svg.isNotEmpty() -> svg.toByteArray(Charsets.UTF_8)
        else -> "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=300&auto=format&fit=crop&q=80"
    }
}

fun saveBackupToDeviceStorage(context: android.content.Context, json: String): String? {
    return try {
        val fileName = "zenith_anime_backup_${System.currentTimeMillis()}.json"
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/json")
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = context.contentResolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                }
                "Saved to Downloads/$fileName"
            } else null
        } else {
            val dir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            dir.mkdirs()
            val file = File(dir, fileName)
            file.writeText(json, Charsets.UTF_8)
            "Saved to Downloads/${file.name}"
        }
    } catch (_: Exception) {
        null
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProfileScreen(
    repository: AnimeRepository,
    onMalSyncClick: () -> Unit = {},
    onPlayerSettingsClick: () -> Unit = {},
    onSubtitleSettingsClick: () -> Unit = {},
    onDownloadSettingsClick: () -> Unit = {},
    onLibrarySettingsClick: () -> Unit = {},
    onNotificationSettingsClick: () -> Unit = {},
    onConfirmationsSettingsClick: () -> Unit = {},
    onCustomizationPlaygroundClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val themeAccent = LocalThemeAccent.current

    val settings by repository.settings.collectAsState(initial = repository.settingsManager.getSettings())

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showDataManagementDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showResetConfirmationDialog by remember { mutableStateOf(false) }
    var showClearHistoryConfirmation by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    val username = settings.username
    val selectedServer = settings.defaultServer
    val selectedAudio = settings.defaultLanguage
    val autoplayEnabled = settings.autoplay
    val autoNextEnabled = settings.autoNextEpisode
    val autoSkipIntroEnabled = settings.autoSkipIntro
    val seekIntervalSec = settings.seekDurationSec
    val defaultSpeed = settings.defaultPlaybackSpeed
    val selectedThemeColor = settings.themeAccent
    val incognitoMode = settings.incognitoMode
    val amoledMode = settings.amoledMode

    val bannerColor = remember(settings.profileBannerColor) {
        try {
            if (settings.profileBannerColor.isNotBlank()) Color(android.graphics.Color.parseColor(settings.profileBannerColor))
            else null
        } catch (_: Throwable) {
            null
        }
    }

    var importJsonText by remember { mutableStateOf("") }
    var backupStatusMessage by remember { mutableStateOf("") }
    var pendingJsonImportMode by remember { mutableStateOf("MERGE") }
    var showLegacyClipboardSection by remember { mutableStateOf(false) }

    val jsonFileImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val jsonContent = withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                            it.readText()
                        }
                    } catch (_: Throwable) {
                        null
                    }
                }
                if (!jsonContent.isNullOrBlank()) {
                    val res = repository.importBackupJson(jsonContent, mode = pendingJsonImportMode)
                    backupStatusMessage = res
                    Toast.makeText(context, res, Toast.LENGTH_LONG).show()
                } else {
                    backupStatusMessage = "Failed to read selected JSON file."
                    Toast.makeText(context, "Could not read selected JSON file", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val jsonFileExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val json = repository.exportBackupJson()
                val ok = withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(json.toByteArray(Charsets.UTF_8))
                        }
                        true
                    } catch (_: Throwable) {
                        false
                    }
                }
                if (ok) {
                    backupStatusMessage = "Backup JSON saved to selected file!"
                    Toast.makeText(context, "Backup file saved!", Toast.LENGTH_LONG).show()
                } else {
                    backupStatusMessage = "Could not write to selected location."
                }
            }
        }
    }

    val watchHistory by repository.allHistory.collectAsState(initial = emptyList())
    val favorites by repository.favorites.collectAsState(initial = emptyList())
    val downloads by repository.allDownloads.collectAsState(initial = emptyList())

    val providerManager = remember { ProviderManager() }

    fun persistAvatar(uri: android.net.Uri) {
        scope.launch {
            val savedPath = withContext(Dispatchers.IO) {
                try {
                    val previous = repository.settingsManager.getSettings().avatarPath
                    val target = File(context.filesDir, "profile_avatar_" + System.currentTimeMillis() + ".jpg")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        target.outputStream().use { output -> input.copyTo(output) }
                    }
                    if (previous != null && previous != target.absolutePath) {
                        File(previous).takeIf { it.exists() }?.delete()
                    }
                    target.absolutePath
                } catch (_: Throwable) {
                    null
                }
            }
            if (savedPath != null) {
                repository.settingsManager.updateAvatarPath(savedPath)
                Toast.makeText(context, "Profile picture updated", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Could not read that image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun removeAvatar() {
        val previous = repository.settingsManager.getSettings().avatarPath
        scope.launch {
            withContext(Dispatchers.IO) {
                if (previous != null) {
                    File(previous).takeIf { it.exists() }?.delete()
                }
            }
            repository.settingsManager.updateAvatarPath(null)
            Toast.makeText(context, "Profile picture removed", Toast.LENGTH_SHORT).show()
        }
    }

    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            persistAvatar(uri)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
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
                text = "Profile & Settings",
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (bannerColor != null) {
                        Brush.horizontalGradient(
                            listOf(bannerColor.copy(alpha = 0.35f), bannerColor.copy(alpha = 0.08f))
                        )
                    } else {
                        SolidColor(MaterialTheme.colorScheme.surface)
                    }
                )
                .combinedClickable(
                    onClick = { showEditProfileDialog = true },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showEditProfileDialog = true
                    }
                )
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .border(2.5.dp, themeAccent, CircleShape)
                    .padding(3.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = getEffectiveAvatarModel(settings.avatarPath, settings.avatarUrl, settings.avatarSvgContent),
                    contentDescription = "Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = username,
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (incognitoMode) "Incognito Active • Local-First" else settings.profileBio.ifBlank { "Ad-Free • Tracker-Free Streaming" },
                    color = themeAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${watchHistory.size}",
                    color = themeAccent,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Watched",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
            Box(
                modifier = Modifier
                    .height(30.dp)
                    .width(1.dp)
                    .background(DarkBorder)
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${favorites.size}",
                    color = themeAccent,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Favorites",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
            Box(
                modifier = Modifier
                    .height(30.dp)
                    .width(1.dp)
                    .background(DarkBorder)
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${downloads.size}",
                    color = themeAccent,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Offline",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            ProfileMenuItem(
                icon = Icons.Default.Edit,
                title = "Edit Profile",
                onClick = { showEditProfileDialog = true }
            )

            ProfileMenuItem(
                icon = Icons.Default.ColorLens,
                title = "Theme & Appearance ($selectedThemeColor)",
                onClick = { showThemeDialog = true }
            )

            ProfileMenuItem(
                icon = Icons.Default.Tune,
                title = "Customization & UI Playground",
                onClick = onCustomizationPlaygroundClick
            )

            ProfileMenuItem(
                icon = Icons.Default.PlayCircleOutline,
                title = "Player Settings",
                onClick = onPlayerSettingsClick
            )

            ProfileMenuItem(
                icon = Icons.Default.Subtitles,
                title = "Subtitle Settings",
                onClick = onSubtitleSettingsClick
            )

            ProfileMenuItem(
                icon = Icons.Default.Download,
                title = "Download Settings",
                onClick = onDownloadSettingsClick
            )

            ProfileMenuItem(
                icon = Icons.Default.VideoLibrary,
                title = "Library & Series Settings",
                onClick = onLibrarySettingsClick
            )

            ProfileMenuItem(
                icon = Icons.Default.Notifications,
                title = "Notification Settings",
                onClick = onNotificationSettingsClick
            )

            ProfileMenuItem(
                icon = Icons.Default.ReportProblem,
                title = "Confirmations & Warnings",
                onClick = onConfirmationsSettingsClick
            )

            ProfileMenuItem(
                icon = Icons.Default.SyncAlt,
                title = "MAL / AniList Watchlist Sync",
                onClick = onMalSyncClick
            )

            ProfileMenuItem(
                icon = Icons.Default.Security,
                title = "Privacy, Ad-Block & Anti-Tracking",
                onClick = { showPrivacyDialog = true }
            )

            ProfileMenuItem(
                icon = Icons.Default.Storage,
                title = "Data Management (Backup / Import)",
                onClick = { showDataManagementDialog = true },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    scope.launch {
                        val json = repository.exportBackupJson()
                        clipboardManager.setText(AnnotatedString(json))
                        Toast.makeText(context, "Quick Export: Backup JSON copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            ProfileMenuItem(
                icon = Icons.Default.Storage,
                title = "Clear Watch History",
                onClick = {
                    if (settings.confirmDestructiveActions) {
                        showClearHistoryConfirmation = true
                    } else {
                        scope.launch {
                            repository.clearHistory()
                            Toast.makeText(context, "Watch history cleared", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )

            ProfileMenuItem(
                icon = Icons.AutoMirrored.Filled.HelpOutline,
                title = "About Zenith Anime",
                onClick = { showAboutDialog = true }
            )

            ProfileMenuItem(
                icon = Icons.Default.DeleteSweep,
                title = "Reset All Application Data",
                titleColor = AccentRed,
                iconColor = AccentRed,
                onClick = {
                    if (settings.confirmDestructiveActions) {
                        showResetConfirmationDialog = true
                    } else {
                        scope.launch {
                            repository.resetAllData()
                            Toast.makeText(context, "All application data reset", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Clean Anime Streaming Engine • Zero Tracking",
                color = TextSecondary,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Version ${com.example.BuildConfig.VERSION_NAME} • ${providerManager.getAvailableProviders().size} Multi-Servers Active",
                color = Color(0xFF475569),
                fontSize = 11.sp
            )
        }
    }

    if (showThemeDialog) {
        val themes = listOf(
            "Emerald" to Color(0xFF06C149),
            "Cyan" to Color(0xFF00E5FF),
            "Sakura" to Color(0xFFFF4081),
            "Violet" to Color(0xFFB388FF),
            "Amber" to Color(0xFFFFB300),
            "Crimson" to Color(0xFFFF5252),
            "Indigo" to Color(0xFF536DFE),
            "Gold" to Color(0xFFFFD54F)
        )

        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Accent Color Theme", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select your personal accent styling:", color = TextSecondary, fontSize = 12.sp)
                    themes.forEach { (name, color) ->
                        val isSelected = selectedThemeColor == name
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    repository.settingsManager.updateThemeAccent(name)
                                    Toast.makeText(context, "Theme set to $name", Toast.LENGTH_SHORT).show()
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(color, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = name,
                                color = if (isSelected) color else TextPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Text("Active", color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)

                    SettingSwitchRow(
                        title = "AMOLED True Black Mode",
                        checked = amoledMode,
                        onCheckedChange = {
                            repository.settingsManager.updateAmoledMode(it)
                            Toast.makeText(context, if (it) "AMOLED mode enabled" else "AMOLED mode disabled", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showThemeDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = themeAccent, contentColor = MaterialTheme.colorScheme.background)
                ) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Privacy & Anti-Tracking", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0F261B))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text("Ad & Popup Blocker", color = themeAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Malicious popup windows, external scam redirects, and third-party advertising scripts are blocked at the network interceptor level.",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    SettingSwitchRow(
                        title = "Ad & Popup Blocker",
                        checked = settings.adBlockEnabled,
                        onCheckedChange = {
                            repository.settingsManager.update { s -> s.copy(adBlockEnabled = it) }
                            Toast.makeText(context, if (it) "Ad blocker enabled" else "Ad blocker disabled", Toast.LENGTH_SHORT).show()
                        }
                    )

                    SettingSwitchRow(
                        title = "Incognito Mode (Do not save history)",
                        checked = incognitoMode,
                        onCheckedChange = {
                            repository.settingsManager.updateIncognitoMode(it)
                            Toast.makeText(context, if (it) "Incognito mode enabled" else "Incognito mode disabled", Toast.LENGTH_SHORT).show()
                        }
                    )

                    SettingSwitchRow(
                        title = "Hide App from Recent Apps List",
                        checked = settings.hideFromRecents,
                        onCheckedChange = {
                            repository.settingsManager.update { s -> s.copy(hideFromRecents = it) }
                            Toast.makeText(context, if (it) "App will be hidden from recents" else "App visible in recents", Toast.LENGTH_SHORT).show()
                        }
                    )

                    SettingSwitchRow(
                        title = "Block Screenshots & Screen Recording",
                        checked = settings.blockScreenshots,
                        onCheckedChange = {
                            repository.settingsManager.update { s -> s.copy(blockScreenshots = it) }
                            Toast.makeText(context, if (it) "Screenshots blocked (FLAG_SECURE)" else "Screenshots allowed", Toast.LENGTH_SHORT).show()
                        }
                    )

                    HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)

                    Text("Zero Cloud Tracking:", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(
                        "• No account required\n• No telemetry identifiers\n• No location access\n• 100% device-local Room database",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showPrivacyDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = themeAccent, contentColor = MaterialTheme.colorScheme.background)
                ) {
                    Text("Understood", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showEditProfileDialog) {
        var tempName by remember { mutableStateOf(username) }
        var tempBio by remember { mutableStateOf(settings.profileBio) }
        var selectedTab by remember { mutableIntStateOf(0) }
        var inputUrl by remember { mutableStateOf(settings.avatarUrl) }
        var pendingFileUri by remember { mutableStateOf<android.net.Uri?>(null) }
        var pendingFilePath by remember { mutableStateOf<String?>(null) }
        var pendingAvatarUrl by remember { mutableStateOf(settings.avatarUrl) }
        var previewFormatBadge by remember {
            mutableStateOf(
                when {
                    settings.avatarPath?.lowercase()?.endsWith(".gif") == true || settings.avatarUrl.lowercase().endsWith(".gif") -> "GIF"
                    settings.avatarPath?.lowercase()?.endsWith(".svg") == true || settings.avatarUrl.lowercase().endsWith(".svg") -> "SVG"
                    settings.avatarUrl.isNotBlank() -> "URL"
                    settings.avatarPath != null -> "FILE"
                    else -> "DEFAULT"
                }
            )
        }
        var validationError by remember { mutableStateOf<String?>(null) }

        val effectiveCurrentAvatar = remember(settings.avatarPath, settings.avatarUrl, settings.avatarSvgContent) {
            getEffectiveAvatarModel(settings.avatarPath, settings.avatarUrl, settings.avatarSvgContent)
        }

        val previewModel: Any = remember(pendingFileUri, pendingFilePath, pendingAvatarUrl, settings.avatarPath, settings.avatarUrl, settings.avatarSvgContent) {
            when {
                pendingFileUri != null -> pendingFileUri!!
                !pendingFilePath.isNullOrBlank() -> File(pendingFilePath!!)
                pendingAvatarUrl.isNotBlank() -> pendingAvatarUrl
                else -> effectiveCurrentAvatar
            }
        }

        val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: android.net.Uri? ->
            if (uri != null) {
                val mimeType = context.contentResolver.getType(uri).orEmpty().lowercase(java.util.Locale.US)
                val isSvg = mimeType.contains("svg") || uri.toString().lowercase(java.util.Locale.US).contains(".svg")
                val isGif = mimeType.contains("gif") || uri.toString().lowercase(java.util.Locale.US).contains(".gif")

                if (isSvg) {
                    try {
                        val content = context.contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() }.orEmpty()
                        if (!sanitizeSvgText(content)) {
                            validationError = "SVG rejected: Executable script elements are blocked for security."
                            Toast.makeText(context, validationError, Toast.LENGTH_LONG).show()
                            return@rememberLauncherForActivityResult
                        }
                        val target = File(context.filesDir, "avatar_${System.currentTimeMillis()}.svg")
                        target.writeText(content)
                        pendingFilePath = target.absolutePath
                        pendingFileUri = null
                        pendingAvatarUrl = ""
                        previewFormatBadge = "SVG"
                        validationError = null
                    } catch (e: Exception) {
                        validationError = "Failed to load SVG: ${e.message}"
                    }
                } else {
                    try {
                        val ext = when {
                            isGif -> "gif"
                            mimeType.contains("png") -> "png"
                            mimeType.contains("webp") -> "webp"
                            else -> "jpg"
                        }
                        val target = File(context.filesDir, "avatar_${System.currentTimeMillis()}.$ext")
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            target.outputStream().use { output -> input.copyTo(output) }
                        }
                        pendingFilePath = target.absolutePath
                        pendingFileUri = null
                        pendingAvatarUrl = ""
                        previewFormatBadge = if (isGif) "GIF" else ext.uppercase()
                        validationError = null
                    } catch (e: Exception) {
                        validationError = "Failed to load file: ${e.message}"
                    }
                }
            }
        }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text("Customize Profile", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 520.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Live preview card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(88.dp)
                                    .border(2.5.dp, themeAccent, CircleShape)
                                    .padding(3.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.background),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = previewModel,
                                    contentDescription = "Preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                    onError = {
                                        if (pendingAvatarUrl.isNotBlank()) {
                                            validationError = "Unable to preview image from URL (format unsupported or network blocked)"
                                        }
                                    },
                                    onSuccess = {
                                        if (validationError?.startsWith("Unable to preview") == true) {
                                            validationError = null
                                        }
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = tempName.ifBlank { "ZenithWatcher" },
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(themeAccent.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = previewFormatBadge,
                                        color = themeAccent,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            if (tempBio.isNotBlank()) {
                                Text(
                                    text = tempBio,
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    if (validationError != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(AccentRed.copy(alpha = 0.15f))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = AccentRed, modifier = Modifier.size(16.dp))
                            Text(validationError!!, color = AccentRed, fontSize = 11.sp, lineHeight = 14.sp)
                        }
                    }

                    // Profile Info
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text("Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = tempBio,
                        onValueChange = { tempBio = it },
                        label = { Text("Bio / Tagline") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)

                    Text("Profile Avatar Source", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    // Tab Switcher
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val tabs = listOf("Presets", "Web URL", "Device File")
                        tabs.forEachIndexed { index, title ->
                            val isSel = selectedTab == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) themeAccent else Color.Transparent)
                                    .clickable {
                                        selectedTab = index
                                        validationError = null
                                    }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = title,
                                    color = if (isSel) MaterialTheme.colorScheme.background else TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    when (selectedTab) {
                        0 -> {
                            // Presets
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "Choose a curated avatar (PNG, animated GIF, or vector SVG):",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    AVATAR_PRESETS.forEach { preset ->
                                        val isSelected = pendingAvatarUrl == preset.url
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .width(72.dp)
                                                .clickable {
                                                    pendingAvatarUrl = preset.url
                                                    pendingFilePath = null
                                                    pendingFileUri = null
                                                    previewFormatBadge = preset.type
                                                    validationError = null
                                                }
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(56.dp)
                                                    .border(
                                                        width = if (isSelected) 2.5.dp else 1.dp,
                                                        color = if (isSelected) themeAccent else DarkBorder,
                                                        shape = CircleShape
                                                    )
                                                    .padding(2.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.background)
                                            ) {
                                                AsyncImage(
                                                    model = preset.url,
                                                    contentDescription = preset.name,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = preset.name,
                                                color = if (isSelected) themeAccent else TextSecondary,
                                                fontSize = 9.sp,
                                                maxLines = 1,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        1 -> {
                            // Web URL
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "Enter an HTTPS or HTTP direct image link (PNG, JPG, WEBP, GIF, SVG):",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                                OutlinedTextField(
                                    value = inputUrl,
                                    onValueChange = {
                                        inputUrl = it
                                        val (isValid, err) = validateAvatarUrl(it)
                                        if (isValid) {
                                            validationError = null
                                            pendingAvatarUrl = it.trim()
                                            pendingFilePath = null
                                            pendingFileUri = null
                                            previewFormatBadge = when {
                                                it.lowercase().contains(".gif") -> "GIF"
                                                it.lowercase().contains(".svg") -> "SVG"
                                                else -> "URL"
                                            }
                                        } else {
                                            validationError = err
                                        }
                                    },
                                    label = { Text("Image URL") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        2 -> {
                            // Device File
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "Pick any photo, animated GIF, or vector SVG from your device storage:",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                                Button(
                                    onClick = { filePicker.launch("image/*") },
                                    colors = ButtonDefaults.buttonColors(containerColor = themeAccent, contentColor = MaterialTheme.colorScheme.background),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Choose File (GIF / SVG / Image)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                if (pendingFilePath != null) {
                                    Text(
                                        text = "Ready to save: ${File(pendingFilePath!!).name}",
                                        color = themeAccent,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)

                    // Reset option
                    if (settings.avatarPath != null || settings.avatarUrl.isNotBlank() || pendingAvatarUrl.isNotBlank() || pendingFilePath != null) {
                        TextButton(
                            onClick = {
                                pendingAvatarUrl = ""
                                pendingFilePath = null
                                pendingFileUri = null
                                inputUrl = ""
                                previewFormatBadge = "DEFAULT"
                                validationError = null
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = AccentRed),
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text("Reset Avatar to Default", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedTab == 1 && inputUrl.isNotBlank()) {
                            val (isValid, err) = validateAvatarUrl(inputUrl)
                            if (!isValid) {
                                validationError = err
                                return@Button
                            }
                        }

                        if (pendingFilePath != null) {
                            repository.settingsManager.updateAvatar(path = pendingFilePath, url = "")
                        } else if (pendingAvatarUrl.isNotBlank()) {
                            repository.settingsManager.updateAvatar(path = null, url = pendingAvatarUrl)
                        } else if (previewFormatBadge == "DEFAULT") {
                            repository.settingsManager.updateAvatar(path = null, url = "")
                        }

                        repository.settingsManager.updateProfile(
                            name = tempName.trim().ifBlank { "ZenithWatcher" },
                            bio = tempBio.trim()
                        )
                        showEditProfileDialog = false
                        Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeAccent, contentColor = MaterialTheme.colorScheme.background)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    if (showDataManagementDialog) {
        AlertDialog(
            onDismissRequest = { showDataManagementDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Backup & Data Portability", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "Zenith Anime is strictly local-first. Export your library and watch history to a .json file, or upload a .json backup file to restore.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "EXPORT BACKUP FILE",
                        color = themeAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    val json = repository.exportBackupJson()
                                    val res = saveBackupToDeviceStorage(context, json)
                                    if (res != null) {
                                        backupStatusMessage = "Backup file saved ($res)"
                                        Toast.makeText(context, "Saved to Downloads", Toast.LENGTH_LONG).show()
                                    } else {
                                        jsonFileExportLauncher.launch("zenith_anime_backup_${System.currentTimeMillis()}.json")
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeAccent, contentColor = MaterialTheme.colorScheme.background),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Save to Downloads", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                jsonFileExportLauncher.launch("zenith_anime_backup_${System.currentTimeMillis()}.json")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = TextPrimary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Save File As…", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "RESTORE / UPLOAD JSON FILE",
                        color = themeAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Select a .json backup file from your device storage or Downloads folder.",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            pendingJsonImportMode = "MERGE"
                            jsonFileImportLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream", "*/*"))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeAccent, contentColor = MaterialTheme.colorScheme.background),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Upload JSON File (Smart Merge)", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = {
                            pendingJsonImportMode = "REPLACE"
                            jsonFileImportLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream", "*/*"))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF331E24), contentColor = AccentRed),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Upload JSON File (Replace All Data)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showLegacyClipboardSection = !showLegacyClipboardSection }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Legacy: Clipboard Copy & Paste JSON",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (showLegacyClipboardSection) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle legacy clipboard section",
                            tint = TextSecondary
                        )
                    }

                    if (showLegacyClipboardSection) {
                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                scope.launch {
                                    val json = repository.exportBackupJson()
                                    clipboardManager.setText(AnnotatedString(json))
                                    backupStatusMessage = "Backup JSON (${json.length} chars) copied to clipboard"
                                    Toast.makeText(context, "Backup copied to clipboard", Toast.LENGTH_LONG).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = TextPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Backup JSON to Clipboard", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = importJsonText,
                            onValueChange = { importJsonText = it },
                            label = { Text("Paste Backup JSON (Legacy)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            maxLines = 4
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        if (importJsonText.isNotBlank()) {
                                            val res = repository.importBackupJson(importJsonText, mode = "MERGE")
                                            backupStatusMessage = res
                                            Toast.makeText(context, res, Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = themeAccent.copy(alpha = 0.2f), contentColor = themeAccent),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Merge Pasted", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    scope.launch {
                                        if (importJsonText.isNotBlank()) {
                                            val res = repository.importBackupJson(importJsonText, mode = "REPLACE")
                                            backupStatusMessage = res
                                            Toast.makeText(context, res, Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF331E24), contentColor = AccentRed),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Replace Pasted", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }

                    if (backupStatusMessage.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = backupStatusMessage,
                            color = themeAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDataManagementDialog = false }) {
                    Text("Close", color = themeAccent)
                }
            }
        )
    }

    if (showResetConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmationDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Reset Application Data?", color = AccentRed, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "This will erase your local watch history, favorites, saved watchlist, and downloads from this device. This action cannot be undone.",
                    color = TextPrimary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            repository.resetAllData()
                            showResetConfirmationDialog = false
                            Toast.makeText(context, "All application data reset", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed, contentColor = TextPrimary)
                ) {
                    Text("Reset Everything", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmationDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    if (showClearHistoryConfirmation && settings.confirmDestructiveActions) {
        AlertDialog(
            onDismissRequest = { showClearHistoryConfirmation = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Clear Watch History?", color = AccentRed, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "This removes the " + watchHistory.size +
                        " episode" + (if (watchHistory.size == 1) "" else "s") +
                        " in your watch history, so Continue Watching starts over. Episode downloads, favorites and your watchlist are kept. This cannot be undone.",
                    color = TextPrimary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            repository.clearHistory()
                            showClearHistoryConfirmation = false
                            Toast.makeText(context, "Watch history cleared", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed, contentColor = TextPrimary)
                ) {
                    Text("Clear History", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryConfirmation = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    if (showAboutDialog) {
        val serverList = remember {
            providerManager.getAvailableProviders().joinToString(", ") { it.displayName }
        }
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("About Zenith Anime", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Zenith Anime is a privacy-first, local-first anime streaming application.",
                        color = TextPrimary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Version: v${com.example.BuildConfig.VERSION_NAME} (Build ${com.example.BuildConfig.VERSION_CODE})\n• Zero Cloud: No accounts, no telemetry, no tracking servers.\n• Multi-Servers Active (${providerManager.getAvailableProviders().size}): $serverList.\n• Automatic ad-blocking and redirect protection.\n• Full offline library, schedule and backup portability.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("OK", color = themeAccent)
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    titleColor: Color = TextPrimary,
    iconColor: Color = TextSecondary,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "menu_scale"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true),
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(22.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = title,
            color = titleColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.weight(1f))

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = DarkBorder,
            modifier = Modifier.size(20.dp)
        )
    }
    HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
}

@Composable
fun SettingSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val themeAccent = LocalThemeAccent.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, color = TextPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = themeAccent,
                checkedTrackColor = themeAccent.copy(alpha = 0.35f)
            )
        )
    }
}
