package com.example

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import com.example.data.util.DeepLinkTarget
import com.example.data.util.ShareUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.repository.AnimeRepository
import com.example.ui.components.BottomNavBar
import com.example.ui.details.AnimeDetailsScreen
import com.example.ui.download.DownloadScreen
import com.example.ui.home.HomeScreen
import com.example.ui.mylist.MyListScreen
import com.example.ui.navigation.Screen
import com.example.ui.notifications.NotificationsScreen
import com.example.ui.player.PlayerScreen
import com.example.ui.profile.ProfileScreen
import com.example.ui.schedule.ScheduleScreen
import com.example.ui.search.SearchScreen
import com.example.ui.settings.ConfirmationsSettingsScreen
import com.example.ui.settings.DownloadSettingsScreen
import com.example.ui.settings.LibrarySettingsScreen
import com.example.ui.settings.NotificationSettingsScreen
import com.example.ui.settings.PlayerSettingsScreen
import com.example.ui.settings.SubtitleSettingsScreen
import coil.Coil
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.SvgDecoder
import com.example.ui.profile.getEffectiveAvatarModel
import com.example.ui.settings.CustomizationPlaygroundScreen
import com.example.ui.splash.AnimatedSplashScreen
import com.example.ui.sync.MalSyncScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private var deepLinkTargetState by mutableStateOf<DeepLinkTarget?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        deepLinkTargetState = ShareUtils.parseDeepLink(intent)

        val imageLoader = ImageLoader.Builder(applicationContext)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
                add(SvgDecoder.Factory())
            }
            .build()
        Coil.setImageLoader(imageLoader)

        val repository = AnimeRepository(applicationContext)

        setContent {
            val settings by repository.settings.collectAsState(initial = repository.settingsManager.getSettings())

            LaunchedEffect(settings.blockScreenshots) {
                if (settings.blockScreenshots) {
                    window.setFlags(
                        WindowManager.LayoutParams.FLAG_SECURE,
                        WindowManager.LayoutParams.FLAG_SECURE
                    )
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            LaunchedEffect(settings.hideFromRecents) {
                try {
                    (getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)
                        ?.appTasks
                        ?.firstOrNull()
                        ?.setExcludeFromRecents(settings.hideFromRecents)
                } catch (_: Throwable) {}
            }

            MyApplicationTheme(
                themeAccent = settings.themeAccent,
                amoled = settings.amoledMode,
                appSettings = settings
            ) {
                MainApp(
                    repository = repository,
                    settings = settings,
                    deepLinkTarget = deepLinkTargetState,
                    onDeepLinkConsumed = { deepLinkTargetState = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val parsed = ShareUtils.parseDeepLink(intent)
        if (parsed != null) {
            deepLinkTargetState = parsed
        }
    }
}

@Composable
fun MainApp(
    repository: AnimeRepository,
    settings: com.example.data.model.AppSettings,
    deepLinkTarget: DeepLinkTarget? = null,
    onDeepLinkConsumed: () -> Unit = {}
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var showSplash by remember { mutableStateOf(deepLinkTarget == null) }
    val bgColor = MaterialTheme.colorScheme.background

    LaunchedEffect(deepLinkTarget) {
        val target = deepLinkTarget ?: return@LaunchedEffect
        showSplash = false
        val detailsRoute = Screen.Details.createRoute(target.animeId)
        navController.navigate(detailsRoute) {
            launchSingleTop = true
        }
        if (target.episodeNumber != null && target.episodeNumber > 0) {
            val lang = settings.defaultLanguage.ifBlank { "sub" }
            navController.navigate(Screen.Player.createRoute(target.animeId, target.episodeNumber, lang)) {
                launchSingleTop = true
            }
        }
        onDeepLinkConsumed()
    }

    LaunchedEffect(Unit) {
        repository.freshStartWipe()
        val initialSettings = repository.settingsManager.getSettings()
        if (initialSettings.autoSyncOnOpen) {
            val malUser = initialSettings.malUsername.trim()
            if (malUser.isNotEmpty()) {
                try {
                    repository.malSyncService.importFromMyAnimeList(malUser).onSuccess { list ->
                        repository.importWatchlist(list, eraseExisting = false)
                    }
                } catch (_: Exception) {}
            }
            val alUser = initialSettings.anilistUsername.trim()
            if (alUser.isNotEmpty()) {
                try {
                    repository.malSyncService.importFromAniList(alUser).onSuccess { list ->
                        repository.importWatchlist(list, eraseExisting = false)
                    }
                } catch (_: Exception) {}
            }
        }
    }

    // Android 13+ drops every notification from an app that has not asked for POST_NOTIFICATIONS,
    // which made download and episode alerts silently vanish. Ask once, right after launch.
    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(
                context,
                "Notifications are off — turn them on in system settings to get download alerts",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    LaunchedEffect(Unit) {
        val missingOnAndroid13Plus = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        if (missingOnAndroid13Plus) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val bottomBarRoutes = listOf(
        Screen.Home.route,
        Screen.Schedule.route,
        Screen.MyList.route,
        Screen.Download.route,
        Screen.Profile.route
    )

    val showBottomBar = currentRoute in bottomBarRoutes

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = bgColor,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
            if (showBottomBar) {
                BottomNavBar(
                    currentRoute = currentRoute,
                    avatarModel = getEffectiveAvatarModel(settings.avatarPath, settings.avatarUrl),
                    onNavigate = { route ->
                        if (currentRoute != route) {
                            navController.navigate(route) {
                                popUpTo(Screen.Home.route) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        val animDuration = if (settings.reduceAnimations) 0 else 220
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
                .padding(bottom = if (showBottomBar) innerPadding.calculateBottomPadding() else 0.dp)
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.fillMaxSize(),
                enterTransition = { fadeIn(animationSpec = tween(animDuration)) },
                exitTransition = { fadeOut(animationSpec = tween(animDuration)) },
                popEnterTransition = { fadeIn(animationSpec = tween(animDuration)) },
                popExitTransition = { fadeOut(animationSpec = tween(animDuration)) }
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(
                        repository = repository,
                        onAnimeClick = { animeId ->
                            navController.navigate(Screen.Details.createRoute(animeId))
                        },
                        onPlayClick = { animeId, episodeNum ->
                            navController.navigate(Screen.Player.createRoute(animeId, episodeNum, settings.defaultLanguage.ifBlank { "sub" }))
                        },
                        onSearchClick = {
                            navController.navigate(Screen.Search.route)
                        },
                        onNotificationsClick = {
                            navController.navigate(Screen.Notifications.route)
                        }
                    )
                }

                composable(Screen.Schedule.route) {
                    ScheduleScreen(
                        repository = repository,
                        onAnimeClick = { animeId ->
                            navController.navigate(Screen.Details.createRoute(animeId))
                        },
                        onSearchClick = {
                            navController.navigate(Screen.Search.route)
                        }
                    )
                }

                composable(Screen.MyList.route) {
                    MyListScreen(
                        repository = repository,
                        onAnimeClick = { animeId ->
                            navController.navigate(Screen.Details.createRoute(animeId))
                        },
                        onSearchClick = {
                            navController.navigate(Screen.Search.route)
                        }
                    )
                }

                composable(Screen.Download.route) {
                    DownloadScreen(
                        repository = repository,
                        onPlayClick = { animeId, episodeNum ->
                            navController.navigate(Screen.Player.createRoute(animeId, episodeNum, settings.defaultLanguage.ifBlank { "sub" }))
                        },
                        onSearchClick = {
                            navController.navigate(Screen.Search.route)
                        }
                    )
                }

                composable(Screen.Profile.route) {
                    ProfileScreen(
                        repository = repository,
                        onMalSyncClick = {
                            navController.navigate(Screen.MalSync.route)
                        },
                        onPlayerSettingsClick = {
                            navController.navigate(Screen.PlayerSettings.route)
                        },
                        onSubtitleSettingsClick = {
                            navController.navigate(Screen.SubtitleSettings.route)
                        },
                        onDownloadSettingsClick = {
                            navController.navigate(Screen.DownloadSettings.route)
                        },
                        onLibrarySettingsClick = {
                            navController.navigate(Screen.LibrarySettings.route)
                        },
                        onNotificationSettingsClick = {
                            navController.navigate(Screen.NotificationSettings.route)
                        },
                        onConfirmationsSettingsClick = {
                            navController.navigate(Screen.ConfirmationsSettings.route)
                        },
                        onCustomizationPlaygroundClick = {
                            navController.navigate(Screen.CustomizationPlayground.route)
                        }
                    )
                }

                composable(Screen.MalSync.route) {
                    MalSyncScreen(
                        repository = repository,
                        onBackClick = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(Screen.PlayerSettings.route) {
                    PlayerSettingsScreen(
                        repository = repository,
                        onBackClick = {
                            navController.popBackStack()
                        },
                        onOpenLayoutPlayground = {
                            navController.navigate(Screen.CustomizationPlayground.route)
                        }
                    )
                }

                composable(Screen.CustomizationPlayground.route) {
                    CustomizationPlaygroundScreen(
                        repository = repository,
                        onBackClick = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(Screen.SubtitleSettings.route) {
                    SubtitleSettingsScreen(
                        repository = repository,
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable(Screen.DownloadSettings.route) {
                    DownloadSettingsScreen(
                        repository = repository,
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable(Screen.LibrarySettings.route) {
                    LibrarySettingsScreen(
                        repository = repository,
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable(Screen.NotificationSettings.route) {
                    NotificationSettingsScreen(
                        repository = repository,
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable(Screen.ConfirmationsSettings.route) {
                    ConfirmationsSettingsScreen(
                        repository = repository,
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable(Screen.Search.route) {
                    SearchScreen(
                        repository = repository,
                        onAnimeClick = { animeId ->
                            navController.navigate(Screen.Details.createRoute(animeId))
                        },
                        onBackClick = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(Screen.Notifications.route) {
                    NotificationsScreen(
                        repository = repository,
                        onBackClick = {
                            navController.popBackStack()
                        },
                        onAnimeClick = { animeId ->
                            navController.navigate(Screen.Details.createRoute(animeId))
                        },
                        onDownloadsClick = {
                            navController.navigate(Screen.Download.route)
                        }
                    )
                }

                composable(
                    route = Screen.Details.route,
                    arguments = listOf(navArgument("animeId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val animeId = backStackEntry.arguments?.getString("animeId") ?: ""
                    AnimeDetailsScreen(
                        animeId = animeId,
                        repository = repository,
                        onBackClick = {
                            navController.popBackStack()
                        },
                        onPlayEpisode = { aid, epNum, lang ->
                            navController.navigate(Screen.Player.createRoute(aid, epNum, lang))
                        },
                        onRelatedAnimeClick = { relatedId ->
                            navController.navigate(Screen.Details.createRoute(relatedId))
                        }
                    )
                }

                composable(
                    route = Screen.Player.route,
                    arguments = listOf(
                        navArgument("animeId") { type = NavType.StringType },
                        navArgument("episodeNumber") { type = NavType.IntType },
                        navArgument("language") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val animeId = backStackEntry.arguments?.getString("animeId") ?: ""
                    val episodeNum = backStackEntry.arguments?.getInt("episodeNumber") ?: 1
                    val lang = backStackEntry.arguments?.getString("language") ?: "sub"

                    PlayerScreen(
                        animeId = animeId,
                        initialEpisodeNumber = episodeNum,
                        initialLanguage = lang,
                        repository = repository,
                        onBackClick = {
                            navController.popBackStack()
                        },
                        onAnimeDetailsClick = { targetAnimeId ->
                            navController.navigate(Screen.Details.createRoute(targetAnimeId))
                        }
                    )
                }
            }
        }
    }

    AnimatedVisibility(
        visible = showSplash,
        enter = fadeIn(tween(150)),
        exit = fadeOut(tween(400)) + scaleOut(targetScale = 1.05f)
    ) {
        AnimatedSplashScreen(onFinish = { showSplash = false })
    }
}
}
