package com.example.data.model

import com.squareup.moshi.JsonClass

/**
 * Every user-facing knob in the app. Serialized to JSON through Moshi so that backups,
 * imports and resets always carry the complete preference set instead of a hand-picked subset.
 */
@JsonClass(generateAdapter = true)
data class AppSettings(
    // ---- Streams & servers ----
    val defaultServer: String = "mega",
    val defaultLanguage: String = "sub",
    val playbackQuality: String = "Auto",
    val autoFailover: Boolean = true,
    val failoverServerCount: Int = 3,

    // ---- Playback behaviour ----
    val autoplay: Boolean = true,
    val autoNextEpisode: Boolean = true,
    val autoNextCountdownSec: Int = 5,
    val defaultPlaybackSpeed: Float = 1.0f,
    val autoSkipIntro: Boolean = false,
    val autoSkipOutro: Boolean = false,
    val introSkipSeconds: Int = 85,
    val outroSkipSeconds: Int = 90,
    val showSkipButtons: Boolean = true,
    val saveProgressEverySecond: Boolean = true,
    val saveProgressIntervalSec: Int = 1,
    val resumeThresholdSec: Int = 10,
    val resumePromptEnabled: Boolean = true,

    // ---- Gestures ----
    val doubleTapSeekEnabled: Boolean = true,
    val seekDurationSec: Int = 10,
    val gestureVolumeEnabled: Boolean = true,
    val gestureBrightnessEnabled: Boolean = true,
    val gestureHorizontalSeekEnabled: Boolean = true,
    val singleTapTogglesControls: Boolean = true,
    val controlsAutoHideSec: Int = 5,
    val swipeUpDownOpensSettings: Boolean = false,

    // ---- Hold to speed ----
    val holdToSpeedEnabled: Boolean = true,
    val holdToSpeedMultiplier: Float = 2.0f,
    val holdToSpeedHaptic: Boolean = true,
    val holdToSpeedShowBadge: Boolean = true,
    /** Where the hold gesture works: "Anywhere", "Left Half" or "Right Half". */
    val holdToSpeedZone: String = "Anywhere",
    /** Hold the other half of the screen to slow down instead of speeding up. */
    val holdToSpeedSlowDown: Boolean = false,
    val holdToSpeedSlowMultiplier: Float = 0.5f,
    /** Ease the rate up instead of jumping straight to the multiplier. */
    val holdToSpeedRampUp: Boolean = true,
    /** Where the speed badge appears: "Top", "Center" or "Bottom". */
    val holdToSpeedBadgePosition: String = "Center",
    /** Keep the video moving at the hold speed after you let go. */
    val holdToSpeedKeepAfterRelease: Boolean = false,

    // ---- Video output ----
    val videoFitMode: String = "Fit",
    val keepScreenOn: Boolean = true,
    val backgroundPlayback: Boolean = true,
    val pipEnabled: Boolean = true,
    val loudnessNormalization: Boolean = false,

    // ---- Player lock ----
    val lockButtonEnabled: Boolean = true,
    val autoLockEnabled: Boolean = false,
    val autoLockDelaySec: Int = 60,
    val lockBlocksTouches: Boolean = true,
    val lockHapticFeedback: Boolean = true,
    val orientationLockMode: String = "Auto",
    val showLockedBadge: Boolean = true,
    /** Where the unobtrusive screen-lock icon sits in the letterbox area: "Left" or "Right". */
    val lockButtonSide: String = "Left",
    /** Switch back to the legacy video player layout when true. */
    val useLegacyPlayerLayout: Boolean = false,

    // ---- Player layout playground ----
    /** Comma-separated top bar control IDs in display order. */
    val playerTopBarOrder: String = "audio,subtitles,speed,servers,fit,sleep,episodes,pip,fullscreen",
    /** Comma-separated control IDs hidden by the user in the layout playground. */
    val playerHiddenControls: String = "",
    /** Where transport controls appear: "Center", "Bottom", or "Split". */
    val playerTransportPosition: String = "Center",
    /**Size of the player control icons: "Compact", "Standard", or "Large". */
    val playerControlSize: String = "Standard",
    /** Accent color override inside the video player ("Theme" uses app themeAccent). */
    val playerAccentOverride: String = "Theme",
    /** Scrim darkness when player controls are visible (0.2f .. 0.85f). */
    val playerOverlayDimAlpha: Float = 0.50f,
    /** Timeline slider style: "Modern", "Minimal", or "Thick". */
    val playerSeekbarStyle: String = "Modern",

    // ---- Subtitles ----
    val subtitlesEnabled: Boolean = true,
    val subtitleLanguage: String = "English",
    val subtitleFetchEnabled: Boolean = true,
    val subtitleAutoMatch: Boolean = true,
    val subtitleFontSizeSp: Int = 16,
    val subtitleTextColor: String = "White",
    val subtitleBackgroundColor: String = "BlackSemi",
    val subtitleOutline: Boolean = true,
    val subtitleBold: Boolean = false,
    val subtitlePosition: String = "Bottom",
    val subtitleDelayMs: Int = 0,

    // ---- Downloads ----
    val downloadQuality: String = "1080p",
    val downloadLocation: String = "In-App",
    val downloadCustomFolderUri: String = "",
    val wifiOnlyDownloads: Boolean = false,
    val downloadConcurrency: Int = 2,
    val downloadAutoNextCount: Int = 0,
    val downloadSubtitles: String = "English",
    val downloadNotifications: Boolean = true,
    val downloadAutoDeleteWatched: Boolean = false,
    val downloadRetryCount: Int = 2,
    val downloadPreferredContainer: String = "Auto",
    val downloadExportAfterFinish: Boolean = false,

    // ---- Library & series ----
    val titleLanguagePreference: String = "English",
    val continueWatchingOneCard: Boolean = true,
    val continueWatchingLimit: Int = 20,
    val continueWatchingShowNextEpisode: Boolean = true,
    val friendlySeasonLabels: Boolean = true,
    val seasonLabelStyle: String = "Season",
    val hideAdaptationLabel: Boolean = true,
    val markWatchedThresholdPercent: Int = 90,
    val autoAddToMyListOnWatch: Boolean = false,
    val spoilerBlurCovers: Boolean = false,
    /** Episode list style: "Cards", "Numbers" or "Grid". */
    val episodeLayout: String = "Cards",
    /** How My List and the library order anime. See LibrarySort for the supported keys. */
    val librarySortOrder: String = "Recently Added",
    /** Show a hint above the episode list telling you which style is active. */
    val showEpisodeLayoutHint: Boolean = true,

    // ---- Notifications ----
    val airingNotifications: Boolean = true,
    val newEpisodeNotifications: Boolean = true,
    val downloadNotificationsSound: Boolean = false,
    val notificationLeadMinutes: Int = 15,
    /** Post a system notification when an episode download finishes. */
    val downloadCompleteNotifications: Boolean = true,

    // ---- Confirmations ----
    /** Ask before anything that cannot be undone: deletes, clears and resets. */
    val confirmDestructiveActions: Boolean = true,
    /** Extra warning before deleting a download you have not finished watching. */
    val confirmBeforeDeletingDownloads: Boolean = true,
    /** Warn when a download will be removed because you finished the episode. */
    val warnBeforeAutoDeletingWatched: Boolean = true,

    // ---- Privacy ----
    val adBlockEnabled: Boolean = true,
    val incognitoMode: Boolean = false,
    val hideFromRecents: Boolean = false,
    val blockScreenshots: Boolean = false,

    // ---- Appearance ----
    val themeAccent: String = "Emerald",
    val amoledMode: Boolean = false,
    val reduceAnimations: Boolean = false,
    val cardCornerRadiusDp: Int = 14,
    /** Grid columns on My List, Search, and Collection screens (2 or 3). */
    val gridColumnsCount: Int = 2,
    /** Show rating star badge on anime cards. */
    val showRatingBadgesOnCards: Boolean = true,
    /** Show episode count badge on anime cards. */
    val showEpisodeBadgesOnCards: Boolean = true,
    /** Show quality/age rating badges on anime cards. */
    val showQualityBadgesOnCards: Boolean = true,
    /** Bottom navigation bar style: "Standard", "Compact", or "Floating". */
    val navBarStyle: String = "Standard",
    /** UI density spacing: "Comfortable", "Compact", or "Spacious". */
    val uiDensity: String = "Comfortable",
    /** Global font scale multiplier (0.85f .. 1.25f). */
    val fontScaleMultiplier: Float = 1.0f,

    // ---- Profile & sync ----
    val username: String = "ZenithWatcher",
    val avatarPath: String? = null,
    /** External avatar URL (supports PNG, JPG, WEBP, GIF, and SVG). */
    val avatarUrl: String = "",
    /** Optional raw/sanitized SVG markup when the user creates or imports an SVG avatar. */
    val avatarSvgContent: String = "",
    val profileBio: String = "Anime Enthusiast • Local-First",
    val profileBannerColor: String = "Default",
    val malUsername: String = "",
    val anilistUsername: String = "",
    val autoSyncOnOpen: Boolean = false,

    // ---- Google Cast & Smart TV (COMPLETELY disabled by default) ----
    val chromecastEnabled: Boolean = false,
    val chromecastDeviceName: String = "",

    // ---- 2-Way Tracker Sync & Auto-Scrobbler (DISABLED by default) ----
    val trackerSyncEnabled: Boolean = false,
    val autoScrobbleEnabled: Boolean = false,
    val anilistAuthToken: String = "",
    val malAuthToken: String = "",
    val scrobbleCompletionPercent: Int = 85,

    // ---- Discord Rich Presence (DISABLED by default) ----
    val discordRpcEnabled: Boolean = false,
    val discordWebhookUrl: String = "",

    // ---- Audio & Background mode ----
    val audioOnlyBackgroundEnabled: Boolean = false,
    /** Comma-separated anime IDs for which the user enabled airing reminders on the Schedule screen. */
    val airingReminderIds: String = "",
    /** Default search maturity filter: "SFW Only" | "Include 18+" | "18+ Only". */
    val adultContentFilter: String = "SFW Only"
) {
    companion object {
        /** Sane values used by the "Reset player preferences" action. */
        val DEFAULT = AppSettings()
    }
}

data class BackupData(
    val version: Int = 3,
    val exportedAt: Long = System.currentTimeMillis(),
    val app: String = "ZenithAnime",
    val anime: List<Anime> = emptyList(),
    val watchProgress: List<WatchProgress> = emptyList(),
    val downloads: List<DownloadItem> = emptyList(),
    val settings: AppSettings = AppSettings()
)
