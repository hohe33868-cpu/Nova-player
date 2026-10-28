package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.model.MediaTrack
import com.example.player.PlayerManager
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.MiniPlayerBar
import com.example.ui.screens.*
import com.example.ui.theme.NovaPlayerTheme
import kotlinx.coroutines.launch

enum class MainTab(val title: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home),
    MUSIC("Music", Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic),
    VIDEOS("Videos", Icons.Filled.Videocam, Icons.Outlined.Videocam),
    PLAYLISTS("Playlists", Icons.Filled.PlaylistPlay, Icons.Outlined.PlaylistPlay),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

enum class SubScreen {
    NONE,
    NOW_PLAYING,
    VIDEO_PLAYER,
    EQUALIZER,
    PLAYBACK_SETTINGS,
    APPEARANCE,
    QUEUE,
    ABOUT
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val app = applicationContext as NovaApplication
            val coroutineScope = rememberCoroutineScope()

            // Observe Repositories and Preferences
            val preferences by app.preferencesRepository.playerPreferences.collectAsState()
            val audioEffects by app.preferencesRepository.audioEffectSettings.collectAsState()
            val capabilities by PlayerManager.effectCapabilities.collectAsState()

            val audioTracks by app.mediaRepository.allAudioTracks.collectAsState()
            val videoTracks by app.mediaRepository.allVideoTracks.collectAsState()
            val favorites by app.mediaRepository.favorites.collectAsState(initial = emptyList())
            val playlists by app.mediaRepository.playlists.collectAsState(initial = emptyList())
            val history by app.mediaRepository.playbackHistory.collectAsState(initial = emptyList())
            val isLoading by app.mediaRepository.isLoading.collectAsState()

            // Observe Player State
            val currentTrack by PlayerManager.currentTrack.collectAsState()
            val isPlaying by PlayerManager.isPlaying.collectAsState()
            val currentPos by PlayerManager.currentPositionMs.collectAsState()
            val durationMs by PlayerManager.durationMs.collectAsState()
            val shuffleEnabled by PlayerManager.shuffleEnabled.collectAsState()
            val repeatMode by PlayerManager.repeatMode.collectAsState()
            val playbackSpeed by PlayerManager.playbackSpeed.collectAsState()
            val queue by PlayerManager.queue.collectAsState()
            val currentIndex by PlayerManager.currentIndex.collectAsState()

            // Navigation state
            var currentTab by remember { mutableStateOf(MainTab.HOME) }
            var activeSubScreen by remember { mutableStateOf(SubScreen.NONE) }
            var activeVideo by remember { mutableStateOf<MediaTrack?>(null) }
            var showSleepTimerDialog by remember { mutableStateOf(false) }
            var trackForPlaylist by remember { mutableStateOf<MediaTrack?>(null) }

            // Permissions Handling
            val context = LocalContext.current
            var hasMediaPermission by remember {
                mutableStateOf(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
                    } else {
                        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                    }
                )
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { perms ->
                val audioGranted = perms[Manifest.permission.READ_MEDIA_AUDIO] == true ||
                        perms[Manifest.permission.READ_EXTERNAL_STORAGE] == true
                hasMediaPermission = audioGranted
                if (audioGranted) {
                    coroutineScope.launch {
                        app.mediaRepository.refreshLibrary()
                    }
                }
            }

            LaunchedEffect(Unit) {
                val neededPermissions = mutableListOf<String>()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    neededPermissions.add(Manifest.permission.READ_MEDIA_AUDIO)
                    neededPermissions.add(Manifest.permission.READ_MEDIA_VIDEO)
                    neededPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    neededPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
                permissionLauncher.launch(neededPermissions.toTypedArray())
            }

            // Resume last played track if configured and nothing loaded
            LaunchedEffect(preferences.resumeLastTrack) {
                if (preferences.resumeLastTrack && currentTrack == null) {
                    val lastUri = app.preferencesRepository.getLastPlayedUri()
                    val lastPos = app.preferencesRepository.getLastPlayedPosition()
                    if (lastUri != null && audioTracks.isNotEmpty()) {
                        val track = audioTracks.firstOrNull { it.uri == lastUri }
                        if (track != null) {
                            PlayerManager.playTrack(track, audioTracks)
                            PlayerManager.pause()
                            if (lastPos > 0) {
                                PlayerManager.seekTo(lastPos)
                            }
                        }
                    }
                }
            }

