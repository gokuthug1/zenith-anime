package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Schedule : Screen("schedule")
    object MyList : Screen("my_list")
    object Download : Screen("download")
    object Profile : Screen("profile")
    object Search : Screen("search")
    object Notifications : Screen("notifications")
    object Details : Screen("details/{animeId}") {
        fun createRoute(animeId: String) = "details/$animeId"
    }
    object Player : Screen("player/{animeId}/{episodeNumber}/{language}") {
        fun createRoute(animeId: String, episodeNumber: Int, language: String = "sub") =
            "player/$animeId/$episodeNumber/$language"
    }
    object MalSync : Screen("mal_sync")
    object UsageStats : Screen("usage_stats")
    object PlayerSettings : Screen("player_settings")
    object SettingsHub : Screen("settings_hub")
    object SubtitleSettings : Screen("subtitle_settings")
    object DownloadSettings : Screen("download_settings")
    object LibrarySettings : Screen("library_settings")
    object NotificationSettings : Screen("notification_settings")
    object ConfirmationsSettings : Screen("confirmations_settings")
    object CustomizationPlayground : Screen("customization_playground")
}
