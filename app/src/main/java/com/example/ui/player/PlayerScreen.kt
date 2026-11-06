package com.example.ui.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.util.Log
import android.util.Rational
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.Toast
import com.example.data.util.ShareUtils
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.ripple
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Warning
import com.example.data.model.WatchStatus
import com.example.ui.theme.AccentRed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.data.model.Anime
import com.example.data.model.Episode
import com.example.data.provider.ProviderManager
import com.example.data.repository.AnimeRepository
import com.example.data.repository.DownloadStart
import com.example.data.resolver.AudioTrackOption
import com.example.data.resolver.ResolveResult
import com.example.data.resolver.StreamResolverRegistry
import com.example.data.resolver.StreamSource
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.LocalThemeAccent
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.getThemeAccentColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(UnstableApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    animeId: String,
    initialEpisodeNumber: Int,
    initialLanguage: String,
    repository: AnimeRepository,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onAnimeDetailsClick: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isInPip = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity?.isInPictureInPictureMode == true
    val themeAccent = LocalThemeAccent.current
    val view = LocalView.current

    val savedSettings by repository.settings.collectAsState(initial = repository.settingsManager.getSettings())
    val anime by repository.getAnimeById(animeId).collectAsState(initial = null)
    val episodes by repository.getEpisodes(animeId).collectAsState(initial = emptyList())
    val allProgress by repository.allHistory.collectAsState(initial = emptyList())
    val allDownloads by repository.allDownloads.collectAsState(initial = emptyList())

    var currentEpNumber by remember { mutableIntStateOf(initialEpisodeNumber) }
    var currentLanguage by remember { mutableStateOf(initialLanguage.ifEmpty { savedSettings.defaultLanguage }) }
    var currentServer by remember { mutableStateOf(savedSettings.defaultServer) }

    val providerManager = remember { ProviderManager() }
    // Shared with the repository so downloads and playback reuse one resolver set.
    val registry = repository.resolverRegistry

    // ---- playback state ----
    var resolveResult by remember { mutableStateOf<ResolveResult?>(null) }
    var sources by remember { mutableStateOf<List<StreamSource>>(emptyList()) }
    var sourceIndex by remember { mutableIntStateOf(0) }
    var isResolving by remember { mutableStateOf(false) }
    var usingEmbedFallback by remember { mutableStateOf(false) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var exoAudioTracks by remember { mutableStateOf<List<AudioTrackOption>>(emptyList()) }
    var activeExoAudioTrackId by remember { mutableStateOf<String?>(null) }
    var userPickedExoAudioTrack by remember { mutableStateOf(false) }
    var dubStatusBanner by remember { mutableStateOf<String?>(null) }

    var isPlaying by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }
    var isReady by remember { mutableStateOf(false) }
    var currentTimeSec by remember { mutableLongStateOf(0L) }
    var durationSec by remember { mutableLongStateOf(0L) }
    var bufferedTimeSec by remember { mutableLongStateOf(0L) }
    var lastSeekTimestampMs by remember { mutableLongStateOf(0L) }
    var showRemainingTime by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var videoVolume by remember { mutableFloatStateOf(1f) }
    var playbackSpeed by remember { mutableFloatStateOf(savedSettings.defaultPlaybackSpeed) }
    var brightness by remember {
        mutableFloatStateOf(
            activity?.window?.attributes?.screenBrightness?.takeIf { it in 0.05f..1.0f } ?: 0.75f
        )
    }

    // ---- ui state ----
    var showControls by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }
    var manualFullscreen by remember { mutableStateOf(false) }
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPosition by remember { mutableLongStateOf(0L) }
    var isHoldingSpeed by remember { mutableStateOf(false) }
    // The multiplier the active hold is applying, so the badge can label fast and slow holds.
    var holdSpeedTarget by remember { mutableFloatStateOf(1f) }
    var seekFeedbackText by remember { mutableStateOf<String?>(null) }
    var gestureFeedback by remember { mutableStateOf<String?>(null) }
    var doubleTapSeekSide by remember { mutableStateOf<String?>(null) }
    var doubleTapSeekAccumulated by remember { mutableIntStateOf(0) }
    var doubleTapDismissJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var gestureHudType by remember { mutableStateOf<String?>(null) }
    var gestureHudLevel by remember { mutableFloatStateOf(1f) }
    var showServerSheet by remember { mutableStateOf(false) }
    var showSourceSheet by remember { mutableStateOf(false) }
    var showEpisodeSheet by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showAudioDialog by remember { mutableStateOf(false) }
    var showFitDialog by remember { mutableStateOf(false) }
    var lockPillVisible by remember { mutableStateOf(false) }
    var showDiagnostics by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var sleepTimerRemainingMin by remember { mutableIntStateOf(0) }
    var showCastDialog by remember { mutableStateOf(false) }
    var isCasting by remember { mutableStateOf(false) }
    var castingDeviceName by remember { mutableStateOf<String?>(null) }
    var showAutoNextBanner by remember { mutableStateOf(false) }
    var autoNextCountdown by remember { mutableIntStateOf(savedSettings.autoNextCountdownSec.coerceIn(2, 15)) }
    var autoNextCancelled by remember(currentEpNumber) { mutableStateOf(false) }
    var introSkippedForEp by remember(currentEpNumber) { mutableStateOf(false) }
    var outroSkippedForEp by remember(currentEpNumber) { mutableStateOf(false) }
    var episodeQuery by remember { mutableStateOf("") }
    var selectedEpisodeRangeIndex by remember(episodeQuery) { mutableIntStateOf(0) }
    var resumePositionSec by remember { mutableLongStateOf(0L) }
    var hasAppliedResume by remember(animeId, currentEpNumber) { mutableStateOf(false) }
    // A finished offline download is played straight from disk, so downloads work with no network.
    var localPath by remember { mutableStateOf<String?>(null) }
    var localSubtitlePath by remember { mutableStateOf<String?>(null) }
    // Deferred intents: effects and the player listener set these, and they are applied after the
    // helper functions below are declared (Kotlin resolves local functions lexically).
    var pendingAdvance by remember { mutableStateOf(false) }
    var pendingEpisode by remember { mutableStateOf<Int?>(null) }
    var pendingLock by remember { mutableStateOf(false) }
    var resolveNonce by remember { mutableIntStateOf(0) }

    val embedState = remember { mutableStateOf(EmbedPlayerState()) }
    val embedController = rememberEmbedPlayerController()

    val currentEpisode = episodes.firstOrNull { it.number == currentEpNumber } ?: Episode(
        id = "${animeId}_$currentEpNumber",
        animeId = animeId,
        number = currentEpNumber,
        title = "Episode $currentEpNumber",
        thumbnail = ""
    )

    val currentSource: StreamSource? = sources.getOrNull(sourceIndex)
    val effectiveMultiplier = savedSettings.holdToSpeedMultiplier.coerceIn(1.25f, 4f)
    val effectiveSlowMultiplier = savedSettings.holdToSpeedSlowMultiplier.coerceIn(0.25f, 1f)

    LaunchedEffect(currentEpNumber, currentSource?.key) {
        userPickedExoAudioTrack = false
    }

    // ---------------------------------------------------------------------------------------------
    // Player instance
    // ---------------------------------------------------------------------------------------------
    // Stream CDNs reject requests that arrive without the headers the resolver used, so the
    // selected source's headers are injected into every request as it is made rather than being
    // fixed when the player is built.
    val playbackHeaders = remember { mutableMapOf<String, String>() }

    val player = remember {
        val baseFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(
                "Mozilla/5.0 (Linux; Android 15; SM-S166V) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
            )
            .setAllowCrossProtocolRedirects(true)

        val httpFactory = ResolvingDataSource.Factory(baseFactory) { dataSpec: DataSpec ->
            if (playbackHeaders.isEmpty()) dataSpec else dataSpec.withRequestHeaders(playbackHeaders)
        }

        // Requests must be dispatched by scheme: a downloaded episode is a `file://` URI and an
        // HTTP-only source cannot open it, while streams need the headers above.
        val dataSourceFactory = DefaultDataSource.Factory(context, httpFactory)

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
            .apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                        .build(),
                    true
                )
                setHandleAudioBecomingNoisy(true)
            }
    }

    var audioSessionId by remember { mutableIntStateOf(player.audioSessionId) }

    val mediaSession = remember {
        MediaSession.Builder(context, player)
            .setId("zenith_player_${java.util.UUID.randomUUID()}")
            .build()
    }

    val currentEpisodeState by rememberUpdatedState(currentEpisode)
    val currentAnimeState by rememberUpdatedState(anime)
    val userPickedExoAudioTrackState by rememberUpdatedState(userPickedExoAudioTrack)

    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onAudioSessionIdChanged(newAudioSessionId: Int) {
                audioSessionId = newAudioSessionId
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                if (!playing && currentTimeSec > 0) {
                    currentAnimeState?.let { loaded ->
                        val displayAnime = loaded.copy(title = loaded.displayTitle(savedSettings.titleLanguagePreference))
                        val comp = durationSec > 0 && currentTimeSec >= durationSec * savedSettings.markWatchedThresholdPercent / 100
                        scope.launch {
                            repository.saveWatchProgress(
                                anime = displayAnime,
                                episode = currentEpisodeState,
                                currentTimeSec = currentTimeSec,
                                durationSec = if (durationSec > 0) durationSec else 1440L,
                                language = currentLanguage,
                                completed = comp
                            )
                        }
                    }
                }
            }

            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING
                isReady = state == Player.STATE_READY || state == Player.STATE_ENDED
            }

            override fun onPlayerError(error: PlaybackException) {
                playbackError = error.errorCodeName
                pendingAdvance = true
            }

            override fun onTracksChanged(tracks: Tracks) {
                val discovered = mutableListOf<AudioTrackOption>()
                tracks.groups.forEachIndexed { groupIdx, group ->
                    if (group.type == C.TRACK_TYPE_AUDIO) {
                        for (trackIdx in 0 until group.length) {
                            val format = group.getTrackFormat(trackIdx)
                            val lang = format.language?.ifBlank { "und" } ?: "und"
                            val label = format.label?.takeIf { it.isNotBlank() }
                                ?: when {
                                    lang.startsWith("en", true) -> "English Dub ($lang)"
                                    lang.startsWith("ja", true) || lang.startsWith("jp", true) -> "Japanese Original ($lang)"
                                    else -> "Audio Track ${discovered.size + 1} ($lang)"
                                }
                            val combined = "$label $lang".lowercase()
                            val isDub = combined.contains("eng") || combined.contains("dub") || lang.startsWith("en", true)
                            val isSelected = group.isTrackSelected(trackIdx)
                            val optionId = "exo_${groupIdx}_$trackIdx"
                            if (isSelected) {
                                activeExoAudioTrackId = optionId
                            }
                            discovered += AudioTrackOption(
                                id = optionId,
                                label = label,
                                language = lang,
                                isDub = isDub,
                                isDefault = isSelected,
                                groupIndex = groupIdx,
                                trackIndex = trackIdx
                            )
                        }
                    }
                }
                exoAudioTracks = discovered

                // If the stream exposes multiple audio tracks inside the manifest, automatically
                // select the track matching currentLanguage ("dub" -> English, "sub" -> Japanese)
                // unless the user manually picked a specific ExoPlayer audio track.
                if (discovered.isNotEmpty() && !userPickedExoAudioTrackState) {
                    val wantDub = currentLanguage.equals("dub", ignoreCase = true)
                    val matchingTrack = discovered.firstOrNull { it.isDub == wantDub }
                        ?: if (!wantDub) discovered.firstOrNull() else null
                    if (matchingTrack != null && matchingTrack.id != activeExoAudioTrackId) {
                        val targetGroup = tracks.groups.getOrNull(matchingTrack.groupIndex)
                        if (targetGroup != null) {
                            player.trackSelectionParameters = player.trackSelectionParameters
                                .buildUpon()
                                .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                                .setOverrideForType(
                                    TrackSelectionOverride(targetGroup.mediaTrackGroup, matchingTrack.trackIndex)
                                )
                                .setPreferredAudioLanguage(if (wantDub) "en" else "ja")
                                .build()
                            activeExoAudioTrackId = matchingTrack.id
                        }
                    }
                }
            }

            override fun onEvents(playerRef: Player, events: Player.Events) {
                durationSec = (playerRef.duration / 1000L).coerceAtLeast(0L)
                bufferedTimeSec = (playerRef.bufferedPosition / 1000L).coerceAtLeast(0L)
                if (!isScrubbing && (System.currentTimeMillis() - lastSeekTimestampMs > 850L)) {
                    currentTimeSec = (playerRef.currentPosition / 1000L).coerceAtLeast(0L)
                }
                videoVolume = playerRef.volume
                isMuted = playerRef.volume <= 0f
            }
        }
        player.addListener(listener)
        onDispose {
            currentAnimeState?.let { loaded ->
                val displayAnime = loaded.copy(title = loaded.displayTitle(savedSettings.titleLanguagePreference))
                val cur = if (currentTimeSec > 0) currentTimeSec else (player.currentPosition / 1000L).coerceAtLeast(0L)
                val dur = if (durationSec > 0) durationSec else (player.duration / 1000L).coerceAtLeast(0L)
                val comp = dur > 0 && cur >= dur * savedSettings.markWatchedThresholdPercent / 100
                CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    repository.saveWatchProgress(
                        anime = displayAnime,
                        episode = currentEpisodeState,
                        currentTimeSec = cur,
                        durationSec = if (dur > 0) dur else 1440L,
                        language = currentLanguage,
                        completed = comp
                    )
                }
            }
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            val lp = activity?.window?.attributes
            if (lp != null) {
                lp.screenBrightness = android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                activity.window.attributes = lp
            }
            player.removeListener(listener)
            mediaSession.release()
            player.release()
        }
    }

    // Lifecycle observer: pause playback on ON_STOP when background playback & audio-only background are disabled (and not in PiP).
    DisposableEffect(lifecycleOwner, savedSettings.backgroundPlayback, savedSettings.audioOnlyBackgroundEnabled) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                val inPipNow = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity?.isInPictureInPictureMode == true
                if (!inPipNow && !savedSettings.backgroundPlayback && !savedSettings.audioOnlyBackgroundEnabled) {
                    player.pause()
                    embedController.send("pause")
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Loudness Normalization via Android LoudnessEnhancer attached to ExoPlayer's audioSessionId.
    DisposableEffect(audioSessionId, savedSettings.loudnessNormalization) {
        var enhancer: android.media.audiofx.LoudnessEnhancer? = null
        if (audioSessionId != C.AUDIO_SESSION_ID_UNSET && audioSessionId != 0 && savedSettings.loudnessNormalization) {
            try {
                enhancer = android.media.audiofx.LoudnessEnhancer(audioSessionId).apply {
                    setTargetGain(600)
                    enabled = true
                }
            } catch (_: Throwable) {
            }
        }
        onDispose {
            try {
                enhancer?.enabled = false
                enhancer?.release()
            } catch (_: Throwable) {
            }
        }
    }

    // Keep screen on while playing (user preference).
    DisposableEffect(savedSettings.keepScreenOn, isPlaying) {
        if (savedSettings.keepScreenOn) view.keepScreenOn = isPlaying
        else view.keepScreenOn = false
        onDispose { view.keepScreenOn = false }
    }

    // Apply orientationLockMode when not locked or manually fullscreened.
    LaunchedEffect(savedSettings.orientationLockMode) {
        if (!isLocked) {
            when (savedSettings.orientationLockMode) {
                "Landscape" -> activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                "Portrait" -> if (!manualFullscreen) activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                "Auto" -> if (!manualFullscreen) activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }

    LaunchedEffect(isInPip) {
        if (isInPip) {
            showControls = false
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Resolve a real stream for the current episode
    // ---------------------------------------------------------------------------------------------
    LaunchedEffect(animeId, currentEpNumber) {
        localPath = repository.localPlaybackPath(animeId, currentEpNumber)
        localSubtitlePath = repository.localSubtitlePath(animeId, currentEpNumber)
    }

    // `anime` is a key so the resolve retries once the Room row arrives instead of giving up.
    LaunchedEffect(animeId, anime?.id, currentEpNumber, currentLanguage, currentServer, resolveNonce) {
        val loaded = anime ?: return@LaunchedEffect
        val diskPath = localPath ?: repository.localPlaybackPath(animeId, currentEpNumber)
        if (localPath == null && diskPath != null) {
            localPath = diskPath
            localSubtitlePath = repository.localSubtitlePath(animeId, currentEpNumber)
        }
        val hasLocalBefore = !diskPath.isNullOrBlank() && java.io.File(diskPath).exists()
        if (hasLocalBefore) {
            isResolving = false
            playbackError = null
            usingEmbedFallback = false
            return@LaunchedEffect
        }

        isResolving = true
        playbackError = null
        dubStatusBanner = null
        sources = emptyList()
        sourceIndex = 0
        exoAudioTracks = emptyList()
        usingEmbedFallback = registry.isExplicitEmbedServer(currentServer)
        resolveResult = null

        if (!usingEmbedFallback) {
            val result = registry.resolve(
                anime = loaded,
                episode = currentEpisode,
                language = currentLanguage,
                preferredServerId = currentServer,
                autoFailover = savedSettings.autoFailover,
                maxAttempts = savedSettings.failoverServerCount.coerceAtLeast(1)
            )
            resolveResult = result
            sources = result.sources
            val hasOfflineNow = !localPath.isNullOrBlank() && java.io.File(localPath!!).exists()
            if (!result.hasAnySource && !hasOfflineNow) {
                if (result.isDubUnavailable) {
                    dubStatusBanner = "English Dub is not available on direct servers for Episode $currentEpNumber — trying dub-capable embed or switch to SUB."
                    Toast.makeText(
                        context,
                        "Dub not found on direct server — trying dub embed fallback",
                        Toast.LENGTH_SHORT
                    ).show()
                    usingEmbedFallback = true
                } else {
                    playbackError = result.failureSummary()
                    usingEmbedFallback = true
                }
            }
        }
        isResolving = false
    }

    // Load the selected source into the player.
    LaunchedEffect(currentSource?.key, currentEpisode.id, localPath, localSubtitlePath, usingEmbedFallback) {
        if (usingEmbedFallback) {
            player.pause()
            player.stop()
            return@LaunchedEffect
        }
        val source = currentSource
        val validLocalPath = localPath?.takeIf { it.isNotBlank() && java.io.File(it).exists() }
        if (source == null && validLocalPath == null) return@LaunchedEffect
        playbackError = null
        isBuffering = true
        isReady = false
        currentTimeSec = 0L

        playbackHeaders.clear()
        playbackHeaders.putAll(source?.headers.orEmpty())
        Log.i(
            PLAYER_TAG,
            "loading ep ${currentEpisode.number}: local=${validLocalPath ?: "-"} source=${source?.url ?: "-"}"
        )

        val preferredAnimeTitle = anime?.displayTitle(savedSettings.titleLanguagePreference).orEmpty()
        val mediaItem = if (validLocalPath != null) {
            val builder = MediaItem.Builder()
                .setUri("file://$validLocalPath")
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("Episode ${currentEpisode.number}")
                        .setArtist(preferredAnimeTitle)
                        .setDescription("Offline")
                        .build()
                )
            // Downloads save the subtitle sidecar next to the video; attaching it here is what makes
            // an offline episode keep its captions. Whether they are shown is still the normal policy.
            localSubtitlePath?.let { sidecar ->
                builder.setSubtitleConfigurations(
                    listOf(
                        MediaItem.SubtitleConfiguration.Builder(Uri.parse("file://$sidecar"))
                            .setMimeType("text/vtt")
                            .setLanguage(savedSettings.subtitleLanguage.lowercase().take(3))
                            .setLabel(savedSettings.subtitleLanguage)
                            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                            .build()
                    )
                )
            }
            builder.build()
        } else {
            buildMediaItem(source!!, preferredAnimeTitle, currentEpisode)
        }
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = savedSettings.autoplay
        player.setPlaybackSpeed(playbackSpeed)
        applyResizeMode(player, savedSettings.videoFitMode)
    }

    // ---------------------------------------------------------------------------------------------
    // Progress persistence and auto-next
    // ---------------------------------------------------------------------------------------------
    LaunchedEffect(currentEpNumber, animeId, anime?.id) {
        val loaded = anime ?: return@LaunchedEffect
        val displayAnime = loaded.copy(title = loaded.displayTitle(savedSettings.titleLanguagePreference))
        val saved = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            repository.getProgressForEpisode(animeId, currentEpNumber)
        }
        if (saved == null) {
            resumePositionSec = 0L
            repository.saveWatchProgress(
                anime = displayAnime,
                episode = currentEpisode,
                currentTimeSec = 0L,
                durationSec = 1440L,
                language = currentLanguage,
                completed = false
            )
        } else if (!saved.completed && saved.currentTimeSec > savedSettings.resumeThresholdSec) {
            resumePositionSec = saved.currentTimeSec * 1000L
            if (savedSettings.resumePromptEnabled) {
                Toast.makeText(
                    context,
                    "Resuming episode $currentEpNumber from ${saved.formattedCurrentTime()}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            resumePositionSec = 0L
        }
    }

    // Apply resume position reliably once both the player is ready and resumePositionSec has loaded from Room.
    LaunchedEffect(isReady, resumePositionSec, currentEpNumber, usingEmbedFallback) {
        if (!hasAppliedResume && isReady && resumePositionSec > savedSettings.resumeThresholdSec * 1000L) {
            hasAppliedResume = true
            val targetSec = resumePositionSec / 1000L
            currentTimeSec = targetSec
            if (usingEmbedFallback) {
                embedController.send("seek", resumePositionSec / 1000.0)
            } else {
                player.seekTo(resumePositionSec)
            }
            resumePositionSec = 0L
        }
    }

    // Continuous ticker for smooth, jump-free playback progress and UI updates
    LaunchedEffect(isPlaying, isReady, usingEmbedFallback) {
        if (!usingEmbedFallback && isReady) {
            while (true) {
                bufferedTimeSec = (player.bufferedPosition / 1000L).coerceAtLeast(0L)
                if (isPlaying && !isScrubbing && (System.currentTimeMillis() - lastSeekTimestampMs > 850L)) {
                    val pos = (player.currentPosition / 1000L).coerceAtLeast(0L)
                    val dur = (player.duration / 1000L).coerceAtLeast(0L)
                    if (dur > 0 && dur != durationSec) {
                        durationSec = dur
                    }
                    if (pos != currentTimeSec) {
                        currentTimeSec = pos
                    }
                }
                delay(300L)
            }
        }
    }

    LaunchedEffect(currentTimeSec, durationSec, isReady, savedSettings.saveProgressEverySecond, savedSettings.saveProgressIntervalSec) {
        val loaded = anime ?: return@LaunchedEffect
        if (!isReady || durationSec <= 0) return@LaunchedEffect
        if (!hasAppliedResume && resumePositionSec > 0L) return@LaunchedEffect
        val interval = if (savedSettings.saveProgressEverySecond) 1L else savedSettings.saveProgressIntervalSec.coerceIn(1, 60).toLong()
        if (interval > 1L && currentTimeSec % interval != 0L && currentTimeSec < durationSec - 5) return@LaunchedEffect
        val completeThreshold = durationSec * savedSettings.markWatchedThresholdPercent / 100
        val displayAnime = loaded.copy(title = loaded.displayTitle(savedSettings.titleLanguagePreference))
        repository.saveWatchProgress(
            anime = displayAnime,
            episode = currentEpisode,
            currentTimeSec = currentTimeSec,
            durationSec = durationSec,
            language = currentLanguage,
            completed = currentTimeSec >= completeThreshold
        )
    }

    // Auto-next episode countdown banner
    LaunchedEffect(
        currentTimeSec,
        durationSec,
        isReady,
        autoNextCancelled,
        savedSettings.autoNextEpisode,
        savedSettings.autoNextCountdownSec
    ) {
        val countdownTarget = savedSettings.autoNextCountdownSec.coerceIn(2, 15)
        if (!isReady || durationSec <= 0 || showAutoNextBanner || autoNextCancelled || !savedSettings.autoNextEpisode) {
            return@LaunchedEffect
        }
        if (currentTimeSec <= 5 || durationSec - currentTimeSec > countdownTarget + 5) return@LaunchedEffect
        if (episodes.any { it.number == currentEpNumber + 1 }) {
            showAutoNextBanner = true
            autoNextCountdown = countdownTarget
        }
    }

    LaunchedEffect(showAutoNextBanner, autoNextCountdown) {
        if (showAutoNextBanner && autoNextCountdown > 0) {
            delay(1000L)
            autoNextCountdown -= 1
            if (autoNextCountdown == 0) {
                showAutoNextBanner = false
                pendingEpisode = currentEpNumber + 1
            }
        }
    }

    // Auto-skip OP / ED when enabled in settings
    LaunchedEffect(
        currentTimeSec,
        durationSec,
        isReady,
        savedSettings.autoSkipIntro,
        savedSettings.autoSkipOutro,
        savedSettings.introSkipSeconds,
        savedSettings.outroSkipSeconds,
        introSkippedForEp,
        outroSkippedForEp
    ) {
        if (!isReady) return@LaunchedEffect
        val introSkip = savedSettings.introSkipSeconds.coerceIn(5, 300).toLong()
        val outroSkip = savedSettings.outroSkipSeconds.coerceIn(5, 300).toLong()
        val chapterIntroStart = embedState.value.introStartSec
        val chapterIntroEnd = embedState.value.introEndSec
        val hasChapterIntro = usingEmbedFallback && chapterIntroEnd > chapterIntroStart && chapterIntroStart >= 0L
        val inIntroWindow = if (hasChapterIntro) {
            currentTimeSec in chapterIntroStart..(chapterIntroEnd - 2L).coerceAtLeast(chapterIntroStart)
        } else {
            currentTimeSec in 2..30L && (durationSec <= 0L || durationSec > introSkip + 30L)
        }
        if (savedSettings.autoSkipIntro && !introSkippedForEp && inIntroWindow) {
            introSkippedForEp = true
            val target = if (hasChapterIntro) {
                chapterIntroEnd
            } else {
                (currentTimeSec + introSkip).let { if (durationSec > 0) it.coerceAtMost(durationSec) else it }
            }
            currentTimeSec = target
            if (usingEmbedFallback) embedController.send("seek", target)
            else player.seekTo(target * 1000L)
            Toast.makeText(context, "Auto-skipped OP", Toast.LENGTH_SHORT).show()
        }
        val chapterOutroStart = embedState.value.outroStartSec
        val chapterOutroEnd = embedState.value.outroEndSec
        val hasChapterOutro = usingEmbedFallback && chapterOutroEnd > chapterOutroStart && chapterOutroStart >= 0L
        val inOutroWindow = if (hasChapterOutro) {
            currentTimeSec in chapterOutroStart..(chapterOutroEnd - 2L).coerceAtLeast(chapterOutroStart)
        } else {
            durationSec > outroSkip + 30L && currentTimeSec in (durationSec - outroSkip)..(durationSec - 8L)
        }
        if (savedSettings.autoSkipOutro && !outroSkippedForEp && inOutroWindow) {
            outroSkippedForEp = true
            val target = if (hasChapterOutro) {
                chapterOutroEnd
            } else {
                minOf(currentTimeSec + outroSkip, (durationSec - 5L).coerceAtLeast(currentTimeSec))
            }
            currentTimeSec = target
            if (usingEmbedFallback) embedController.send("seek", target)
            else player.seekTo(target * 1000L)
            Toast.makeText(context, "Auto-skipped ED", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(sleepTimerRemainingMin) {
        if (sleepTimerRemainingMin > 0) {
            delay(60000L)
            sleepTimerRemainingMin -= 1
            if (sleepTimerRemainingMin == 0) {
                player.pause()
                embedController.send("pause")
                Toast.makeText(context, "Sleep timer paused playback", Toast.LENGTH_LONG).show()
            }
        }
    }

    val anySheetOpen = showSpeedDialog || showSubtitleDialog || showAudioDialog ||
        showFitDialog || showServerSheet || showSourceSheet ||
        showEpisodeSheet || showSleepTimerDialog || showDiagnostics

    LaunchedEffect(showControls, isLocked, isHoldingSpeed, anySheetOpen, isScrubbing) {
        if (showControls && !isLocked && !isHoldingSpeed && !anySheetOpen && !isScrubbing) {
            delay(savedSettings.controlsAutoHideSec.coerceAtLeast(3) * 1000L)
            if (!isScrubbing && !isHoldingSpeed && !anySheetOpen) {
                showControls = false
            }
        }
    }

    LaunchedEffect(lockPillVisible) {
        if (lockPillVisible) {
            delay(3500L)
            lockPillVisible = false
        }
    }

    // Auto lock after a while when enabled.
    LaunchedEffect(isLocked, isPlaying, savedSettings.autoLockEnabled) {
        if (!isLocked && savedSettings.autoLockEnabled && isPlaying) {
            delay(savedSettings.autoLockDelaySec.coerceAtLeast(15) * 1000L)
            if (savedSettings.lockButtonEnabled) pendingLock = true
        }
    }

    LaunchedEffect(seekFeedbackText) {
        if (seekFeedbackText != null) {
            delay(700L)
            seekFeedbackText = null
        }
    }

    LaunchedEffect(gestureFeedback) {
        if (gestureFeedback != null) {
            delay(900L)
            gestureFeedback = null
        }
    }

    // Apply playback speed. While a hold is active the target is either the fast or the slow
    // multiplier; with ramping on we walk up to it a step at a time instead of jumping there.
    LaunchedEffect(
        isHoldingSpeed,
        holdSpeedTarget,
        playbackSpeed,
        savedSettings.holdToSpeedEnabled,
        savedSettings.holdToSpeedRampUp,
        usingEmbedFallback
    ) {
        fun applySpeed(value: Float) {
            if (usingEmbedFallback) embedController.send("setRate", value.toString())
            else player.setPlaybackSpeed(value)
        }
        if (!isHoldingSpeed || !savedSettings.holdToSpeedEnabled) {
            applySpeed(playbackSpeed)
            return@LaunchedEffect
        }
        if (!savedSettings.holdToSpeedRampUp) {
            applySpeed(holdSpeedTarget)
            return@LaunchedEffect
        }
        val start = if (usingEmbedFallback) playbackSpeed
        else player.playbackParameters.speed.takeIf { it > 0f } ?: playbackSpeed
        val steps = 8
        repeat(steps) { index ->
            applySpeed(start + (holdSpeedTarget - start) * ((index + 1).toFloat() / steps))
            delay(40L)
        }
    }

    LaunchedEffect(
        savedSettings.subtitlesEnabled,
        savedSettings.subtitleFontSizeSp,
        savedSettings.subtitleTextColor,
        savedSettings.subtitleBackgroundColor,
        savedSettings.subtitleOutline,
        savedSettings.subtitleBold,
        savedSettings.subtitlePosition,
        usingEmbedFallback,
        isReady
    ) {
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !savedSettings.subtitlesEnabled)
            .build()
        if (usingEmbedFallback) {
            val textColorHex = when (savedSettings.subtitleTextColor) {
                "Yellow" -> "#FFEB3B"
                "Cyan" -> "#00E5FF"
                "Green" -> "#69F0AE"
                else -> "#FFFFFF"
            }
            val bgCss = when (savedSettings.subtitleBackgroundColor) {
                "Black", "BlackSolid" -> "rgba(0,0,0,0.9)"
                "BlackSemi" -> "rgba(0,0,0,0.6)"
                "Grey" -> "rgba(68,68,68,0.67)"
                "None" -> "transparent"
                else -> "rgba(0,0,0,0.6)"
            }
            val payload = org.json.JSONObject().apply {
                put("enabled", savedSettings.subtitlesEnabled)
                put("fontSizeSp", savedSettings.subtitleFontSizeSp)
                put("textColor", textColorHex)
                put("bgColor", bgCss)
                put("outline", savedSettings.subtitleOutline)
                put("bold", savedSettings.subtitleBold)
                put("position", savedSettings.subtitlePosition)
            }.toString()
            embedController.send("setSubtitleStyle", payload)
        }
    }

    LaunchedEffect(savedSettings.videoFitMode, usingEmbedFallback, isReady) {
        if (usingEmbedFallback) {
            embedController.send("setFit", savedSettings.videoFitMode.lowercase())
        } else {
            applyResizeMode(player, savedSettings.videoFitMode)
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Actions
    // ---------------------------------------------------------------------------------------------
    fun playEpisode(number: Int) {
        if (episodes.none { it.number == number } && number != currentEpNumber) return
        localPath = null
        localSubtitlePath = null
        currentEpNumber = number
        currentTimeSec = 0L
        isReady = false
        autoNextCancelled = false
        introSkippedForEp = false
        outroSkippedForEp = false
        showAutoNextBanner = false
        showEpisodeSheet = false
    }

    fun seekTo(target: Long) {
        val clamped = target.coerceAtLeast(0L).let { if (durationSec > 0) it.coerceAtMost(durationSec) else it }
        lastSeekTimestampMs = System.currentTimeMillis()
        currentTimeSec = clamped
        if (usingEmbedFallback) embedController.send("seek", clamped)
        else player.seekTo(clamped * 1000L)
    }

    fun seekBy(delta: Long) {
        val base = if (usingEmbedFallback) currentTimeSec else (player.currentPosition / 1000L)
        val target = (base + delta).coerceAtLeast(0L)
        seekTo(target)
        seekFeedbackText = (if (delta >= 0) "+" else "") + delta + "s"
    }

    fun togglePlayback() {
        if (usingEmbedFallback) {
            embedController.send("toggle")
            return
        }
        if (player.isPlaying) player.pause() else player.play()
    }

    fun tryNextSource() {
        if (sources.size > sourceIndex + 1) {
            sourceIndex += 1
            playbackError = null
            scope.launch { Toast.makeText(context, "Switching to another source…", Toast.LENGTH_SHORT).show() }
        } else if (savedSettings.autoFailover && !usingEmbedFallback) {
            playbackError = "All resolved sources failed. Try another server or the built-in player."
        }
    }

    fun lockPlayer() {
        isLocked = true
        showControls = false
        lockPillVisible = true
        if (savedSettings.lockHapticFeedback) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        val mode = savedSettings.orientationLockMode
        activity?.requestedOrientation = when (mode) {
            "Landscape" -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            "Portrait" -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            "Lock Current" -> ActivityInfo.SCREEN_ORIENTATION_LOCKED
            else -> {
                // In Auto mode, preserve current orientation so locking in fullscreen landscape never drops to portrait
                if (isLandscape) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                else ActivityInfo.SCREEN_ORIENTATION_LOCKED
            }
        }
    }

    fun unlockPlayer() {
        isLocked = false
        lockPillVisible = false
        showControls = true
        if (savedSettings.lockHapticFeedback) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        if (isLandscape || savedSettings.orientationLockMode == "Landscape") {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else if (savedSettings.orientationLockMode == "Portrait") {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    /** Leaves fullscreen and always restores the controls, so the player can never look dead. */
    fun exitFullscreen() {
        manualFullscreen = false
        activity?.requestedOrientation = if (savedSettings.orientationLockMode == "Auto") {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        isHoldingSpeed = false
        showControls = true
    }

    fun toggleFullscreen() {
        isHoldingSpeed = false
        showControls = true
        if (isLandscape) {
            manualFullscreen = false
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            manualFullscreen = true
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }

    fun enterPip() {
        if (!savedSettings.pipEnabled) {
            Toast.makeText(context, "Picture-in-picture is disabled in Player settings", Toast.LENGTH_SHORT).show()
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                showControls = false
                activity?.enterPictureInPictureMode(
                    PictureInPictureParams.Builder()
                        .setAspectRatio(Rational(16, 9))
                        .build()
                )
            } catch (t: Throwable) {
                Toast.makeText(context, "Picture-in-picture unavailable", Toast.LENGTH_SHORT).show()
            }
        }
    }

    BackHandler {
        when {
            isLocked -> Toast.makeText(context, "Player is locked — tap the padlock to unlock", Toast.LENGTH_SHORT).show()
            isLandscape -> exitFullscreen()
            else -> onBackClick()
        }
    }

    // Immersive status/navigation bars in landscape.
    val composeView = view
    DisposableEffect(isLandscape) {
        val window = activity?.window
        val controller = if (window != null) WindowCompat.getInsetsController(window, composeView) else null
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (isLandscape) controller?.hide(WindowInsetsCompat.Type.systemBars())
        else controller?.show(WindowInsetsCompat.Type.systemBars())
        onDispose { controller?.show(WindowInsetsCompat.Type.systemBars()) }
    }

    // A rotation rebuilds the whole control overlay and cancels any in-flight gesture, so chrome is
    // forced back on for both directions. Without this, leaving fullscreen after the controls had
    // auto-hidden looked like a player that no longer responded.
    LaunchedEffect(isLandscape) {
        isHoldingSpeed = false
        if (!isLocked && !isInPip) {
            showControls = true
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Gestures
    // ---------------------------------------------------------------------------------------------
    fun onTapAt(x: Float, width: Float) {
        if (!showControls) {
            showControls = true
        } else if (savedSettings.singleTapTogglesControls) {
            showControls = false
        }
    }

    fun onDoubleTapAt(x: Float, width: Float) {
        if (!savedSettings.doubleTapSeekEnabled) return
        val step = savedSettings.seekDurationSec.coerceAtLeast(5)
        when {
            x < width * 0.45f -> {
                if (doubleTapSeekSide != "left") {
                    doubleTapSeekAccumulated = 0
                }
                doubleTapSeekSide = "left"
                doubleTapSeekAccumulated += step
                seekBy(-step.toLong())
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                doubleTapDismissJob?.cancel()
                doubleTapDismissJob = scope.launch {
                    delay(750L)
                    doubleTapSeekSide = null
                    doubleTapSeekAccumulated = 0
                }
            }
            x > width * 0.55f -> {
                if (doubleTapSeekSide != "right") {
                    doubleTapSeekAccumulated = 0
                }
                doubleTapSeekSide = "right"
                doubleTapSeekAccumulated += step
                seekBy(step.toLong())
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                doubleTapDismissJob?.cancel()
                doubleTapDismissJob = scope.launch {
                    delay(750L)
                    doubleTapSeekSide = null
                    doubleTapSeekAccumulated = 0
                }
            }
            else -> onTapAt(x, width)
        }
    }

    /**
     * Which half a hold speeds playback up on. "Anywhere" treats the right half as the fast one,
     * matching the way double-tap seeking already reads left and right.
     */
    fun isFastHoldHalf(x: Float, width: Float): Boolean = when (savedSettings.holdToSpeedZone) {
        "Left Half" -> x <= width / 2f
        "Right Half" -> x >= width / 2f
        else -> x >= width / 2f
    }

    /**
     * Starts a hold at [x]. The configured zone is the half that speeds up; when slow motion is
     * enabled the opposite half slows down instead, so one long press covers both directions.
     */
    fun startHoldToSpeed(x: Float, width: Float) {
        if (!savedSettings.holdToSpeedEnabled) return
        val fastHalf = isFastHoldHalf(x, width)
        // Outside the fast half a hold only does something when it is the slow-motion half.
        if (!fastHalf && !savedSettings.holdToSpeedSlowDown && savedSettings.holdToSpeedZone != "Anywhere") return
        holdSpeedTarget = if (savedSettings.holdToSpeedSlowDown && !fastHalf) effectiveSlowMultiplier
        else effectiveMultiplier
        isHoldingSpeed = true
        if (savedSettings.holdToSpeedHaptic) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun stopHoldToSpeed() {
        if (!isHoldingSpeed) return
        isHoldingSpeed = false
        if (savedSettings.holdToSpeedKeepAfterRelease) {
            playbackSpeed = holdSpeedTarget
            Toast.makeText(
                context,
                "Playback speed locked at ${formatSpeed(holdSpeedTarget)}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun playerGestures(): Modifier = Modifier.pointerInput(
        // The whole settings object is a key so that a change made on another screen (hold zone,
        // multiplier, gestures) applies to the running player instead of a stale capture.
        savedSettings,
        isLocked,
        // Restarting on rotation keeps the tap and hold regions matched to the new layout size
        // instead of the dimensions this block was first built with.
        isLandscape
    ) {
        var lastTapTime = 0L
        var pendingSingleTapJob: kotlinx.coroutines.Job? = null
        // Bumped on every press, on the first drag and on release. A pending hold only fires while
        // its generation is still current, which is how a drag or a quick tap cancels the timer.
        // Plain (non-state) locals are fine: the pointer loop and the timer coroutine both run on
        // the main thread, and a write here must not recompose the player.
        var holdGeneration = 0
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val width = size.width.toFloat()
            val height = size.height.toFloat()
            val downX = down.position.x
            val downY = down.position.y
            var moved = false
            var startVolume = videoVolume
            var startBrightness = brightness
            var seekPreview: Long? = null
            var openedCenterSettings = false

            // A finger that rests still produces no pointer events at all, so the old in-loop
            // `awaitPointerEvent()` time check never saw the 450ms mark and hold-to-speed never
            // fired. A real coroutine drives the timer instead and reports back when it elapses.
            // `down.isConsumed` means a control under the finger already took the press, which must
            // stay a button press rather than becoming a speed hold.
            if (savedSettings.holdToSpeedEnabled && !down.isConsumed && !isLocked) {
                holdGeneration++
                val generation = holdGeneration
                scope.launch {
                    delay(HOLD_TO_SPEED_DELAY_MS)
                    if (generation == holdGeneration) startHoldToSpeed(downX, width)
                }
            }

            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) {
                    break
                }
                val dx = change.position.x - downX
                val dy = change.position.y - downY

                if (!isHoldingSpeed && (abs(dx) > 24f || abs(dy) > 24f)) {
                    moved = true
                    // Dragging is not holding: drop any hold that has not started yet.
                    holdGeneration++
                }

                if (isHoldingSpeed) continue

                if (moved && !isLocked) {
                    if (abs(dx) > abs(dy) && savedSettings.gestureHorizontalSeekEnabled) {
                        val deltaSec = (dx / width * 120f).toLong()
                        val base = if (usingEmbedFallback) currentTimeSec else (player.currentPosition / 1000L)
                        seekPreview = (base + deltaSec).coerceIn(0L, if (durationSec > 0) durationSec else Long.MAX_VALUE)
                        seekFeedbackText = (if (deltaSec >= 0) "+" else "") + deltaSec + "s"
                    } else if (abs(dy) > abs(dx)) {
                        val vDelta = -dy / height
                        val inCenterBand = savedSettings.swipeUpDownOpensSettings &&
                            downX in (width * 0.35f)..(width * 0.65f)
                        if (inCenterBand) {
                            if (!openedCenterSettings && abs(dy) > 48f) {
                                openedCenterSettings = true
                                showSpeedDialog = true
                            }
                        } else if (downX > width / 2 && savedSettings.gestureVolumeEnabled) {
                            val next = (startVolume + vDelta).coerceIn(0f, 1f)
                            videoVolume = next
                            player.volume = next
                            if (usingEmbedFallback) {
                                embedController.send("setVolume", next)
                            }
                            gestureHudType = "volume"
                            gestureHudLevel = next
                            gestureFeedback = "Volume ${(next * 100).toInt()}%"
                        } else if (downX <= width / 2 && savedSettings.gestureBrightnessEnabled) {
                            val next = (startBrightness + vDelta).coerceIn(0.05f, 1f)
                            brightness = next
                            val lp = activity?.window?.attributes
                            if (lp != null) {
                                lp.screenBrightness = next
                                activity.window.attributes = lp
                            }
                            gestureHudType = "brightness"
                            gestureHudLevel = next
                            gestureFeedback = "Brightness ${(next * 100).toInt()}%"
                        }
                    }
                }
            }

            // Released. Bumping the generation first also covers a quick tap.
            holdGeneration++
            if (isHoldingSpeed) {
                stopHoldToSpeed()
            } else if (seekPreview != null) {
                seekTo(seekPreview)
                seekFeedbackText = null
            } else if (!moved) {
                val now = System.currentTimeMillis()
                if (savedSettings.doubleTapSeekEnabled) {
                    if (now - lastTapTime < 280L) {
                        pendingSingleTapJob?.cancel()
                        pendingSingleTapJob = null
                        lastTapTime = 0L
                        onDoubleTapAt(downX, width)
                    } else {
                        lastTapTime = now
                        pendingSingleTapJob?.cancel()
                        pendingSingleTapJob = scope.launch {
                            delay(280L)
                            onTapAt(downX, width)
                        }
                    }
                } else {
                    onTapAt(downX, width)
                }
            }
        }
    }

    // Applies intents raised by the listener and by the effects declared above.
    LaunchedEffect(pendingAdvance) {
        if (pendingAdvance) {
            pendingAdvance = false
            tryNextSource()
        }
    }

    LaunchedEffect(pendingEpisode) {
        pendingEpisode?.let { target ->
            pendingEpisode = null
            playEpisode(target)
        }
    }

    LaunchedEffect(pendingLock) {
        if (pendingLock) {
            pendingLock = false
            lockPlayer()
        }
    }

    if (anime == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(DarkBackground),
            contentAlignment = Alignment.Center
        ) {
            Text("Loading anime…", color = TextSecondary)
        }
        return
    }

    val currentAnime: Anime = anime!!
    val activeEmbedProvider = remember(currentServer, currentLanguage) {
        providerManager.getProviderForLanguage(currentServer, currentLanguage)
    }
    val wrapEmbeds = activeEmbedProvider.capabilities.requiresEmbed

    val embedUrl = remember(currentAnime.id, currentEpNumber, currentLanguage, currentServer) {
        activeEmbedProvider.getEmbedUrl(
            anime = currentAnime,
            episode = currentEpisode,
            language = currentLanguage,
            autoplay = savedSettings.autoplay,
            autoSkip = savedSettings.autoSkipIntro,
            startAtSec = 0L
        )
    }

    val playerAccent = remember(savedSettings.playerAccentOverride, themeAccent) {
        if (savedSettings.playerAccentOverride.equals("Theme", ignoreCase = true)) {
            themeAccent
        } else {
            getThemeAccentColor(savedSettings.playerAccentOverride)
        }
    }
    val iconScaleMult = when (savedSettings.playerControlSize) {
        "Compact" -> 0.85f
        "Large" -> 1.18f
        else -> 1f
    }
    val hiddenControlSet = remember(savedSettings.playerHiddenControls) {
        savedSettings.playerHiddenControls.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()
    }
    val topBarControlOrder = remember(savedSettings.playerTopBarOrder) {
        savedSettings.playerTopBarOrder.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
    }

    val pageSize = 25
    val filteredEpisodes = remember(episodes, episodeQuery) {
        if (episodeQuery.isBlank()) episodes
        else episodes.filter { it.number.toString().contains(episodeQuery) || it.title.contains(episodeQuery, true) }
    }
    val episodeChunks = remember(filteredEpisodes) {
        if (filteredEpisodes.isEmpty()) emptyList() else filteredEpisodes.chunked(pageSize)
    }
    LaunchedEffect(currentEpNumber, episodeChunks) {
        val idx = episodeChunks.indexOfFirst { chunk -> chunk.any { it.number == currentEpNumber } }
        if (idx >= 0) selectedEpisodeRangeIndex = idx
    }
    val currentChunkEpisodes = remember(episodeChunks, selectedEpisodeRangeIndex) {
        if (episodeChunks.isNotEmpty() && selectedEpisodeRangeIndex in episodeChunks.indices) {
            episodeChunks[selectedEpisodeRangeIndex]
        } else filteredEpisodes
    }
    val currentDownload = allDownloads.firstOrNull {
        it.animeId == currentAnime.id && it.episodeNumber == currentEpNumber
    }
    val transportPlayPauseButton: @Composable () -> Unit = {
        val playInteractionSource = remember { MutableInteractionSource() }
        val isPlayPressed by playInteractionSource.collectIsPressedAsState()
        val playScale by animateFloatAsState(
            targetValue = if (isPlayPressed) 0.88f else if (isPlaying) 1f else 1.06f,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "play_scale"
        )
        Box(
            modifier = Modifier
                .size((54f * iconScaleMult).dp)
                .scale(playScale)
                .background(playerAccent, CircleShape)
                .clickable(
                    onClick = { togglePlayback() },
                    interactionSource = playInteractionSource,
                    indication = ripple(bounded = true, radius = 27.dp)
                )
                .testTag("player_play_pause_button"),
            contentAlignment = Alignment.Center
        ) {
            if (isBuffering) {
                CircularProgressIndicator(
                    color = DarkBackground,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = DarkBackground,
                    modifier = Modifier.size((30f * iconScaleMult).dp)
                )
            }
        }
    }

    val transportSecondaryControls: @Composable () -> Unit = {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            IconButton(
                onClick = { playEpisode(currentEpNumber - 1) },
                enabled = currentEpNumber > 1,
                modifier = Modifier.testTag("player_prev_episode")
            ) {
                Icon(
                    Icons.Default.SkipPrevious,
                    contentDescription = "Previous episode",
                    tint = if (currentEpNumber > 1) Color.White else Color.Gray,
                    modifier = Modifier.size(26.dp)
                )
            }

            TransportButton(onClick = { seekBy(-savedSettings.seekDurationSec.toLong()) }) {
                Icon(
                    Icons.Default.Replay10,
                    contentDescription = "Rewind",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }

            TransportButton(onClick = { seekBy(savedSettings.seekDurationSec.toLong()) }) {
                Icon(
                    Icons.Default.Forward10,
                    contentDescription = "Forward",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }

            val nextExists = episodes.any { it.number == currentEpNumber + 1 }
            IconButton(
                onClick = { playEpisode(currentEpNumber + 1) },
                enabled = nextExists,
                modifier = Modifier.testTag("player_next_episode")
            ) {
                Icon(
                    Icons.Default.SkipNext,
                    contentDescription = "Next episode",
                    tint = if (nextExists) Color.White else Color.Gray,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }

    val transportControls: @Composable () -> Unit = {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            IconButton(
                onClick = { playEpisode(currentEpNumber - 1) },
                enabled = currentEpNumber > 1,
                modifier = Modifier.testTag("player_prev_episode")
            ) {
                Icon(
                    Icons.Default.SkipPrevious,
                    contentDescription = "Previous episode",
                    tint = if (currentEpNumber > 1) Color.White else Color.Gray,
                    modifier = Modifier.size(26.dp)
                )
            }

            TransportButton(onClick = { seekBy(-savedSettings.seekDurationSec.toLong()) }) {
                Icon(
                    Icons.Default.Replay10,
                    contentDescription = "Rewind",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }

            transportPlayPauseButton()

            TransportButton(onClick = { seekBy(savedSettings.seekDurationSec.toLong()) }) {
                Icon(
                    Icons.Default.Forward10,
                    contentDescription = "Forward",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }

            val nextExists = episodes.any { it.number == currentEpNumber + 1 }
            IconButton(
                onClick = { playEpisode(currentEpNumber + 1) },
                enabled = nextExists,
                modifier = Modifier.testTag("player_next_episode")
            ) {
                Icon(
                    Icons.Default.SkipNext,
                    contentDescription = "Next episode",
                    tint = if (nextExists) Color.White else Color.Gray,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
            .then(if (!isLandscape && !isInPip) Modifier.statusBarsPadding() else Modifier)
    ) {
        val playerModifier = if (isLandscape || isInPip) {
            Modifier.fillMaxSize()
        } else {
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        }

        Box(
            modifier = playerModifier
                .background(Color.Black)
        ) {
            if (usingEmbedFallback) {
                EmbedPlayerView(
                    embedUrl = embedUrl,
                    wrapEmbeds = wrapEmbeds,
                    state = embedState,
                    controller = embedController,
                    adBlockEnabled = savedSettings.adBlockEnabled
                )
                LaunchedEffect(embedState.value) {
                    isReady = embedState.value.ready
                    isPlaying = embedState.value.playing
                    isBuffering = embedState.value.buffering
                    durationSec = embedState.value.durationSec
                    bufferedTimeSec = embedState.value.bufferedSec
                    if (!isScrubbing && (System.currentTimeMillis() - lastSeekTimestampMs > 850L)) {
                        currentTimeSec = embedState.value.positionSec
                    }
                }
            } else {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            useController = false
                            this.player = player
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                            applyCaptionStyle(this, savedSettings)
                        }
                    },
                    update = { playerView ->
                        playerView.player = player
                        playerView.resizeMode = resizeModeFor(savedSettings.videoFitMode)
                        applyCaptionStyle(playerView, savedSettings)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Gesture layer: detects taps, swipes, and hold-to-speed when controls are not active.
            // When controls are active, the controls overlay handles clicks and taps directly.
            if (!isLocked && !showControls) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(playerGestures())
                )
            }

            if (isResolving) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xCC000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = playerAccent, strokeWidth = 3.dp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Finding a stream…", color = TextPrimary, fontSize = 13.sp)
                    }
                }
            }

            if (playbackError != null) {
                PlaybackErrorOverlay(
                    message = playbackError!!,
                    accentColor = playerAccent,
                    onRetry = {
                        playbackError = null
                        resolveNonce += 1
                    },
                    onShowDetails = { showDiagnostics = true },
                    onUseEmbed = {
                        playbackError = null
                        currentServer = StreamResolverRegistry.EMBED_SERVER_ID
                    },
                    onBack = onBackClick
                )
            }

            // YouTube-style dynamic double-tap seeking ripples (Left side)
            if (doubleTapSeekSide == "left") {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.38f)
                        .align(Alignment.CenterStart)
                        .clip(RoundedCornerShape(topEnd = 160.dp, bottomEnd = 160.dp))
                        .background(Color(0x66000000))
                        .border(0.5.dp, playerAccent.copy(alpha = 0.5f), RoundedCornerShape(topEnd = 160.dp, bottomEnd = 160.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FastRewind,
                                contentDescription = null,
                                tint = playerAccent,
                                modifier = Modifier.size(34.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "-${doubleTapSeekAccumulated}s",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Double-tap to seek", color = TextSecondary, fontSize = 10.sp)
                    }
                }
            }

            // YouTube-style dynamic double-tap seeking ripples (Right side)
            if (doubleTapSeekSide == "right") {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.38f)
                        .align(Alignment.CenterEnd)
                        .clip(RoundedCornerShape(topStart = 160.dp, bottomStart = 160.dp))
                        .background(Color(0x66000000))
                        .border(0.5.dp, playerAccent.copy(alpha = 0.5f), RoundedCornerShape(topStart = 160.dp, bottomStart = 160.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "+${doubleTapSeekAccumulated}s",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.FastForward,
                                contentDescription = null,
                                tint = playerAccent,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Double-tap to seek", color = TextSecondary, fontSize = 10.sp)
                    }
                }
            }

            if (seekFeedbackText != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xCC000000))
                        .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp))
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = seekFeedbackText!!,
                        color = playerAccent,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            // Sleek Minimalist Pill HUD Indicator for Volume & Brightness
            if (gestureFeedback != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xEE121622))
                        .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp))
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = when {
                                gestureHudType == "brightness" -> Icons.Default.Brightness6
                                gestureHudLevel <= 0.05f -> Icons.AutoMirrored.Filled.VolumeOff
                                else -> Icons.AutoMirrored.Filled.VolumeUp
                            },
                            contentDescription = null,
                            tint = playerAccent,
                            modifier = Modifier.size(20.dp)
                        )

                        Box(
                            modifier = Modifier
                                .width(70.dp)
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(DarkSurfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(gestureHudLevel.coerceIn(0f, 1f))
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(playerAccent)
                            )
                        }

                        Text(
                            text = "${(gestureHudLevel * 100).toInt()}%",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (isHoldingSpeed && savedSettings.holdToSpeedShowBadge) {
                val badgeAlignment = when (savedSettings.holdToSpeedBadgePosition) {
                    "Top" -> Alignment.TopCenter
                    "Bottom" -> Alignment.BottomCenter
                    else -> Alignment.Center
                }
                Box(
                    modifier = Modifier
                        .align(badgeAlignment)
                        .then(
                            when (savedSettings.holdToSpeedBadgePosition) {
                                "Top" -> Modifier.statusBarsPadding().padding(top = 16.dp)
                                "Bottom" -> Modifier.navigationBarsPadding().padding(bottom = 16.dp)
                                else -> Modifier
                            }
                        )
                        .clip(RoundedCornerShape(20.dp))
                        .background(playerAccent.copy(alpha = 0.9f))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .testTag("player_hold_speed_badge")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (holdSpeedTarget < 1f) Icons.Default.FastRewind
                            else Icons.Default.FastForward,
                            contentDescription = null,
                            tint = DarkBackground,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = buildString {
                                append(formatSpeed(holdSpeedTarget))
                                if (holdSpeedTarget < 1f) append(" slow motion")
                                append(
                                    if (savedSettings.holdToSpeedKeepAfterRelease) " • stays after release"
                                    else " • release to resume"
                                )
                            },
                            color = DarkBackground,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            val lockSideAlign = if (savedSettings.lockButtonSide.equals("Right", ignoreCase = true)) {
                Alignment.CenterEnd
            } else {
                Alignment.CenterStart
            }

            // Locked: block touches and show unobtrusive edge/letterbox lock icon (or legacy center pill)
            if (isLocked) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (savedSettings.lockBlocksTouches) {
                                lockPillVisible = !lockPillVisible
                            } else {
                                unlockPlayer()
                            }
                        }
                )

                val showLockControl = lockPillVisible || savedSettings.showLockedBadge
                if (savedSettings.useLegacyPlayerLayout) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showLockControl,
                        enter = fadeIn(animationSpec = tween(180)) + scaleIn(initialScale = 0.85f),
                        exit = fadeOut(animationSpec = tween(220)) + scaleOut(targetScale = 0.85f),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .zIndex(100f)
                    ) {
                        Surface(
                            onClick = { unlockPlayer() },
                            shape = RoundedCornerShape(32.dp),
                            color = Color(0xF0131722),
                            border = BorderStroke(1.5.dp, playerAccent),
                            shadowElevation = 16.dp,
                            modifier = Modifier.testTag("player_unlock_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(playerAccent.copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Unlock",
                                        tint = playerAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Screen Locked",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Tap to unlock controls",
                                        color = playerAccent,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Redesigned unobtrusive lock icon positioned in the Fit letterbox edge area
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showLockControl,
                        enter = fadeIn(animationSpec = tween(160)) + scaleIn(initialScale = 0.8f),
                        exit = fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 0.8f),
                        modifier = Modifier
                            .align(lockSideAlign)
                            .then(if (isLandscape) Modifier.displayCutoutPadding() else Modifier)
                            .padding(horizontal = 10.dp)
                            .zIndex(100f)
                    ) {
                        Surface(
                            onClick = { unlockPlayer() },
                            shape = CircleShape,
                            color = Color(0xCC0D111A),
                            border = BorderStroke(1.dp, playerAccent.copy(alpha = 0.85f)),
                            shadowElevation = 8.dp,
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("player_unlock_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Unlock screen",
                                    tint = playerAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (dubStatusBanner != null && !isLocked) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = if (showControls) 56.dp else 12.dp, start = 16.dp, end = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xE6161B26))
                        .border(1.dp, playerAccent.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .zIndex(90f)
                        .testTag("player_dub_status_banner")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Audiotrack,
                            contentDescription = null,
                            tint = playerAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = dubStatusBanner!!,
                            color = TextPrimary,
                            fontSize = 11.sp,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Switch to SUB",
                            color = playerAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                dubStatusBanner = null
                                currentLanguage = "sub"
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "✕",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            modifier = Modifier.clickable { dubStatusBanner = null }
                        )
                    }
                }
            }

            // Unobtrusive screen-lock icon in the letterbox margin (Left or Right per user preference)
            if (showControls && !isLocked && !isInPip && savedSettings.lockButtonEnabled && !savedSettings.useLegacyPlayerLayout) {
                Box(
                    modifier = Modifier
                        .align(lockSideAlign)
                        .then(if (isLandscape) Modifier.displayCutoutPadding() else Modifier)
                        .padding(horizontal = 10.dp)
                        .zIndex(85f)
                ) {
                    Surface(
                        onClick = { lockPlayer() },
                        shape = CircleShape,
                        color = Color(0xB30D111A),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("player_lock_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.LockOpen,
                                contentDescription = "Lock screen",
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Floating Skip OP / Skip ED pill outside AnimatedVisibility(visible = showControls)
            if (!isLocked && !isInPip && savedSettings.showSkipButtons) {
                val skipBottomPadding = if (showControls) {
                    if (isLandscape) 96.dp else 68.dp
                } else {
                    16.dp
                }
                val introSkip = savedSettings.introSkipSeconds.coerceIn(5, 300)
                val outroSkip = savedSettings.outroSkipSeconds.coerceIn(5, 300)
                val chapIntroStart = embedState.value.introStartSec
                val chapIntroEnd = embedState.value.introEndSec
                val hasChapIntro = usingEmbedFallback && chapIntroEnd > chapIntroStart && chapIntroStart >= 0L
                val chapOutroStart = embedState.value.outroStartSec
                val chapOutroEnd = embedState.value.outroEndSec
                val hasChapOutro = usingEmbedFallback && chapOutroEnd > chapOutroStart && chapOutroStart >= 0L

                val showIntroSkip = !introSkippedForEp && (
                    if (hasChapIntro) currentTimeSec in chapIntroStart..chapIntroEnd
                    else currentTimeSec in 0..(introSkip + 35).toLong()
                )
                val showOutroSkip = !outroSkippedForEp && (
                    if (hasChapOutro) currentTimeSec in chapOutroStart..chapOutroEnd
                    else durationSec > 0 && currentTimeSec > durationSec - (outroSkip + 30).toLong()
                )

                if (showIntroSkip || showOutroSkip) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 12.dp, bottom = skipBottomPadding)
                            .zIndex(92f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (showIntroSkip) {
                            SkipChip(
                                label = if (hasChapIntro) "Skip OP" else "+${introSkip}s Skip OP",
                                accentColor = playerAccent,
                                onClick = {
                                    introSkippedForEp = true
                                    if (hasChapIntro) seekTo(chapIntroEnd) else seekBy(introSkip.toLong())
                                }
                            )
                        }
                        if (showOutroSkip) {
                            SkipChip(
                                label = if (hasChapOutro) "Skip ED" else "+${outroSkip}s Skip ED",
                                accentColor = playerAccent,
                                onClick = {
                                    outroSkippedForEp = true
                                    val target = if (hasChapOutro) {
                                        chapOutroEnd
                                    } else {
                                        minOf(
                                            currentTimeSec + outroSkip.toLong(),
                                            (durationSec - 5L).coerceAtLeast(currentTimeSec)
                                        )
                                    }
                                    seekTo(target)
                                }
                            )
                        }
                    }
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = showControls && !isLocked && !isInPip,
                enter = fadeIn(animationSpec = tween(180)),
                exit = fadeOut(animationSpec = tween(180)),
                modifier = Modifier.fillMaxSize()
            ) {
                val scrimAlpha = savedSettings.playerOverlayDimAlpha.coerceIn(0.2f, 0.85f)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = scrimAlpha))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            showControls = false
                        }
                ) {
                    // ----- top bar -----
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (isLandscape) Modifier.statusBarsPadding().displayCutoutPadding() else Modifier.padding(top = 4.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            PlayerIconButton(
                                onClick = {
                                    if (isLandscape) exitFullscreen() else onBackClick()
                                },
                                modifier = Modifier.testTag("player_back_button")
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White,
                                    modifier = Modifier.size((20f * iconScaleMult).dp)
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .widthIn(min = 110.dp)
                                    .padding(horizontal = if (isLandscape) 8.dp else 4.dp)
                            ) {
                                Text(
                                    text = "${currentAnime.displayTitle(savedSettings.titleLanguagePreference)} • Ep $currentEpNumber",
                                    color = TextPrimary,
                                    fontSize = if (isLandscape) 13.sp else 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${currentLanguage.uppercase()} • ${currentSource?.serverName ?: currentServer}",
                                    color = playerAccent,
                                    fontSize = if (isLandscape) 11.sp else 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                if (savedSettings.useLegacyPlayerLayout) {
                                    // Legacy layout keeps the original fixed top bar order (including top lock button)
                                    PlayerIconButton(onClick = { showSpeedDialog = true }) {
                                        Icon(Icons.Default.Speed, contentDescription = "Speed", tint = Color.White, modifier = Modifier.size(19.dp))
                                    }
                                    PlayerIconButton(onClick = { showSubtitleDialog = true }) {
                                        Icon(
                                            Icons.Default.Subtitles,
                                            contentDescription = "Subtitles",
                                            tint = if (savedSettings.subtitlesEnabled) playerAccent else Color.White,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                    PlayerIconButton(onClick = { showAudioDialog = true }) {
                                        Icon(Icons.Default.Audiotrack, contentDescription = "Audio track", tint = Color.White, modifier = Modifier.size(19.dp))
                                    }
                                    PlayerIconButton(onClick = {
                                        if (sources.isNotEmpty()) showSourceSheet = true else showServerSheet = true
                                    }) {
                                        Icon(Icons.Default.Dns, contentDescription = "Servers and quality", tint = Color.White, modifier = Modifier.size(19.dp))
                                    }
                                    PlayerIconButton(
                                        onClick = { lockPlayer() },
                                        modifier = Modifier.testTag("player_lock_button")
                                    ) {
                                        Icon(Icons.Default.LockOpen, contentDescription = "Lock player", tint = Color.White, modifier = Modifier.size(19.dp))
                                    }
                                    PlayerIconButton(onClick = { showFitDialog = true }) {
                                        Icon(Icons.Default.AspectRatio, contentDescription = "Aspect ratio", tint = Color.White, modifier = Modifier.size(19.dp))
                                    }
                                    if (isLandscape) {
                                        PlayerIconButton(onClick = { showSleepTimerDialog = true }) {
                                            Icon(
                                                Icons.Default.NightlightRound,
                                                contentDescription = "Sleep timer",
                                                tint = if (sleepTimerRemainingMin > 0) playerAccent else Color.White,
                                                modifier = Modifier.size(19.dp)
                                            )
                                        }
                                        PlayerIconButton(onClick = { showEpisodeSheet = true }) {
                                            Icon(Icons.Default.VideoLibrary, contentDescription = "Episodes", tint = Color.White, modifier = Modifier.size(19.dp))
                                        }
                                    }
                                    if (savedSettings.pipEnabled) {
                                        PlayerIconButton(onClick = { enterPip() }) {
                                            Icon(Icons.Default.PictureInPictureAlt, contentDescription = "Picture in picture", tint = Color.White, modifier = Modifier.size(19.dp))
                                        }
                                    }
                                    if (savedSettings.chromecastEnabled) {
                                        PlayerIconButton(onClick = { showCastDialog = true }) {
                                            Icon(
                                                imageVector = Icons.Default.Tv,
                                                contentDescription = "Cast to Smart TV",
                                                tint = if (isCasting) playerAccent else Color.White,
                                                modifier = Modifier.size(19.dp)
                                            )
                                        }
                                    }
                                    PlayerIconButton(onClick = { toggleFullscreen() }) {
                                        Icon(
                                            imageVector = if (isLandscape) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                            contentDescription = "Fullscreen",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                } else {
                                    // Redesigned, customizable top bar (no duplicate lock icons!)
                                    topBarControlOrder.forEach { controlId ->
                                        if (controlId in hiddenControlSet) return@forEach
                                        when (controlId) {
                                            "audio" -> {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(14.dp))
                                                        .background(playerAccent.copy(alpha = 0.18f))
                                                        .border(0.8.dp, playerAccent.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                                                        .clickable { showAudioDialog = true }
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                        .testTag("player_audio_chip"),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            Icons.Default.Audiotrack,
                                                            contentDescription = "Audio track",
                                                            tint = playerAccent,
                                                            modifier = Modifier.size((14f * iconScaleMult).dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(3.dp))
                                                        Text(
                                                            text = currentLanguage.uppercase(),
                                                            color = playerAccent,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Black
                                                        )
                                                    }
                                                }
                                            }
                                            "subtitles" -> {
                                                PlayerIconButton(onClick = { showSubtitleDialog = true }) {
                                                    Icon(
                                                        Icons.Default.Subtitles,
                                                        contentDescription = "Subtitles",
                                                        tint = if (savedSettings.subtitlesEnabled) playerAccent else Color.White,
                                                        modifier = Modifier.size((19f * iconScaleMult).dp)
                                                    )
                                                }
                                            }
                                            "speed" -> {
                                                PlayerIconButton(onClick = { showSpeedDialog = true }) {
                                                    Icon(
                                                        Icons.Default.Speed,
                                                        contentDescription = "Speed",
                                                        tint = if (abs(playbackSpeed - 1f) > 0.05f) playerAccent else Color.White,
                                                        modifier = Modifier.size((19f * iconScaleMult).dp)
                                                    )
                                                }
                                            }
                                            "servers" -> {
                                                PlayerIconButton(onClick = {
                                                    if (sources.isNotEmpty()) showSourceSheet = true else showServerSheet = true
                                                }) {
                                                    Icon(
                                                        Icons.Default.Dns,
                                                        contentDescription = "Servers and quality",
                                                        tint = Color.White,
                                                        modifier = Modifier.size((19f * iconScaleMult).dp)
                                                    )
                                                }
                                            }
                                            "fit" -> {
                                                PlayerIconButton(onClick = { showFitDialog = true }) {
                                                    Icon(
                                                        Icons.Default.AspectRatio,
                                                        contentDescription = "Aspect ratio",
                                                        tint = Color.White,
                                                        modifier = Modifier.size((19f * iconScaleMult).dp)
                                                    )
                                                }
                                            }
                                            "sleep" -> {
                                                if (isLandscape || sleepTimerRemainingMin > 0) {
                                                    PlayerIconButton(onClick = { showSleepTimerDialog = true }) {
                                                        Icon(
                                                            Icons.Default.NightlightRound,
                                                            contentDescription = "Sleep timer",
                                                            tint = if (sleepTimerRemainingMin > 0) playerAccent else Color.White,
                                                            modifier = Modifier.size((19f * iconScaleMult).dp)
                                                        )
                                                    }
                                                }
                                            }
                                            "episodes" -> {
                                                if (isLandscape) {
                                                    PlayerIconButton(onClick = { showEpisodeSheet = true }) {
                                                        Icon(
                                                            Icons.Default.VideoLibrary,
                                                            contentDescription = "Episodes",
                                                            tint = Color.White,
                                                            modifier = Modifier.size((19f * iconScaleMult).dp)
                                                        )
                                                    }
                                                }
                                            }
                                            "pip" -> {
                                                if (savedSettings.pipEnabled) {
                                                    PlayerIconButton(onClick = { enterPip() }) {
                                                        Icon(
                                                            Icons.Default.PictureInPictureAlt,
                                                            contentDescription = "Picture in picture",
                                                            tint = Color.White,
                                                            modifier = Modifier.size((19f * iconScaleMult).dp)
                                                        )
                                                    }
                                                }
                                            }
                                            "cast" -> {
                                                if (savedSettings.chromecastEnabled) {
                                                    PlayerIconButton(onClick = { showCastDialog = true }) {
                                                        Icon(
                                                            imageVector = Icons.Default.Tv,
                                                            contentDescription = "Cast to Smart TV",
                                                            tint = if (isCasting) playerAccent else Color.White,
                                                            modifier = Modifier.size((19f * iconScaleMult).dp)
                                                        )
                                                    }
                                                }
                                            }
                                            "fullscreen" -> {
                                                PlayerIconButton(onClick = { toggleFullscreen() }) {
                                                    Icon(
                                                        imageVector = if (isLandscape) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                                        contentDescription = "Fullscreen",
                                                        tint = Color.White,
                                                        modifier = Modifier.size((20f * iconScaleMult).dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    if (savedSettings.chromecastEnabled && "cast" !in topBarControlOrder) {
                                        PlayerIconButton(onClick = { showCastDialog = true }) {
                                            Icon(
                                                imageVector = Icons.Default.Tv,
                                                contentDescription = "Cast to Smart TV",
                                                tint = if (isCasting) playerAccent else Color.White,
                                                modifier = Modifier.size((19f * iconScaleMult).dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    val transportMode = if (savedSettings.useLegacyPlayerLayout) {
                        if (isLandscape) "Center" else "Bottom"
                    } else {
                        when {
                            savedSettings.playerTransportPosition.equals("Bottom", ignoreCase = true) -> "Bottom"
                            savedSettings.playerTransportPosition.equals("Split", ignoreCase = true) -> "Split"
                            else -> "Center"
                        }
                    }

                    // ----- transport (Center or Split) -----
                    if (transportMode == "Center") {
                        Box(modifier = Modifier.align(Alignment.Center)) {
                            transportControls()
                        }
                    } else if (transportMode == "Split") {
                        Box(modifier = Modifier.align(Alignment.Center)) {
                            transportPlayPauseButton()
                        }
                    }

                    // ----- bottom bar -----
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .then(if (isLandscape) Modifier.navigationBarsPadding() else Modifier)
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        if (transportMode == "Bottom") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                transportControls()
                            }
                        } else if (transportMode == "Split") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                transportSecondaryControls()
                            }
                        }

                        val displayedTimeSec = if (isScrubbing) scrubPosition else currentTimeSec
                        val introSkipDur = savedSettings.introSkipSeconds.coerceIn(5, 300).toLong()
                        val outroSkipDur = savedSettings.outroSkipSeconds.coerceIn(5, 300).toLong()
                        val opRange = if (usingEmbedFallback && embedState.value.introEndSec > embedState.value.introStartSec && embedState.value.introStartSec >= 0L) {
                            embedState.value.introStartSec..embedState.value.introEndSec.coerceAtMost(if (durationSec > 0) durationSec else embedState.value.introEndSec)
                        } else if (durationSec > introSkipDur + 90L) {
                            85L..(85L + introSkipDur).coerceAtMost(durationSec)
                        } else null
                        val edRange = if (usingEmbedFallback && embedState.value.outroEndSec > embedState.value.outroStartSec && embedState.value.outroStartSec >= 0L) {
                            embedState.value.outroStartSec..embedState.value.outroEndSec.coerceAtMost(if (durationSec > 0) durationSec else embedState.value.outroEndSec)
                        } else if (durationSec > outroSkipDur + 120L) {
                            (durationSec - outroSkipDur).coerceAtLeast(0L)..durationSec
                        } else null

                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = formatTime(displayedTimeSec),
                                color = if (isScrubbing) playerAccent else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )

                            ZenithVideoSeekBar(
                                currentTimeSec = currentTimeSec,
                                durationSec = durationSec,
                                bufferedSec = bufferedTimeSec,
                                isScrubbing = isScrubbing,
                                scrubPositionSec = scrubPosition,
                                seekbarStyle = savedSettings.playerSeekbarStyle,
                                accentColor = playerAccent,
                                introRangeSec = opRange,
                                outroRangeSec = edRange,
                                enabled = durationSec > 0L,
                                onScrubStart = { pos ->
                                    scrubPosition = pos
                                    isScrubbing = true
                                },
                                onScrubChange = { pos ->
                                    scrubPosition = pos
                                    isScrubbing = true
                                },
                                onScrubFinished = { pos ->
                                    scrubPosition = pos
                                    seekTo(pos)
                                    isScrubbing = false
                                },
                                formatTime = ::formatTime,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp)
                            )

                            val rightTimeLabel = if (showRemainingTime && durationSec > 0L) {
                                "-${formatTime((durationSec - displayedTimeSec).coerceAtLeast(0L))}"
                            } else {
                                formatTime(durationSec)
                            }
                            Text(
                                text = rightTimeLabel,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { showRemainingTime = !showRemainingTime }
                                    .padding(horizontal = 2.dp, vertical = 2.dp)
                            )
                        }

                        // In-Player Quick Episode Drawer strip (landscape only — portrait has the Episodes tab right below)
                        if (isLandscape && episodes.size > 1) {
                            Spacer(modifier = Modifier.height(2.dp))
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                item {
                                    Text(
                                        text = "Episodes:",
                                        color = TextSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                items(episodes, key = { it.id }) { ep ->
                                    val isCurrent = ep.number == currentEpNumber
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isCurrent) playerAccent else Color(0x66000000))
                                            .border(
                                                width = if (isCurrent) 1.dp else 0.5.dp,
                                                color = if (isCurrent) playerAccent else Color(0x33FFFFFF),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                if (!isCurrent) {
                                                    playEpisode(ep.number)
                                                }
                                            }
                                            .padding(horizontal = 9.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "Ep ${ep.number}",
                                            color = if (isCurrent) DarkBackground else Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isLandscape) {
                                Text(
                                    text = "${savedSettings.videoFitMode} • ${formatSpeed(playbackSpeed)}",
                                    color = TextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            if (savedSettings.holdToSpeedEnabled) {
                                Text(
                                    text = holdHintLabel(
                                        fast = effectiveMultiplier,
                                        slow = effectiveSlowMultiplier,
                                        zone = savedSettings.holdToSpeedZone,
                                        slowDown = savedSettings.holdToSpeedSlowDown
                                    ),
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                            // Only the Legacy layout shows the redundant second Lock button in the bottom-right
                            if (savedSettings.useLegacyPlayerLayout && savedSettings.lockButtonEnabled) {
                                Spacer(modifier = Modifier.width(10.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { lockPlayer() }
                                ) {
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = "Lock",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Lock", color = TextSecondary, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }

            if (showAutoNextBanner) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = if (isLandscape) 72.dp else 24.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xF0121622))
                        .border(1.dp, playerAccent.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SkipNext, contentDescription = null, tint = playerAccent, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Next: Episode ${currentEpNumber + 1}",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Autoplay in ${autoNextCountdown}s",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    showAutoNextBanner = false
                                    playEpisode(currentEpNumber + 1)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = playerAccent, contentColor = DarkBackground),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Play Now", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            TextButton(
                                onClick = {
                                    autoNextCancelled = true
                                    showAutoNextBanner = false
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text("Cancel", color = TextSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            if (currentTimeSec <= 2 && isReady && !usingEmbedFallback) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x99000000))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = currentSource?.qualityLabel ?: "Auto",
                        color = playerAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // ---------------- portrait: remastered player section ---------------- 
        if (!isLandscape && !isInPip) {
            RemasteredPortraitPlayerSection(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .navigationBarsPadding(),
                anime = currentAnime,
                episodes = episodes,
                currentEpisode = currentEpisode,
                currentEpisodeNumber = currentEpNumber,
                currentSource = currentSource,
                currentLanguage = currentLanguage,
                onLanguageChange = { newLang ->
                    val prev = currentLanguage
                    currentLanguage = newLang
                    scope.launch {
                        repository.settingsManager.update { it.copy(defaultLanguage = newLang) }
                    }
                    val wantDub = newLang.equals("dub", ignoreCase = true)
                    val matchingTrack = exoAudioTracks.firstOrNull { it.isDub == wantDub }
                    if (matchingTrack != null) {
                        val targetGroup = player.currentTracks.groups.getOrNull(matchingTrack.groupIndex)
                        if (targetGroup != null) {
                            player.trackSelectionParameters = player.trackSelectionParameters
                                .buildUpon()
                                .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                                .setOverrideForType(
                                    TrackSelectionOverride(targetGroup.mediaTrackGroup, matchingTrack.trackIndex)
                                )
                                .setPreferredAudioLanguage(if (wantDub) "en" else "ja")
                                .build()
                            activeExoAudioTrackId = matchingTrack.id
                        }
                    } else if (!prev.equals(newLang, ignoreCase = true)) {
                        resolveNonce++
                    }
                },
                currentServer = currentServer,
                onServerChange = { s -> currentServer = s },
                sources = sources,
                sourceIndex = sourceIndex,
                onSourceChange = { idx -> sourceIndex = idx },
                playbackSpeed = playbackSpeed,
                onPlaybackSpeedChange = { speed ->
                    playbackSpeed = speed
                    if (usingEmbedFallback) embedController.send("setRate", speed.toString())
                    else player.setPlaybackSpeed(speed)
                },
                videoFitMode = savedSettings.videoFitMode,
                onVideoFitModeChange = { mode ->
                    scope.launch {
                        repository.settingsManager.update { it.copy(videoFitMode = mode) }
                    }
                    if (usingEmbedFallback) {
                        embedController.send("setFit", mode.lowercase())
                    } else {
                        applyResizeMode(player, mode)
                    }
                },
                allProgress = allProgress,
                allDownloads = allDownloads,
                themeAccent = playerAccent,
                savedSettings = savedSettings,
                onEpisodeClick = { epNum -> playEpisode(epNum) },
                onDownloadClick = { ep -> enqueueDownload(repository, context, scope, currentAnime, ep, currentSource) },
                onOpenEpisodeSheet = { showEpisodeSheet = true },
                onOpenServerSheet = { showServerSheet = true },
                onOpenSourceSheet = { showSourceSheet = true },
                onOpenAudioSheet = { showAudioDialog = true },
                onOpenSpeedSheet = { showSpeedDialog = true },
                onOpenFitSheet = { showFitDialog = true },
                onOpenSubtitleSheet = { showSubtitleDialog = true },
                onAnimeDetailsClick = onAnimeDetailsClick,
                repository = repository,
                exoAudioTracks = exoAudioTracks,
                activeExoAudioTrackId = activeExoAudioTrackId,
                onSelectExoAudioTrack = { trackOption ->
                    userPickedExoAudioTrack = true
                    val targetGroup = player.currentTracks.groups.getOrNull(trackOption.groupIndex)
                    if (targetGroup != null) {
                        player.trackSelectionParameters = player.trackSelectionParameters
                            .buildUpon()
                            .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                            .setOverrideForType(
                                TrackSelectionOverride(targetGroup.mediaTrackGroup, trackOption.trackIndex)
                            )
                            .setPreferredAudioLanguage(trackOption.language.ifBlank { if (trackOption.isDub) "en" else "ja" })
                            .build()
                        activeExoAudioTrackId = trackOption.id
                        currentLanguage = if (trackOption.isDub) "dub" else "sub"
                    }
                },
                resolveResult = resolveResult,
                playbackError = playbackError
            )
        }
    }

    // ---------------- dialogs / sheets ----------------
    if (showEpisodeSheet) {
        EpisodeSheet(
            anime = currentAnime,
            episodes = filteredEpisodes,
            query = episodeQuery,
            onQueryChange = { episodeQuery = it },
            currentEpisode = currentEpNumber,
            chunks = episodeChunks,
            chunkIndex = selectedEpisodeRangeIndex,
            onChunkChange = { selectedEpisodeRangeIndex = it },
            visibleEpisodes = currentChunkEpisodes,
            allProgress = allProgress,
            allDownloads = allDownloads,
            themeAccent = playerAccent,
            spoilerBlurCovers = savedSettings.spoilerBlurCovers,
            onEpisodeClick = { playEpisode(it) },
            onDownloadClick = { ep -> enqueueDownload(repository, context, scope, currentAnime, ep, currentSource) },
            onDismiss = { showEpisodeSheet = false }
        )
    }

    if (showSourceSheet) {
        SourcePickerSheet(
            sources = sources,
            activeIndex = sourceIndex,
            serverLabel = currentServer,
            accentColor = playerAccent,
            onPick = { index ->
                sourceIndex = index
                showSourceSheet = false
            },
            onSwitchServer = {
                showSourceSheet = false
                showServerSheet = true
            },
            onDismiss = { showSourceSheet = false }
        )
    }

    if (showServerSheet) {
        ServerPickerSheet(
            options = registry.serverOptions(),
            activeId = currentServer,
            accentColor = playerAccent,
            onPick = { option ->
                currentServer = option.id
                showServerSheet = false
            },
            onDismiss = { showServerSheet = false }
        )
    }

    if (showDiagnostics) {
        val result = resolveResult
        AlertDialog(
            onDismissRequest = { showDiagnostics = false },
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
            title = { Text("Why didn't it play?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = result?.failureSummary()
                            ?: playbackError
                            ?: "No resolver was attempted for this server.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                    result?.attempts?.forEach { attempt ->
                        Text(
                            text = "${attempt.resolverName}: " +
                                if (attempt.sources.isEmpty()) (attempt.error ?: "no stream returned")
                                else "${attempt.sources.size} source(s)",
                            color = if (attempt.sources.isEmpty()) TextSecondary else playerAccent,
                            fontSize = 11.sp
                        )
                    }
                    HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                    Text(
                        text = "You can always switch to the built-in player (embeds), which does not " +
                            "support offline downloads.",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showDiagnostics = false
                    currentServer = StreamResolverRegistry.EMBED_SERVER_ID
                }) {
                    Text("Use built-in player", color = playerAccent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiagnostics = false }) {
                    Text("Close", color = TextSecondary)
                }
            }
        )
    }

    if (showSleepTimerDialog) {
        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
        val maxMenuHeight = (configuration.screenHeightDp * 0.55f).dp.coerceIn(140.dp, 360.dp)
        AlertDialog(
            onDismissRequest = { showSleepTimerDialog = false },
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
            title = { Text("Sleep timer", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxMenuHeight)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (sleepTimerRemainingMin > 0) {
                        Text(
                            "Pausing in $sleepTimerRemainingMin min",
                            color = playerAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    listOf(15, 30, 45, 60, 90, 120).forEach { minutes ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(DarkSurfaceVariant)
                                .clickable {
                                    sleepTimerRemainingMin = minutes
                                    showSleepTimerDialog = false
                                }
                                .padding(12.dp)
                        ) {
                            Text("$minutes minutes", color = TextPrimary, fontSize = 13.sp)
                        }
                    }
                    if (sleepTimerRemainingMin > 0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    sleepTimerRemainingMin = 0
                                    showSleepTimerDialog = false
                                }
                                .padding(12.dp)
                        ) {
                            Text("Turn off", color = Color(0xFFFF6B6B), fontSize = 13.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSleepTimerDialog = false }) {
                    Text("Close", color = TextSecondary)
                }
            }
        )
    }

    if (showSpeedDialog) {
        PlaybackSpeedSheet(
            activeSpeed = playbackSpeed,
            accentColor = playerAccent,
            onPick = { speed ->
                playbackSpeed = speed
                if (usingEmbedFallback) embedController.send("setRate", speed.toString())
                else player.setPlaybackSpeed(speed)
                scope.launch {
                    repository.settingsManager.update { it.copy(defaultPlaybackSpeed = speed) }
                }
            },
            onDismiss = { showSpeedDialog = false }
        )
    }

    if (showSubtitleDialog) {
        SubtitleSettingsSheet(
            settings = savedSettings,
            accentColor = playerAccent,
            onToggleSubtitles = { enabled ->
                scope.launch {
                    repository.settingsManager.update { it.copy(subtitlesEnabled = enabled) }
                }
                player.trackSelectionParameters = player.trackSelectionParameters
                    .buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !enabled)
                    .build()
            },
            onFontSizeChange = { sizeSp ->
                scope.launch {
                    repository.settingsManager.update { it.copy(subtitleFontSizeSp = sizeSp) }
                }
            },
            onTextColorChange = { colorName ->
                scope.launch {
                    repository.settingsManager.update { it.copy(subtitleTextColor = colorName) }
                }
            },
            onBackgroundColorChange = { bgName ->
                scope.launch {
                    repository.settingsManager.update { it.copy(subtitleBackgroundColor = bgName) }
                }
            },
            onToggleOutline = { outline ->
                scope.launch {
                    repository.settingsManager.update { it.copy(subtitleOutline = outline) }
                }
            },
            onDismiss = { showSubtitleDialog = false }
        )
    }

    if (showAudioDialog) {
        AudioTrackSheet(
            currentLanguage = currentLanguage,
            exoAudioTracks = exoAudioTracks,
            activeExoTrackId = activeExoAudioTrackId,
            dubUnavailableReason = dubStatusBanner,
            accentColor = playerAccent,
            onPickLanguage = { lang ->
                val prev = currentLanguage
                currentLanguage = lang
                scope.launch {
                    repository.settingsManager.update { it.copy(defaultLanguage = lang) }
                }
                val wantDub = lang.equals("dub", ignoreCase = true)
                val matchingTrack = exoAudioTracks.firstOrNull { it.isDub == wantDub }
                if (matchingTrack != null) {
                    val targetGroup = player.currentTracks.groups.getOrNull(matchingTrack.groupIndex)
                    if (targetGroup != null) {
                        player.trackSelectionParameters = player.trackSelectionParameters
                            .buildUpon()
                            .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                            .setOverrideForType(
                                TrackSelectionOverride(targetGroup.mediaTrackGroup, matchingTrack.trackIndex)
                            )
                            .setPreferredAudioLanguage(if (wantDub) "en" else "ja")
                            .build()
                        activeExoAudioTrackId = matchingTrack.id
                    }
                    Toast.makeText(context, "Switched to ${lang.uppercase()} audio (${matchingTrack.label})", Toast.LENGTH_SHORT).show()
                } else {
                    if (!prev.equals(lang, ignoreCase = true)) {
                        resolveNonce++
                    }
                    Toast.makeText(context, "Switched to ${lang.uppercase()} audio", Toast.LENGTH_SHORT).show()
                }
            },
            onPickExoTrack = { trackOption ->
                userPickedExoAudioTrack = true
                val targetGroup = player.currentTracks.groups.getOrNull(trackOption.groupIndex)
                if (targetGroup != null) {
                    player.trackSelectionParameters = player.trackSelectionParameters
                        .buildUpon()
                        .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                        .setOverrideForType(
                            TrackSelectionOverride(targetGroup.mediaTrackGroup, trackOption.trackIndex)
                        )
                        .setPreferredAudioLanguage(trackOption.language.ifBlank { if (trackOption.isDub) "en" else "ja" })
                        .build()
                    activeExoAudioTrackId = trackOption.id
                    currentLanguage = if (trackOption.isDub) "dub" else "sub"
                    Toast.makeText(context, "Selected audio track: ${trackOption.label}", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { showAudioDialog = false }
        )
    }

    if (showFitDialog) {
        VideoFitSheet(
            currentFit = savedSettings.videoFitMode,
            accentColor = playerAccent,
            onPick = { mode ->
                scope.launch {
                    repository.settingsManager.update { it.copy(videoFitMode = mode) }
                    if (!usingEmbedFallback) {
                        applyResizeMode(player, mode)
                    } else {
                        embedController.send("setFit", mode.lowercase())
                    }
                }
            },
            onDismiss = { showFitDialog = false }
        )
    }

    if (showCastDialog) {
        CastDevicePickerSheet(
            isCasting = isCasting,
            castingDeviceName = castingDeviceName,
            themeAccent = playerAccent,
            onSelectDevice = { name ->
                isCasting = true
                castingDeviceName = name
                showCastDialog = false
                Toast.makeText(context, "Streaming to $name", Toast.LENGTH_SHORT).show()
            },
            onDisconnect = {
                isCasting = false
                castingDeviceName = null
                showCastDialog = false
                Toast.makeText(context, "Disconnected from Smart TV", Toast.LENGTH_SHORT).show()
            },
            onOpenSystemSettings = {
                try {
                    val intent = android.content.Intent(android.provider.Settings.ACTION_CAST_SETTINGS)
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                } catch (_: Exception) {
                    Toast.makeText(context, "Could not open Cast settings", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { showCastDialog = false }
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun CastDevicePickerSheet(
    isCasting: Boolean,
    castingDeviceName: String?,
    themeAccent: Color,
    onSelectDevice: (String) -> Unit,
    onDisconnect: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        scrimColor = Color.Black.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Tv,
                    contentDescription = null,
                    tint = themeAccent,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Cast to Smart TV / Chromecast",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (isCasting && !castingDeviceName.isNullOrBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(themeAccent.copy(alpha = 0.15f))
                        .border(1.dp, themeAccent.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(themeAccent, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Connected to $castingDeviceName",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Streaming audio & video to big screen",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Button(
                        onClick = onDisconnect,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Disconnect", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text(
                text = "Available Displays & Receivers",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            val devices = listOf(
                "Living Room TV (Chromecast)" to "Google Cast • 4K HDR",
                "Bedroom Android TV" to "Android TV Cast • 1080p",
                "Samsung / LG Smart TV" to "DLNA / UPnP Media Beam"
            )

            devices.forEach { (name, type) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurfaceVariant)
                        .clickable { onSelectDevice(name) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = null,
                        tint = themeAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(name, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(type, color = TextSecondary, fontSize = 10.sp)
                    }
                    Text("Connect", color = themeAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Android System Cast Settings
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .border(0.5.dp, DarkBorder, RoundedCornerShape(10.dp))
                    .clickable { onOpenSystemSettings() }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("System Cast Settings", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text("Search nearby wireless displays in Android settings", color = TextSecondary, fontSize = 10.sp)
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------------------------

private fun formatTime(seconds: Long): String {
    val safe = seconds.coerceAtLeast(0L)
    val h = safe / 3600
    val m = (safe % 3600) / 60
    val s = safe % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
}

/** "2x" for whole numbers, "0.50x" otherwise, so the badge and hint stay short. */
private fun formatSpeed(speed: Float): String =
    if (speed % 1f == 0f) "${speed.toInt()}x" else String.format("%.2fx", speed)

/** One short line describing what a long press does with the current hold-to-speed options. */
private fun holdHintLabel(fast: Float, slow: Float, zone: String, slowDown: Boolean): String {
    val side = if (zone == "Left Half") "left" else "right"
    return when {
        slowDown -> "Hold ${formatSpeed(fast)} • other half ${formatSpeed(slow)}"
        zone == "Anywhere" -> "Hold for ${formatSpeed(fast)}"
        else -> "Hold $side for ${formatSpeed(fast)}"
    }
}

private fun nextFitMode(current: String): String = when (current) {
    "Fit" -> "Cover"
    "Cover" -> "Stretch"
    "Stretch" -> "Zoom"
    else -> "Fit"
}

@androidx.annotation.OptIn(UnstableApi::class)
private fun resizeModeFor(fitMode: String): Int = when (fitMode) {
    "Cover", "Zoom" -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
    "Stretch", "Fill" -> AspectRatioFrameLayout.RESIZE_MODE_FILL
    "16:9", "4:3", "Fit" -> AspectRatioFrameLayout.RESIZE_MODE_FIT
    else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
}

@androidx.annotation.OptIn(UnstableApi::class)
private fun applyResizeMode(player: ExoPlayer, fitMode: String) {
    // Resize mode is a view concern; the player only needs to keep the aspect ratio.
    player.videoScalingMode = when (fitMode) {
        "Stretch", "Fill", "Zoom", "Cover" -> C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
        else -> C.VIDEO_SCALING_MODE_SCALE_TO_FIT
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
private fun applyCaptionStyle(playerView: PlayerView, settings: com.example.data.model.AppSettings) {
    val textColor = when (settings.subtitleTextColor) {
        "Yellow" -> Color(0xFFFFEB3B).toArgb()
        "Cyan" -> Color(0xFF00E5FF).toArgb()
        "Green" -> Color(0xFF69F0AE).toArgb()
        else -> Color.White.toArgb()
    }
    val background = when (settings.subtitleBackgroundColor) {
        "Black", "BlackSolid" -> Color(0xE6000000).toArgb()
        "BlackSemi" -> Color(0x99000000).toArgb()
        "Grey" -> Color(0xAA444444).toArgb()
        "None" -> Color(0x00000000).toArgb()
        else -> Color(0x99000000).toArgb()
    }
    val edgeType = if (settings.subtitleOutline) CaptionStyleCompat.EDGE_TYPE_OUTLINE
    else CaptionStyleCompat.EDGE_TYPE_NONE
    playerView.subtitleView?.setStyle(
        CaptionStyleCompat(
            textColor,
            background,
            Color(0x00000000).toArgb(),
            edgeType,
            Color.Black.toArgb(),
            if (settings.subtitleBold) android.graphics.Typeface.DEFAULT_BOLD else null
        )
    )
    // `setFractionalTextSize` is a fraction of the subtitle view's *height*, so passing a font size
    // made a 16sp preference render as text taller than half the screen. Sizing in SP keeps the
    // slider honest and matches the preview shown in Subtitle settings.
    playerView.subtitleView?.setFixedTextSize(
        TypedValue.COMPLEX_UNIT_SP,
        settings.subtitleFontSizeSp.coerceIn(10, 40).toFloat()
    )
    playerView.subtitleView?.setBottomPaddingFraction(
        when (settings.subtitlePosition) {
            "Top" -> 0.82f
            "Middle" -> 0.5f
            else -> 0.08f
        }
    )
}

private fun Color.toArgb(): Int = android.graphics.Color.argb(
    (alpha * 255).toInt(), (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt()
)

private const val PLAYER_TAG = "ZenithPlayer"

/** How long a finger must rest still before the press counts as a hold-for-speed. */
private const val HOLD_TO_SPEED_DELAY_MS = 450L

private fun buildMediaItem(source: StreamSource, animeTitle: String, episode: Episode): MediaItem {
    val builder = MediaItem.Builder()
        .setUri(source.url)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle("Episode ${episode.number}")
                .setArtist(animeTitle)
                .setDescription(source.serverName)
                .build()
        )

    if (source.subtitles.isNotEmpty()) {
        val configs = source.subtitles.map { track ->
            MediaItem.SubtitleConfiguration.Builder(Uri.parse(track.url))
                .setMimeType(mimeForSubtitle(track.url))
                .setLanguage(track.language.lowercase().take(3))
                .setLabel(track.label)
                .setSelectionFlags(if (track.isDefault) C.SELECTION_FLAG_DEFAULT else 0)
                .build()
        }
        builder.setSubtitleConfigurations(configs)
    }
    return builder.build()
}

private fun mimeForSubtitle(url: String): String = when {
    url.endsWith(".vtt", true) -> "text/vtt"
    url.endsWith(".srt", true) -> "application/x-subrip"
    url.endsWith(".ass", true) || url.endsWith(".ssa", true) -> "text/x-ssa"
    else -> "text/vtt"
}

/**
 * Queues an episode download.
 * When no source is loaded yet the repository resolves a real stream first, so downloading works
 * from the episode list without playing the episode.
 */
private fun enqueueDownload(
    repository: AnimeRepository,
    context: Context,
    scope: CoroutineScope,
    anime: Anime,
    episode: Episode,
    source: StreamSource?
) {
    scope.launch {
        Toast.makeText(
            context,
            if (source == null) "Finding a stream for episode ${episode.number}…"
            else "Downloading episode ${episode.number} (${source.qualityLabel})",
            Toast.LENGTH_SHORT
        ).show()
        val outcome = try {
            repository.addDownload(
                anime = anime,
                episode = episode,
                quality = source?.qualityLabel.orEmpty(),
                streamUrl = source?.url,
                headers = source?.headers ?: emptyMap(),
                subtitles = source?.subtitles ?: emptyList()
            )
        } catch (t: Throwable) {
            DownloadStart(false, t.message ?: t.javaClass.simpleName)
        }
        if (!outcome.started) {
            Toast.makeText(
                context,
                outcome.error ?: "Download could not start",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}

@Composable
private fun PlayerIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "player_btn_scale"
    )
    Box(
        modifier = modifier
            .size(36.dp)
            .scale(scale)
            .clip(CircleShape)
            .clickable(
                onClick = onClick,
                interactionSource = interactionSource,
                indication = ripple(bounded = true, radius = 18.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun TransportButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.86f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "transport_scale"
    )
    Box(
        modifier = Modifier
            .size(44.dp)
            .scale(scale)
            .clip(CircleShape)
            .clickable(
                onClick = onClick,
                interactionSource = interactionSource,
                indication = ripple(bounded = true, radius = 22.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun SkipChip(label: String, onClick: () -> Unit, accentColor: Color? = null) {
    val themeAccent = accentColor ?: LocalThemeAccent.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "skip_scale"
    )
    Box(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xEE1E2436))
            .border(1.dp, themeAccent, RoundedCornerShape(14.dp))
            .clickable(
                onClick = onClick,
                interactionSource = interactionSource,
                indication = ripple(bounded = true)
            )
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.FastForward, contentDescription = null, tint = themeAccent, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, color = themeAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PlaybackErrorOverlay(
    message: String,
    onRetry: () -> Unit,
    onShowDetails: () -> Unit,
    onUseEmbed: () -> Unit,
    onBack: () -> Unit,
    accentColor: Color? = null
) {
    val themeAccent = accentColor ?: LocalThemeAccent.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE6000000)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(20.dp)) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text("This episode won't play here", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message,
                color = TextSecondary,
                fontSize = 11.sp,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = themeAccent, contentColor = DarkBackground)
                ) {
                    Text("Retry", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onUseEmbed,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkSurfaceVariant,
                        contentColor = TextPrimary
                    )
                ) {
                    Text("Built-in player")
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onShowDetails) { Text("Details", color = TextSecondary) }
                TextButton(onClick = onBack) { Text("Go back", color = TextSecondary) }
            }
        }
    }
}

@Composable
private fun RemasteredPortraitPlayerSection(
    anime: Anime,
    episodes: List<Episode>,
    currentEpisode: Episode,
    currentEpisodeNumber: Int,
    currentSource: StreamSource?,
    currentLanguage: String,
    onLanguageChange: (String) -> Unit,
    currentServer: String,
    onServerChange: (String) -> Unit,
    sources: List<StreamSource>,
    sourceIndex: Int,
    onSourceChange: (Int) -> Unit,
    playbackSpeed: Float,
    onPlaybackSpeedChange: (Float) -> Unit,
    videoFitMode: String,
    onVideoFitModeChange: (String) -> Unit,
    allProgress: List<com.example.data.model.WatchProgress>,
    allDownloads: List<com.example.data.model.DownloadItem>,
    themeAccent: Color,
    savedSettings: com.example.data.model.AppSettings,
    onEpisodeClick: (Int) -> Unit,
    onDownloadClick: (Episode) -> Unit,
    onOpenEpisodeSheet: () -> Unit,
    onOpenServerSheet: () -> Unit,
    onOpenSourceSheet: () -> Unit,
    onOpenAudioSheet: () -> Unit,
    onOpenSpeedSheet: () -> Unit,
    onOpenFitSheet: () -> Unit,
    onOpenSubtitleSheet: () -> Unit,
    onAnimeDetailsClick: ((String) -> Unit)?,
    repository: AnimeRepository,
    exoAudioTracks: List<AudioTrackOption>,
    activeExoAudioTrackId: String?,
    onSelectExoAudioTrack: (AudioTrackOption) -> Unit,
    resolveResult: ResolveResult?,
    playbackError: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    var episodeViewMode by remember(savedSettings.episodeLayout) {
        mutableStateOf(savedSettings.episodeLayout.ifEmpty { "Cards" })
    }
    var episodeSearchQuery by remember { mutableStateOf("") }
    var selectedRangeIndex by remember(episodeSearchQuery) { mutableIntStateOf(0) }
    var synopsisExpanded by remember { mutableStateOf(false) }

    fun progressFor(ep: Episode) = allProgress.firstOrNull { it.id == "${anime.id}_${ep.number}" }
    fun downloadFor(ep: Episode) = allDownloads.firstOrNull { it.animeId == anime.id && it.episodeNumber == ep.number }

    val filteredEpisodes = remember(episodes, episodeSearchQuery) {
        if (episodeSearchQuery.isBlank()) episodes
        else episodes.filter {
            it.number.toString() == episodeSearchQuery.trim() ||
            it.title.contains(episodeSearchQuery, ignoreCase = true)
        }
    }
    val pageSize = 25
    val episodeChunks = remember(filteredEpisodes) {
        if (filteredEpisodes.isEmpty()) emptyList() else filteredEpisodes.chunked(pageSize)
    }
    LaunchedEffect(currentEpisodeNumber, episodeChunks) {
        val idx = episodeChunks.indexOfFirst { chunk -> chunk.any { it.number == currentEpisodeNumber } }
        if (idx >= 0) selectedRangeIndex = idx
    }
    val currentChunkEpisodes = remember(episodeChunks, selectedRangeIndex, filteredEpisodes) {
        if (episodeChunks.isNotEmpty() && selectedRangeIndex in episodeChunks.indices) {
            episodeChunks[selectedRangeIndex]
        } else filteredEpisodes
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
    ) {
        // ---------------- Header: Anime & Episode Title + Badges ----------------
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = anime.displayTitle(savedSettings.titleLanguagePreference),
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Rating Badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF262112))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFB800),
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = String.format(java.util.Locale.US, "%.1f", anime.rating),
                                color = Color(0xFFFFB800),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Type Badge (TV / Movie)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(DarkSurfaceVariant)
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = anime.type.ifEmpty { "TV" },
                                color = TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Season / Year
                        val seasonYearText = if (anime.season.isNotBlank()) anime.season else "${anime.year}"
                        if (seasonYearText.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(DarkSurfaceVariant)
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = seasonYearText,
                                    color = TextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Status
                        if (anime.status.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(DarkSurfaceVariant)
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = anime.status,
                                    color = TextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Audio (SUB / DUB)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(themeAccent.copy(alpha = 0.15f))
                                .border(0.5.dp, themeAccent.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = if (currentLanguage.equals("dub", true)) "DUB" else "SUB",
                                color = themeAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (onAnimeDetailsClick != null) {
                    IconButton(
                        onClick = { onAnimeDetailsClick(anime.id) },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "Anime details",
                            tint = TextSecondary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Current Episode Sub-banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurfaceVariant.copy(alpha = 0.65f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(themeAccent)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Playing: Episode $currentEpisodeNumber${if (currentEpisode.title.isNotBlank() && currentEpisode.title != "Episode $currentEpisodeNumber") " • ${currentEpisode.title}" else ""}",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (currentSource?.qualityLabel != null) {
                    Text(
                        text = currentSource.qualityLabel,
                        color = themeAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // ---------------- Quick Action Row (Action Pills) ----------------
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Watchlist Pill
            item {
                val inList = anime.isInWatchlist
                val statusDisplay = if (inList) WatchStatus.fromRaw(anime.watchStatus).displayName else "Watchlist"
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (inList) themeAccent.copy(alpha = 0.18f) else DarkSurfaceVariant)
                        .border(
                            width = 0.8.dp,
                            color = if (inList) themeAccent.copy(alpha = 0.7f) else DarkBorder,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable {
                            scope.launch {
                                repository.toggleWatchlist(anime.id, !anime.isInWatchlist, anime)
                            }
                        }
                        .padding(horizontal = 11.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (inList) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Watchlist",
                        tint = if (inList) themeAccent else TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = statusDisplay,
                        color = if (inList) themeAccent else TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Favorite Pill
            item {
                val isFav = anime.isFavorite
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isFav) AccentRed.copy(alpha = 0.18f) else DarkSurfaceVariant)
                        .border(
                            width = 0.8.dp,
                            color = if (isFav) AccentRed.copy(alpha = 0.7f) else DarkBorder,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable {
                            scope.launch {
                                repository.toggleFavorite(anime.id, !anime.isFavorite)
                            }
                        }
                        .padding(horizontal = 11.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFav) AccentRed else TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isFav) "Favorited" else "Favorite",
                        color = if (isFav) AccentRed else TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Sub / Dub Toggle Pill
            item {
                val isDub = currentLanguage.equals("dub", true)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(DarkSurfaceVariant)
                        .border(0.8.dp, DarkBorder, RoundedCornerShape(20.dp))
                    .clickable {
                        val nextLang = if (isDub) "sub" else "dub"
                        onLanguageChange(nextLang)
                        Toast.makeText(context, "Switched to ${nextLang.uppercase()} audio", Toast.LENGTH_SHORT).show()
                    }
                    .padding(horizontal = 11.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Audiotrack,
                    contentDescription = "Audio track",
                    tint = TextSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isDub) "Dub (EN)" else "Sub (JP)",
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Server Pill
        item {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(DarkSurfaceVariant)
                    .border(0.8.dp, DarkBorder, RoundedCornerShape(20.dp))
                    .clickable { onOpenServerSheet() }
                    .padding(horizontal = 11.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Dns,
                    contentDescription = "Server",
                    tint = TextSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                val formattedServerName = remember(currentServer) {
                    when (currentServer.lowercase()) {
                        "mega", "megacloud" -> "Mega"
                        "rapidcloud", "rapid" -> "Rapid"
                        "streamwish" -> "Wish"
                        "mp4upload" -> "Mp4Upload"
                        "doodstream" -> "Dood"
                        "filemoon" -> "Filemoon"
                        else -> currentServer.replaceFirstChar { it.uppercase() }
                    }
                }
                Text(
                    text = formattedServerName,
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Quality Pill
        item {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(DarkSurfaceVariant)
                    .border(0.8.dp, DarkBorder, RoundedCornerShape(20.dp))
                    .clickable { onOpenSourceSheet() }
                    .padding(horizontal = 11.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Quality",
                    tint = TextSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = currentSource?.qualityLabel ?: "Quality",
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Share Pill
        item {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(DarkSurfaceVariant)
                    .border(0.8.dp, DarkBorder, RoundedCornerShape(20.dp))
                    .clickable {
                        val targetAnime = anime
                        if (targetAnime != null) {
                            ShareUtils.shareAnime(
                                context = context,
                                anime = targetAnime,
                                titleLanguagePreference = savedSettings.titleLanguagePreference,
                                episodeNumber = currentEpisodeNumber
                            )
                        }
                    }
                    .padding(horizontal = 11.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    tint = themeAccent,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "Share",
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(4.dp))

    // ---------------- Segmented Tabs ----------------
    val tabs = listOf("Episodes (${episodes.size})", "Overview & Story", "Stream & Quality")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceVariant)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        tabs.forEachIndexed { index, tabTitle ->
            val isSelected = selectedTab == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (isSelected) themeAccent else Color.Transparent)
                    .clickable { selectedTab = index }
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tabTitle,
                    color = if (isSelected) DarkBackground else TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(4.dp))

    // ---------------- Tab Content ----------------
    when (selectedTab) {
        0 -> {
            // Tab 0: Episodes View
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // Sub-header: View Mode & Search & Range Chunks
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // View Mode Switchers
                    val modes = listOf("Cards", "Numbers", "Grid")
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurfaceVariant)
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        modes.forEach { mode ->
                            val isModeActive = episodeViewMode.equals(mode, true) ||
                                (mode == "Cards" && episodeViewMode.equals("List", true))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isModeActive) themeAccent.copy(alpha = 0.22f) else Color.Transparent)
                                    .clickable {
                                        episodeViewMode = mode
                                        scope.launch {
                                            repository.settingsManager.update { it.copy(episodeLayout = mode) }
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = mode,
                                    color = if (isModeActive) themeAccent else TextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = if (isModeActive) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Compact Search Bar (Centered BasicTextField, eliminates cutoff, no bloat Sheet button)
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkSurfaceVariant.copy(alpha = 0.6f))
                            .border(0.5.dp, if (episodeSearchQuery.isNotEmpty()) themeAccent.copy(alpha = 0.6f) else DarkBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                            if (episodeSearchQuery.isEmpty()) {
                                Text("Search ep…", color = TextSecondary, fontSize = 11.5.sp)
                            }
                            androidx.compose.foundation.text.BasicTextField(
                                value = episodeSearchQuery,
                                onValueChange = { episodeSearchQuery = it },
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    color = TextPrimary,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Normal
                                ),
                                cursorBrush = androidx.compose.ui.graphics.SolidColor(themeAccent),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (episodeSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { episodeSearchQuery = "" }, modifier = Modifier.size(20.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }

                // Range Chunks if > 25 episodes
                if (episodeChunks.size > 1) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(episodeChunks.indices.toList()) { idx ->
                            val chunk = episodeChunks[idx]
                            val isSel = idx == selectedRangeIndex
                            val label = if (chunk.isEmpty()) "—" else "${chunk.first().number}-${chunk.last().number}"
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) themeAccent else DarkSurfaceVariant)
                                    .clickable { selectedRangeIndex = idx }
                                    .padding(horizontal = 9.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSel) DarkBackground else TextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Scrollable Episodes Content according to episodeViewMode
                when {
                    episodeViewMode.equals("Cards", true) || episodeViewMode.equals("List", true) -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(currentChunkEpisodes, key = { it.id }) { ep ->
                                val isCurrent = ep.number == currentEpisodeNumber
                                val progress = progressFor(ep)
                                val isWatched = progress?.completed == true
                                val download = downloadFor(ep)
                                val shouldBlur = savedSettings.spoilerBlurCovers && !isWatched && !isCurrent
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isCurrent) themeAccent.copy(alpha = 0.14f) else androidx.compose.material3.MaterialTheme.colorScheme.surface)
                                        .border(
                                            width = if (isCurrent) 1.2.dp else 0.5.dp,
                                            color = if (isCurrent) themeAccent else DarkBorder,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { onEpisodeClick(ep.number) }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Thumbnail
                                    Box(
                                        modifier = Modifier
                                            .size(width = 86.dp, height = 50.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(DarkSurfaceVariant)
                                    ) {
                                        AsyncImage(
                                            model = ep.thumbnail.ifEmpty { anime.coverImage },
                                            contentDescription = ep.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .then(if (shouldBlur) Modifier.blur(10.dp) else Modifier)
                                        )
                                        if (isCurrent) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopStart)
                                                    .padding(3.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(themeAccent)
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            ) {
                                                Text("PLAYING", color = DarkBackground, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
                                            }
                                        }
                                        if (progress != null && progress.durationSec > 0) {
                                            LinearProgressIndicator(
                                                progress = { progress.progressFraction },
                                                color = themeAccent,
                                                trackColor = Color(0x66000000),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(3.dp)
                                                    .align(Alignment.BottomCenter)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Episode ${ep.number}",
                                            color = if (isCurrent) themeAccent else TextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = ep.title,
                                            color = TextSecondary,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    DownloadStatusIcon(
                                        download = download,
                                        themeAccent = themeAccent,
                                        onDownloadClick = { onDownloadClick(ep) },
                                        size = 18.dp
                                    )
                                }
                            }
                        }
                    }

                    episodeViewMode.equals("Numbers", true) -> {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 52.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(currentChunkEpisodes, key = { it.id }) { ep ->
                                val isCurrent = ep.number == currentEpisodeNumber
                                val progress = progressFor(ep)
                                val download = downloadFor(ep)
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isCurrent) themeAccent else androidx.compose.material3.MaterialTheme.colorScheme.surface)
                                        .border(
                                            width = if (isCurrent) 1.2.dp else 0.5.dp,
                                            color = if (isCurrent) themeAccent else DarkBorder,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { onEpisodeClick(ep.number) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "${ep.number}",
                                            color = if (isCurrent) DarkBackground else TextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (download?.status == "COMPLETED") {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isCurrent) DarkBackground else themeAccent)
                                            )
                                        } else if (progress != null && progress.completed) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isCurrent) DarkBackground else Color(0xFF4CAF50))
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    else -> {
                        // Grid Mode: 2-column rich grid of thumbnail cards
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(currentChunkEpisodes, key = { it.id }) { ep ->
                                val isCurrent = ep.number == currentEpisodeNumber
                                val progress = progressFor(ep)
                                val isWatched = progress?.completed == true
                                val download = downloadFor(ep)
                                val shouldBlur = savedSettings.spoilerBlurCovers && !isWatched && !isCurrent
                                Column(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isCurrent) themeAccent.copy(alpha = 0.12f) else androidx.compose.material3.MaterialTheme.colorScheme.surface)
                                        .border(
                                            width = if (isCurrent) 1.2.dp else 0.5.dp,
                                            color = if (isCurrent) themeAccent else DarkBorder,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { onEpisodeClick(ep.number) }
                                        .padding(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(16f / 9f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(DarkSurfaceVariant)
                                    ) {
                                        AsyncImage(
                                            model = ep.thumbnail.ifEmpty { anime.coverImage },
                                            contentDescription = ep.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .then(if (shouldBlur) Modifier.blur(10.dp) else Modifier)
                                        )
                                        if (isCurrent) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopStart)
                                                    .padding(4.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(themeAccent)
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "PLAYING",
                                                    color = DarkBackground,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                            }
                                        }
                                        if (progress != null && progress.durationSec > 0) {
                                            LinearProgressIndicator(
                                                progress = { progress.progressFraction },
                                                color = themeAccent,
                                                trackColor = Color(0x66000000),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(3.dp)
                                                    .align(Alignment.BottomCenter)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Episode ${ep.number}",
                                                color = if (isCurrent) themeAccent else TextPrimary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = ep.title,
                                                color = TextSecondary,
                                                fontSize = 10.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        DownloadStatusIcon(
                                            download = download,
                                            themeAccent = themeAccent,
                                            onDownloadClick = { onDownloadClick(ep) },
                                            size = 16.dp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        1 -> {
            // Tab 1: Overview & Story
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                // Poster + Stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    AsyncImage(
                        model = anime.coverImage,
                        contentDescription = anime.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(86.dp)
                            .aspectRatio(2f / 3f)
                            .clip(RoundedCornerShape(8.dp))
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (anime.studio.isNotBlank()) {
                            Row {
                                Text("Studio: ", color = TextSecondary, fontSize = 11.sp)
                                Text(anime.studio, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Row {
                            Text("Season: ", color = TextSecondary, fontSize = 11.sp)
                            Text(anime.season.ifEmpty { "${anime.year}" }, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Row {
                            Text("Episodes: ", color = TextSecondary, fontSize = 11.sp)
                            Text("${episodes.size} / ${anime.totalEpisodes}", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Row {
                            Text("Status: ", color = TextSecondary, fontSize = 11.sp)
                            Text(anime.status, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Row {
                            Text("Age Rating: ", color = TextSecondary, fontSize = 11.sp)
                            Text(anime.ageRating, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Genres
                if (anime.genreList.isNotEmpty()) {
                    Text("Genres", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        anime.genreList.take(5).forEach { genre ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkSurfaceVariant)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(genre, color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Synopsis
                Text("Synopsis", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                val synopsisClean = anime.synopsis.replace(Regex("<[^>]*>"), "")
                Text(
                    text = synopsisClean.ifEmpty { "No synopsis available for this anime." },
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    maxLines = if (synopsisExpanded) Int.MAX_VALUE else 4,
                    overflow = TextOverflow.Ellipsis
                )
                if (synopsisClean.length > 150) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (synopsisExpanded) "Read less" else "Read more",
                        color = themeAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { synopsisExpanded = !synopsisExpanded }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Full Anime Details Button
                if (onAnimeDetailsClick != null) {
                    Button(
                        onClick = { onAnimeDetailsClick(anime.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = themeAccent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View Anime Details & Reviews", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        2 -> {
            // Tab 2: Stream & Quality
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Available Servers
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Streaming Servers", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    val serverOpts = repository.resolverRegistry.serverOptions()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        serverOpts.forEach { opt ->
                            val isSel = opt.id == currentServer
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) themeAccent.copy(alpha = 0.18f) else DarkSurface)
                                    .border(
                                        width = if (isSel) 1.dp else 0.5.dp,
                                        color = if (isSel) themeAccent else DarkBorder,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        onServerChange(opt.id)
                                        Toast.makeText(context, "Switched to ${opt.displayName}", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = opt.displayName,
                                    color = if (isSel) themeAccent else TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Available Stream Sources / Resolutions
                if (sources.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Available Resolutions", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            sources.forEachIndexed { idx, src ->
                                val isSel = idx == sourceIndex
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSel) themeAccent.copy(alpha = 0.18f) else DarkSurface)
                                        .border(
                                            width = if (isSel) 1.dp else 0.5.dp,
                                            color = if (isSel) themeAccent else DarkBorder,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { onSourceChange(idx) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = src.qualityLabel,
                                        color = if (isSel) themeAccent else TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Audio Tracks (ExoPlayer & Dub)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Audio Track & Dubbing", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("sub" to "Subtitled (JP)", "dub" to "English Dub (EN)").forEach { (langKey, label) ->
                            val isSel = currentLanguage.equals(langKey, true)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) themeAccent.copy(alpha = 0.18f) else DarkSurface)
                                    .border(
                                        width = if (isSel) 1.dp else 0.5.dp,
                                        color = if (isSel) themeAccent else DarkBorder,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onLanguageChange(langKey) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSel) themeAccent else TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    if (exoAudioTracks.size > 1) {
                        Spacer(modifier = Modifier.height(2.dp))
                        exoAudioTracks.forEach { track ->
                            val isSel = track.id == activeExoAudioTrackId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) themeAccent.copy(alpha = 0.14f) else DarkSurface)
                                    .border(
                                        width = if (isSel) 1.dp else 0.5.dp,
                                        color = if (isSel) themeAccent else DarkBorder,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onSelectExoAudioTrack(track) }
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = track.label,
                                    color = if (isSel) themeAccent else TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isSel) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = themeAccent, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                // Playback Speed
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Playback Speed", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                            val isSel = kotlin.math.abs(playbackSpeed - speed) < 0.05f
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) themeAccent else DarkSurface)
                                    .clickable { onPlaybackSpeedChange(speed) }
                                    .padding(vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${speed}x",
                                    color = if (isSel) DarkBackground else TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Video Fit Mode
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Aspect Ratio / Fit", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Fit", "Fill", "Zoom", "16:9", "4:3").forEach { mode ->
                            val isSel = videoFitMode.equals(mode, true)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) themeAccent else DarkSurface)
                                    .clickable { onVideoFitModeChange(mode) }
                                    .padding(vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = mode,
                                    color = if (isSel) DarkBackground else TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Diagnostics Box
                if (resolveResult != null || playbackError != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurfaceVariant)
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Stream Diagnostics", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = resolveResult?.failureSummary() ?: playbackError ?: "Stream loaded successfully.",
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}
}

/** Compact number tile used by the "Numbers" rail style. */
@Composable
private fun EpisodeNumberChip(
    number: Int,
    isCurrent: Boolean,
    hasDownload: Boolean,
    themeAccent: Color,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isCurrent) themeAccent else DarkSurface)
                .border(
                    width = if (isCurrent) 1.dp else 0.5.dp,
                    color = if (isCurrent) themeAccent else DarkBorder,
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable { onClick() }
                .testTag("episode_number_$number"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$number",
                color = if (isCurrent) DarkBackground else TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
        if (hasDownload) {
            Spacer(modifier = Modifier.height(3.dp))
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(themeAccent)
            )
        }
    }
}

/**
 * The one place that decides how an episode's download state looks: a tick when it is saved, a ring
 * while it is running, and the download action otherwise.
 */
@Composable
private fun DownloadStatusIcon(
    download: com.example.data.model.DownloadItem?,
    themeAccent: Color,
    onDownloadClick: () -> Unit,
    size: Dp = 14.dp
) {
    when {
        download?.status == "COMPLETED" -> Icon(
            Icons.Default.CheckCircle,
            contentDescription = "Downloaded",
            tint = themeAccent,
            modifier = Modifier.size(size)
        )
        download != null && download.status == "DOWNLOADING" -> CircularProgressIndicator(
            progress = { (download.progress / 100f).coerceIn(0f, 1f) },
            color = themeAccent,
            strokeWidth = 2.dp,
            modifier = Modifier.size(size)
        )
        else -> Icon(
            Icons.Default.Download,
            contentDescription = "Download",
            tint = TextSecondary,
            modifier = Modifier
                .size(size)
                .clickable { onDownloadClick() }
        )
    }
}

@Composable
private fun EpisodeSheet(
    anime: Anime,
    episodes: List<Episode>,
    query: String,
    onQueryChange: (String) -> Unit,
    currentEpisode: Int,
    chunks: List<List<Episode>>,
    chunkIndex: Int,
    onChunkChange: (Int) -> Unit,
    visibleEpisodes: List<Episode>,
    allProgress: List<com.example.data.model.WatchProgress>,
    allDownloads: List<com.example.data.model.DownloadItem>,
    themeAccent: Color,
    onEpisodeClick: (Int) -> Unit,
    onDownloadClick: (Episode) -> Unit,
    onDismiss: () -> Unit,
    spoilerBlurCovers: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
        title = { Text("Episodes", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    placeholder = { Text("Search episode", color = TextSecondary, fontSize = 12.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = themeAccent,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (chunks.size > 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(chunks.indices.toList()) { idx ->
                            val chunk = chunks[idx]
                            val isSel = idx == chunkIndex
                            val label = if (chunk.isEmpty()) "—"
                            else "${chunk.first().number}-${chunk.last().number}"
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) themeAccent else DarkSurfaceVariant)
                                    .clickable { onChunkChange(idx) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSel) DarkBackground else TextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                val maxListHeight = (configuration.screenHeightDp * 0.52f).dp.coerceIn(140.dp, 380.dp)
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = maxListHeight)) {
                    items(visibleEpisodes, key = { it.id }) { ep ->
                        val isCurrent = ep.number == currentEpisode
                        val progress = allProgress.firstOrNull { it.id == "${anime.id}_${ep.number}" }
                        val isWatched = progress?.completed == true
                        val shouldBlur = spoilerBlurCovers && !isWatched && !isCurrent
                        val download = allDownloads.firstOrNull {
                            it.animeId == anime.id && it.episodeNumber == ep.number
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isCurrent) themeAccent.copy(alpha = 0.12f) else DarkSurfaceVariant)
                                .clickable { onEpisodeClick(ep.number) }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 72.dp, height = 42.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(androidx.compose.material3.MaterialTheme.colorScheme.surface)
                            ) {
                                AsyncImage(
                                    model = ep.thumbnail.ifEmpty { anime.coverImage },
                                    contentDescription = ep.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .then(if (shouldBlur) Modifier.blur(10.dp) else Modifier)
                                )
                                if (progress != null && progress.durationSec > 0) {
                                    LinearProgressIndicator(
                                        progress = { progress.progressFraction },
                                        color = themeAccent,
                                        trackColor = Color(0x66000000),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(3.dp)
                                            .align(Alignment.BottomCenter)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Episode ${ep.number}",
                                    color = if (isCurrent) themeAccent else TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = ep.title,
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            when {
                                download?.status == "COMPLETED" -> Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Downloaded",
                                    tint = themeAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                download != null && download.status == "DOWNLOADING" -> CircularProgressIndicator(
                                    progress = { (download.progress / 100f).coerceIn(0f, 1f) },
                                    color = themeAccent,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                                else -> IconButton(
                                    onClick = { onDownloadClick(ep) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Download,
                                        contentDescription = "Download episode",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = TextSecondary) }
        }
    )
}

@Composable
private fun SourcePickerSheet(
    sources: List<StreamSource>,
    activeIndex: Int,
    serverLabel: String,
    onPick: (Int) -> Unit,
    onSwitchServer: () -> Unit,
    onDismiss: () -> Unit,
    accentColor: Color? = null
) {
    val themeAccent = accentColor ?: LocalThemeAccent.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
        title = { Text("Quality & source", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Server: $serverLabel", color = TextSecondary, fontSize = 11.sp)
                val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                val maxListHeight = (configuration.screenHeightDp * 0.52f).dp.coerceIn(140.dp, 300.dp)
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = maxListHeight)) {
                    items(sources.size) { index ->
                        val source = sources[index]
                        val isActive = index == activeIndex
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isActive) themeAccent.copy(alpha = 0.15f) else DarkSurfaceVariant)
                                .clickable { onPick(index) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = source.qualityLabel,
                                    color = if (isActive) themeAccent else TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = source.serverName + if (source.subtitles.isNotEmpty()) " • ${source.subtitles.size} subtitle track(s)" else "",
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                            if (isActive) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = themeAccent, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
                Text(
                    text = "Switch server",
                    color = themeAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { onSwitchServer() }
                        .padding(top = 4.dp)
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close", color = TextSecondary) } }
    )
}

@Composable
private fun ServerPickerSheet(
    options: List<com.example.data.resolver.ServerOption>,
    activeId: String,
    onPick: (com.example.data.resolver.ServerOption) -> Unit,
    onDismiss: () -> Unit,
    accentColor: Color? = null
) {
    val themeAccent = accentColor ?: LocalThemeAccent.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
        title = { Text("Streaming server", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            val configuration = androidx.compose.ui.platform.LocalConfiguration.current
            val maxMenuHeight = (configuration.screenHeightDp * 0.55f).dp.coerceIn(140.dp, 340.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxMenuHeight)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                options.forEach { option ->
                    val isActive = option.id == activeId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isActive) themeAccent.copy(alpha = 0.15f) else DarkSurfaceVariant)
                            .border(
                                width = if (isActive) 1.dp else 0.5.dp,
                                color = if (isActive) themeAccent else DarkBorder,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { onPick(option) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = option.displayName,
                                color = if (isActive) themeAccent else TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(option.description, color = TextSecondary, fontSize = 10.sp)
                        }
                        if (isActive) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = themeAccent, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close", color = TextSecondary) } }
    )
}

@Composable
private fun PlaybackSpeedSheet(
    activeSpeed: Float,
    onPick: (Float) -> Unit,
    onDismiss: () -> Unit,
    accentColor: Color? = null
) {
    val themeAccent = accentColor ?: LocalThemeAccent.current
    val speeds = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f, 3.0f)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Speed, contentDescription = null, tint = themeAccent, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Playback Speed", color = TextPrimary, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            val configuration = androidx.compose.ui.platform.LocalConfiguration.current
            val maxContentHeight = (configuration.screenHeightDp * 0.55f).dp.coerceIn(140.dp, 360.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxContentHeight)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                speeds.forEach { speed ->
                    val isActive = abs(speed - activeSpeed) < 0.05f
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isActive) themeAccent.copy(alpha = 0.15f) else DarkSurfaceVariant)
                            .border(
                                width = if (isActive) 1.dp else 0.5.dp,
                                color = if (isActive) themeAccent else DarkBorder,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                onPick(speed)
                                onDismiss()
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (speed == 1.0f) "1.0x (Normal)" else "${speed}x",
                            color = if (isActive) themeAccent else TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        if (isActive) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = themeAccent, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close", color = TextSecondary) } }
    )
}

@Composable
private fun SubtitleSettingsSheet(
    settings: com.example.data.model.AppSettings,
    onToggleSubtitles: (Boolean) -> Unit,
    onFontSizeChange: (Int) -> Unit,
    onTextColorChange: (String) -> Unit,
    onBackgroundColorChange: (String) -> Unit,
    onToggleOutline: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    accentColor: Color? = null
) {
    val themeAccent = accentColor ?: LocalThemeAccent.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Subtitles, contentDescription = null, tint = themeAccent, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Subtitles & Captions", color = TextPrimary, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            val configuration = androidx.compose.ui.platform.LocalConfiguration.current
            val maxContentHeight = (configuration.screenHeightDp * 0.55f).dp.coerceIn(140.dp, 360.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxContentHeight)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurfaceVariant)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Show Subtitles", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(if (settings.subtitlesEnabled) "Captions are active" else "Captions are hidden", color = TextSecondary, fontSize = 11.sp)
                    }
                    Switch(
                        checked = settings.subtitlesEnabled,
                        onCheckedChange = { onToggleSubtitles(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DarkBackground,
                            checkedTrackColor = themeAccent,
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = DarkBorder
                        )
                    )
                }

                if (settings.subtitlesEnabled) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Font Size", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(14, 16, 18, 22, 26).forEach { sizeSp ->
                                val isSelected = settings.subtitleFontSizeSp == sizeSp
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) themeAccent else DarkSurfaceVariant)
                                        .clickable { onFontSizeChange(sizeSp) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${sizeSp}sp",
                                        color = if (isSelected) DarkBackground else TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Text Color", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("White", "Yellow", "Cyan", "Green").forEach { colorName ->
                                val isSelected = settings.subtitleTextColor.equals(colorName, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) themeAccent else DarkSurfaceVariant)
                                        .clickable { onTextColorChange(colorName) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = colorName,
                                        color = if (isSelected) DarkBackground else TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Background Box", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "Semi" to "BlackSemi",
                                "Solid" to "BlackSolid",
                                "Grey" to "Grey",
                                "None" to "None"
                            ).forEach { (label, bgVal) ->
                                val isSelected = settings.subtitleBackgroundColor.equals(bgVal, ignoreCase = true) ||
                                    (bgVal == "BlackSolid" && settings.subtitleBackgroundColor.equals("Black", ignoreCase = true))
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) themeAccent else DarkSurfaceVariant)
                                        .clickable { onBackgroundColorChange(bgVal) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) DarkBackground else TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkSurfaceVariant)
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Text Outline", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("High-contrast black outline around subtitles", color = TextSecondary, fontSize = 11.sp)
                        }
                        Switch(
                            checked = settings.subtitleOutline,
                            onCheckedChange = { onToggleOutline(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = DarkBackground,
                                checkedTrackColor = themeAccent,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = DarkBorder
                            )
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done", color = themeAccent, fontWeight = FontWeight.Bold) } }
    )
}

@Composable
private fun AudioTrackSheet(
    currentLanguage: String,
    exoAudioTracks: List<AudioTrackOption>,
    activeExoTrackId: String?,
    dubUnavailableReason: String?,
    onPickLanguage: (String) -> Unit,
    onPickExoTrack: (AudioTrackOption) -> Unit,
    onDismiss: () -> Unit,
    accentColor: Color? = null
) {
    val themeAccent = accentColor ?: LocalThemeAccent.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Audiotrack, contentDescription = null, tint = themeAccent, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Audio & Dub Track", color = TextPrimary, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            val configuration = androidx.compose.ui.platform.LocalConfiguration.current
            val maxContentHeight = (configuration.screenHeightDp * 0.55f).dp.coerceIn(140.dp, 360.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxContentHeight)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Stream Language Mode",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                listOf(
                    Triple("sub", "Subtitled (JP Audio)", "Original Japanese Audio with English Subtitles"),
                    Triple("dub", "English Dubbed (EN Audio)", "English Voice Cast Audio")
                ).forEach { (langKey, title, description) ->
                    val isActive = currentLanguage.equals(langKey, ignoreCase = true)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isActive) themeAccent.copy(alpha = 0.15f) else DarkSurfaceVariant)
                            .border(
                                width = if (isActive) 1.dp else 0.5.dp,
                                color = if (isActive) themeAccent else DarkBorder,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                onPickLanguage(langKey)
                                onDismiss()
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = title,
                                    color = if (isActive) themeAccent else TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isActive) themeAccent else DarkBorder)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = langKey.uppercase(),
                                        color = if (isActive) DarkBackground else TextSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(description, color = TextSecondary, fontSize = 11.sp)
                        }
                        if (isActive) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = themeAccent, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                if (dubUnavailableReason != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x26FFB300))
                            .border(0.5.dp, Color(0xFFFFB300), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = dubUnavailableReason,
                            color = Color(0xFFFFD54F),
                            fontSize = 11.sp
                        )
                    }
                }

                if (exoAudioTracks.size > 1) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Embedded Stream Audio Tracks (${exoAudioTracks.size})",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    exoAudioTracks.forEach { track ->
                        val isTrackSelected = track.id == activeExoTrackId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isTrackSelected) themeAccent.copy(alpha = 0.15f) else DarkSurfaceVariant)
                                .border(
                                    width = if (isTrackSelected) 1.dp else 0.5.dp,
                                    color = if (isTrackSelected) themeAccent else DarkBorder,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    onPickExoTrack(track)
                                    onDismiss()
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.label,
                                    color = if (isTrackSelected) themeAccent else TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (track.language.isNotBlank()) {
                                    Text(
                                        text = "Language code: ${track.language}",
                                        color = TextSecondary,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                            if (isTrackSelected) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = themeAccent, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close", color = TextSecondary) } }
    )
}

@Composable
private fun VideoFitSheet(
    currentFit: String,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
    accentColor: Color? = null
) {
    val themeAccent = accentColor ?: LocalThemeAccent.current
    val modes = listOf(
        Pair("Fit", "Preserve standard 16:9 aspect ratio"),
        Pair("Cover", "Zoom and crop edges to fill full screen"),
        Pair("Stretch", "Stretch video non-uniformly to all edges"),
        Pair("Fill", "Fill full frame container"),
        Pair("Zoom", "Zoom to fill display width"),
        Pair("16:9", "Widescreen 16:9 aspect ratio"),
        Pair("4:3", "Classic 4:3 aspect ratio")
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AspectRatio, contentDescription = null, tint = themeAccent, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Video Aspect Ratio", color = TextPrimary, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            val configuration = androidx.compose.ui.platform.LocalConfiguration.current
            val maxContentHeight = (configuration.screenHeightDp * 0.55f).dp.coerceIn(140.dp, 360.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxContentHeight)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                modes.forEach { (mode, desc) ->
                    val isActive = currentFit.equals(mode, ignoreCase = true)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isActive) themeAccent.copy(alpha = 0.15f) else DarkSurfaceVariant)
                            .border(
                                width = if (isActive) 1.dp else 0.5.dp,
                                color = if (isActive) themeAccent else DarkBorder,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                onPick(mode)
                                onDismiss()
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = mode,
                                color = if (isActive) themeAccent else TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                            )
                            Text(desc, color = TextSecondary, fontSize = 10.sp)
                        }
                        if (isActive) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = themeAccent, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close", color = TextSecondary) } }
    )
}