            NovaPlayerTheme(
                themeMode = preferences.themeMode,
                accentColor = preferences.accentColor
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Back handler for sub-screens
                    if (activeSubScreen != SubScreen.NONE) {
                        BackHandler {
                            activeSubScreen = SubScreen.NONE
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            contentWindowInsets = WindowInsets.safeDrawing,
                            bottomBar = {
                                if (activeSubScreen != SubScreen.VIDEO_PLAYER && activeSubScreen != SubScreen.NOW_PLAYING) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .windowInsetsPadding(WindowInsets.navigationBars)
                                    ) {
                                        // Mini Player Bar directly above navigation
                                        MiniPlayerBar(
                                            currentTrack = currentTrack,
                                            isPlaying = isPlaying,
                                            currentPositionMs = currentPos,
                                            durationMs = durationMs,
                                            onExpandClick = {
                                                activeSubScreen = SubScreen.NOW_PLAYING
                                            }
                                        )

                                        NavigationBar(
                                            modifier = Modifier.testTag("main_navigation_bar"),
                                            containerColor = MaterialTheme.colorScheme.surface
                                        ) {
                                            MainTab.values().forEach { tab ->
                                                val isSelected = currentTab == tab && activeSubScreen == SubScreen.NONE
                                                NavigationBarItem(
                                                    selected = isSelected,
                                                    onClick = {
                                                        currentTab = tab
                                                        activeSubScreen = SubScreen.NONE
                                                    },
                                                    icon = {
                                                        Icon(
                                                            imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                                            contentDescription = tab.title
                                                        )
                                                    },
                                                    label = { Text(tab.title) },
                                                    modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                // Main Tabs
                                when (currentTab) {
                                    MainTab.HOME -> {
                                        HomeScreen(
                                            audioTracks = audioTracks,
                                            videoTracks = videoTracks,
                                            playbackHistory = history,
                                            favorites = favorites,
                                            currentTrack = currentTrack,
                                            isPlaying = isPlaying,
                                            isRefreshing = isLoading,
                                            onRefreshLibrary = {
                                                coroutineScope.launch { app.mediaRepository.refreshLibrary() }
                                            },
                                            onImportUri = { uri ->
                                                coroutineScope.launch { app.mediaRepository.importFromUri(uri) }
                                            },
                                            onTrackClick = { track, list ->
                                                if (track.isVideo) {
                                                    activeVideo = track
                                                    activeSubScreen = SubScreen.VIDEO_PLAYER
                                                } else {
                                                    PlayerManager.playTrack(track, list)
                                                    activeSubScreen = SubScreen.NOW_PLAYING
                                                }
                                            },
                                            onToggleFavorite = { track ->
                                                coroutineScope.launch { app.mediaRepository.toggleFavorite(track) }
                                            },
                                            onAddToPlaylistClick = { track ->
                                                trackForPlaylist = track
                                            },
                                            onNavigateToMusic = { currentTab = MainTab.MUSIC },
                                            onNavigateToVideos = { currentTab = MainTab.VIDEOS }
                                        )
                                    }

                                    MainTab.MUSIC -> {
                                        MusicLibraryScreen(
                                            tracks = audioTracks,
                                            favorites = favorites,
                                            currentTrack = currentTrack,
                                            isPlaying = isPlaying,
                                            isCompact = preferences.compactLayout,
                                            onTrackClick = { track, list ->
                                                PlayerManager.playTrack(track, list)
                                                activeSubScreen = SubScreen.NOW_PLAYING
                                            },
                                            onToggleFavorite = { track ->
                                                coroutineScope.launch { app.mediaRepository.toggleFavorite(track) }
                                            },
                                            onAddToPlaylistClick = { track ->
                                                trackForPlaylist = track
                                            },
                                            onImportUri = { uri ->
                                                coroutineScope.launch { app.mediaRepository.importFromUri(uri) }
                                            },
                                            onRefreshLibrary = {
                                                coroutineScope.launch { app.mediaRepository.refreshLibrary() }
                                            }
                                        )
                                    }

                                    MainTab.VIDEOS -> {
                                        VideoLibraryScreen(
                                            videos = videoTracks,
                                            onVideoClick = { video ->
                                                activeVideo = video
                                                activeSubScreen = SubScreen.VIDEO_PLAYER
                                            },
                                            onImportUri = { uri ->
                                                coroutineScope.launch { app.mediaRepository.importFromUri(uri) }
                                            },
                                            onRefresh = {
                                                coroutineScope.launch { app.mediaRepository.refreshLibrary() }
                                            }
                                        )
                                    }

                                    MainTab.PLAYLISTS -> {
                                        PlaylistsScreen(
                                            playlists = playlists,
                                            favorites = favorites,
                                            allTracks = audioTracks,
                                            currentTrack = currentTrack,
                                            isPlaying = isPlaying,
                                            onCreatePlaylist = { name ->
                                                coroutineScope.launch { app.mediaRepository.createPlaylist(name) }
                                            },
                                            onRenamePlaylist = { id, name ->
                                                coroutineScope.launch { app.mediaRepository.renamePlaylist(id, name) }
                                            },
                                            onDeletePlaylist = { playlist ->
                                                coroutineScope.launch { app.mediaRepository.deletePlaylist(playlist) }
                                            },
                                            onTrackClick = { track, list ->
                                                PlayerManager.playTrack(track, list)
                                                activeSubScreen = SubScreen.NOW_PLAYING
                                            },
                                            onToggleFavorite = { track ->
                                                coroutineScope.launch { app.mediaRepository.toggleFavorite(track) }
                                            }
                                        )
                                    }

                                    MainTab.SETTINGS -> {
                                        SettingsScreen(
                                            onNavigateToEqualizer = { activeSubScreen = SubScreen.EQUALIZER },
                                            onNavigateToPlayback = { activeSubScreen = SubScreen.PLAYBACK_SETTINGS },
                                            onNavigateToAppearance = { activeSubScreen = SubScreen.APPEARANCE },
                                            onNavigateToSleepTimer = { showSleepTimerDialog = true },
                                            onNavigateToAbout = { activeSubScreen = SubScreen.ABOUT }
                                        )
                                    }
                                }
                            }
                        }

                        // Fullscreen Sub-screens with animations
                        AnimatedVisibility(
                            visible = activeSubScreen == SubScreen.NOW_PLAYING,
                            enter = slideInVertically { it } + fadeIn(),
                            exit = slideOutVertically { it } + fadeOut()
                        ) {
                            val isFav = favorites.any { it.mediaUri == currentTrack?.uri }

                            NowPlayingSheet(
                                track = currentTrack,
                                isPlaying = isPlaying,
                                currentPositionMs = currentPos,
                                durationMs = durationMs,
                                shuffleEnabled = shuffleEnabled,
                                repeatMode = repeatMode,
                                playbackSpeed = playbackSpeed,
                                isFavorite = isFav,
                                onToggleFavorite = {
                                    currentTrack?.let { tr ->
                                        coroutineScope.launch { app.mediaRepository.toggleFavorite(tr) }
                                    }
                                },
                                onCloseClick = { activeSubScreen = SubScreen.NONE },
                                onOpenEqualizer = { activeSubScreen = SubScreen.EQUALIZER },
                                onOpenSleepTimer = { showSleepTimerDialog = true },
                                onOpenQueue = { activeSubScreen = SubScreen.QUEUE }
                            )
                        }

                        // Dedicated Fullscreen Video Player
                        AnimatedVisibility(
                            visible = activeSubScreen == SubScreen.VIDEO_PLAYER && activeVideo != null,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            activeVideo?.let { vid ->
                                VideoPlayerScreen(
                                    video = vid,
                                    onBackClick = {
                                        activeSubScreen = SubScreen.NONE
                                        activeVideo = null
                                    }
                                )
                            }
                        }

                        // Equalizer Screen
                        AnimatedVisibility(
                            visible = activeSubScreen == SubScreen.EQUALIZER,
                            enter = slideInHorizontally { it } + fadeIn(),
                            exit = slideOutHorizontally { it } + fadeOut()
                        ) {
                            EqualizerScreen(
                                capabilities = capabilities,
                                settings = audioEffects,
                                onBackClick = { activeSubScreen = SubScreen.NONE }
                            )
                        }

                        // Playback Settings Screen
                        AnimatedVisibility(
                            visible = activeSubScreen == SubScreen.PLAYBACK_SETTINGS,
                            enter = slideInHorizontally { it } + fadeIn(),
                            exit = slideOutHorizontally { it } + fadeOut()
                        ) {
                            PlaybackSettingsScreen(
                                preferences = preferences,
                                onBackClick = { activeSubScreen = SubScreen.NONE }
                            )
                        }

                        // Appearance & Themes Screen
                        AnimatedVisibility(
                            visible = activeSubScreen == SubScreen.APPEARANCE,
                            enter = slideInHorizontally { it } + fadeIn(),
                            exit = slideOutHorizontally { it } + fadeOut()
                        ) {
                            AppearanceScreen(
                                preferences = preferences,
                                onBackClick = { activeSubScreen = SubScreen.NONE }
                            )
                        }

                        // Queue Screen
                        AnimatedVisibility(
                            visible = activeSubScreen == SubScreen.QUEUE,
                            enter = slideInVertically { it } + fadeIn(),
                            exit = slideOutVertically { it } + fadeOut()
                        ) {
                            QueueScreen(
                                queue = queue,
                                currentIndex = currentIndex,
                                isPlaying = isPlaying,
                                onBackClick = { activeSubScreen = SubScreen.NOW_PLAYING }
                            )
                        }

                        // About & Permissions Screen
                        AnimatedVisibility(
                            visible = activeSubScreen == SubScreen.ABOUT,
                            enter = slideInHorizontally { it } + fadeIn(),
                            exit = slideOutHorizontally { it } + fadeOut()
                        ) {
                            AboutPermissionsScreen(
                                onBackClick = { activeSubScreen = SubScreen.NONE }
                            )
                        }

                        // Sleep Timer Dialog
                        if (showSleepTimerDialog) {
                            SleepTimerDialog(onDismiss = { showSleepTimerDialog = false })
                        }

                        // Add to Playlist Dialog
                        trackForPlaylist?.let { track ->
                            AddToPlaylistDialog(
                                track = track,
                                playlists = playlists,
                                onDismiss = { trackForPlaylist = null }
                            )
                        }
                    }
                }
            }
        }
    }
}
