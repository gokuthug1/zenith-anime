package com.example.ui.sync

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.UploadFile
import com.example.ui.profile.saveBackupToDeviceStorage
import com.example.ui.theme.AccentRed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import com.example.data.model.Anime
import com.example.data.model.WatchStatus
import com.example.data.repository.AnimeRepository
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.LocalThemeAccent
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun MalSyncScreen(
    repository: AnimeRepository,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val themeAccent = LocalThemeAccent.current
    val settings by repository.settings.collectAsState(initial = repository.settingsManager.getSettings())

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Import", "Export", "Tracker Scrobbler")

    var selectedProvider by remember { mutableStateOf("MyAnimeList") }
    val providers = listOf("MyAnimeList", "AniList")

    var usernameInput by remember(settings) {
        mutableStateOf(if (selectedProvider == "MyAnimeList") settings.malUsername else settings.anilistUsername)
    }

    var anilistTokenInput by remember(settings.anilistAuthToken) {
        mutableStateOf(settings.anilistAuthToken)
    }
    var showAnilistToken by remember { mutableStateOf(false) }

    var malTokenInput by remember(settings.malAuthToken) {
        mutableStateOf(settings.malAuthToken)
    }
    var showMalToken by remember { mutableStateOf(false) }

    var syncWatching by remember { mutableStateOf(true) }
    var syncCompleted by remember { mutableStateOf(true) }
    var syncOnHold by remember { mutableStateOf(true) }
    var syncDropped by remember { mutableStateOf(false) }
    var syncPlanToWatch by remember { mutableStateOf(true) }

    var eraseExisting by remember { mutableStateOf(false) }
    var isSyncing by remember { mutableStateOf(false) }
    var syncResultText by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Top bar
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
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "MAL / AniList Sync",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = themeAccent
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            color = if (selectedTab == index) themeAccent else TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .navigationBarsPadding()
        ) {
            if (selectedTab == 0) {
                // IMPORT TAB
                Text(
                    text = "Select Provider",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    providers.forEach { p ->
                        val isSel = selectedProvider == p
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSel) themeAccent.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface)
                                .border(
                                    width = if (isSel) 1.5.dp else 0.5.dp,
                                    color = if (isSel) themeAccent else DarkBorder,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    selectedProvider = p
                                    usernameInput = if (p == "MyAnimeList") settings.malUsername else settings.anilistUsername
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = p,
                                color = if (isSel) themeAccent else TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "$selectedProvider Username",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = usernameInput,
                    onValueChange = { usernameInput = it },
                    placeholder = { Text("Enter public username...", color = TextSecondary, fontSize = 13.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = themeAccent,
                        unfocusedBorderColor = DarkBorder,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Statuses to Import",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    SyncStatusRow("Watching", syncWatching) { syncWatching = it }
                    HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                    SyncStatusRow("Completed", syncCompleted) { syncCompleted = it }
                    HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                    SyncStatusRow("On-Hold", syncOnHold) { syncOnHold = it }
                    HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                    SyncStatusRow("Plan to Watch", syncPlanToWatch) { syncPlanToWatch = it }
                    HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                    SyncStatusRow("Dropped", syncDropped) { syncDropped = it }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Erase Existing Watchlist", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Wipe local list before merging new items", color = TextSecondary, fontSize = 11.sp)
                    }
                    Switch(
                        checked = eraseExisting,
                        onCheckedChange = { eraseExisting = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.background,
                            checkedTrackColor = themeAccent,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val name = usernameInput.trim()
                        if (name.isEmpty()) {
                            Toast.makeText(context, "Please enter a username", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        if (selectedProvider == "MyAnimeList") {
                            repository.settingsManager.updateMalUsername(name)
                        } else {
                            repository.settingsManager.updateAnilistUsername(name)
                        }

                        isSyncing = true
                        syncResultText = null
                        scope.launch {
                            val res: Result<List<Anime>> = if (selectedProvider == "MyAnimeList") {
                                repository.malSyncService.importFromMyAnimeList(name)
                            } else {
                                repository.malSyncService.importFromAniList(name)
                            }

                            res.onSuccess { rawList: List<Anime> ->
                                val filtered = rawList.filter { item: Anime ->
                                    when (item.watchStatus) {
                                        WatchStatus.WATCHING.name -> syncWatching
                                        WatchStatus.COMPLETED.name -> syncCompleted
                                        WatchStatus.ON_HOLD.name -> syncOnHold
                                        WatchStatus.DROPPED.name -> syncDropped
                                        WatchStatus.PLAN_TO_WATCH.name -> syncPlanToWatch
                                        else -> true
                                    }
                                }

                                val imported = repository.importWatchlist(filtered, eraseExisting)
                                syncResultText = "Successfully imported $imported anime from $selectedProvider!"
                                Toast.makeText(context, syncResultText, Toast.LENGTH_LONG).show()
                            }.onFailure { err ->
                                syncResultText = "Import error: ${err.localizedMessage ?: "Unknown network failure"}"
                                Toast.makeText(context, syncResultText, Toast.LENGTH_LONG).show()
                            }
                            isSyncing = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeAccent, contentColor = MaterialTheme.colorScheme.background),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isSyncing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.background, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Fetching Watchlist...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Import from $selectedProvider", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                if (syncResultText != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(themeAccent.copy(alpha = 0.15f))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = syncResultText!!,
                            color = themeAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else if (selectedTab == 1) {
                var backupStatusMsg by remember { mutableStateOf("") }
                var legacyJsonInput by remember { mutableStateOf("") }
                var showLegacyClipboard by remember { mutableStateOf(false) }
                var pendingFileImportMode by remember { mutableStateOf("MERGE") }

                val jsonImportLauncher = rememberLauncherForActivityResult(
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
                                val res = repository.importBackupJson(jsonContent, mode = pendingFileImportMode)
                                backupStatusMsg = res
                                Toast.makeText(context, res, Toast.LENGTH_LONG).show()
                            } else {
                                backupStatusMsg = "Could not read selected JSON file."
                                Toast.makeText(context, "Failed to read JSON file", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                // EXPORT & BACKUP TAB
                Text(
                    text = "JSON File Backup & Restore",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Export your local watchlist, watch history, and progress into a .json backup file, or upload an existing .json file to restore.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        scope.launch {
                            val json = repository.exportBackupJson()
                            val saved = saveBackupToDeviceStorage(context, json)
                            if (saved != null) {
                                backupStatusMsg = "Backup saved ($saved)"
                                Toast.makeText(context, saved, Toast.LENGTH_LONG).show()
                            } else {
                                clipboardManager.setText(AnnotatedString(json))
                                backupStatusMsg = "Copied backup JSON to clipboard"
                                Toast.makeText(context, "Copied backup JSON to clipboard", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeAccent, contentColor = MaterialTheme.colorScheme.background),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Backup JSON File to Downloads", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Upload JSON Backup File",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            pendingFileImportMode = "MERGE"
                            jsonImportLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream", "*/*"))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeAccent, contentColor = MaterialTheme.colorScheme.background),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Upload (Merge)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            pendingFileImportMode = "REPLACE"
                            jsonImportLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream", "*/*"))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF331E24), contentColor = AccentRed),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Upload (Replace)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                if (backupStatusMsg.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = backupStatusMsg,
                        color = themeAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showLegacyClipboard = !showLegacyClipboard }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Legacy: Clipboard Copy & Paste JSON",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (showLegacyClipboard) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = TextSecondary
                    )
                }

                if (showLegacyClipboard) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                val json = repository.exportBackupJson()
                                clipboardManager.setText(AnnotatedString(json))
                                Toast.makeText(context, "Full Watchlist JSON copied to clipboard!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = TextPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Copy Export JSON to Clipboard (Legacy)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = legacyJsonInput,
                        onValueChange = { legacyJsonInput = it },
                        label = { Text("Paste Backup JSON (Legacy)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        maxLines = 4
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (legacyJsonInput.isNotBlank()) {
                                scope.launch {
                                    val res = repository.importBackupJson(legacyJsonInput, mode = "MERGE")
                                    backupStatusMsg = res
                                    Toast.makeText(context, res, Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeAccent.copy(alpha = 0.2f), contentColor = themeAccent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Import Pasted JSON (Legacy)", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // TRACKER SCROBBLER TAB
                Text(
                    text = "2-Way Tracker Auto-Scrobbler & Auth Tokens",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Automatically sync and update your watched episode progress to your AniList and MyAnimeList accounts as you watch.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Enable 2-Way Tracker Sync", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Synchronize watching progress with online lists", color = TextSecondary, fontSize = 11.sp)
                        }
                        Switch(
                            checked = settings.trackerSyncEnabled,
                            onCheckedChange = { repository.settingsManager.update { s -> s.copy(trackerSyncEnabled = it) } },
                            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.background, checkedTrackColor = themeAccent)
                        )
                    }

                    HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto-Scrobble Watched Episodes", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Mark episodes watched when reaching ${settings.scrobbleCompletionPercent}% threshold", color = TextSecondary, fontSize = 11.sp)
                        }
                        Switch(
                            checked = settings.autoScrobbleEnabled,
                            onCheckedChange = { repository.settingsManager.update { s -> s.copy(autoScrobbleEnabled = it) } },
                            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.background, checkedTrackColor = themeAccent)
                        )
                    }

                    HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto-Sync on Launch", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Check for online list changes when the app opens", color = TextSecondary, fontSize = 11.sp)
                        }
                        Switch(
                            checked = settings.autoSyncOnOpen,
                            onCheckedChange = { repository.settingsManager.update { s -> s.copy(autoSyncOnOpen = it) } },
                            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.background, checkedTrackColor = themeAccent)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text("AniList Personal Access Token", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Generate from AniList Settings > Developer > Create New Client", color = TextSecondary, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = anilistTokenInput,
                        onValueChange = { anilistTokenInput = it },
                        placeholder = { Text("Paste AniList Bearer token...", color = TextSecondary, fontSize = 12.sp) },
                        singleLine = true,
                        visualTransformation = if (showAnilistToken) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showAnilistToken = !showAnilistToken }) {
                                Icon(
                                    imageVector = if (showAnilistToken) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showAnilistToken) "Hide token" else "Show token",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = themeAccent,
                            unfocusedBorderColor = DarkBorder,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            repository.settingsManager.update { it.copy(anilistAuthToken = anilistTokenInput.trim()) }
                            Toast.makeText(context, "AniList token saved!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeAccent, contentColor = MaterialTheme.colorScheme.background),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("MyAnimeList OAuth Access Token", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Access token for MAL v2 API scrobbling", color = TextSecondary, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = malTokenInput,
                        onValueChange = { malTokenInput = it },
                        placeholder = { Text("Paste MAL API token...", color = TextSecondary, fontSize = 12.sp) },
                        singleLine = true,
                        visualTransformation = if (showMalToken) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showMalToken = !showMalToken }) {
                                Icon(
                                    imageVector = if (showMalToken) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showMalToken) "Hide token" else "Show token",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = themeAccent,
                            unfocusedBorderColor = DarkBorder,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            repository.settingsManager.update { it.copy(malAuthToken = malTokenInput.trim()) }
                            Toast.makeText(context, "MAL token saved!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeAccent, contentColor = MaterialTheme.colorScheme.background),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncStatusRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val themeAccent = LocalThemeAccent.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = themeAccent,
                checkmarkColor = MaterialTheme.colorScheme.background,
                uncheckedColor = TextSecondary
            )
        )
    }
}
