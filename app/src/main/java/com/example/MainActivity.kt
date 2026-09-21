package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Song
import com.example.ui.AppTab
import com.example.ui.MainViewModel
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.AiPlaylistGeneratorDialog
import com.example.ui.components.CreatePlaylistDialog
import com.example.ui.components.FullscreenPlayer
import com.example.ui.components.JamSessionDialog
import com.example.ui.components.MiniPlayer
import com.example.ui.components.ThemeSelectorDialog
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.LikedSongsScreen
import com.example.ui.screens.PlaylistDetailScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.util.ShareHelper

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val currentThemePreset by viewModel.currentThemePreset.collectAsStateWithLifecycle()
            val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()

            MyApplicationTheme(
                preset = currentThemePreset,
                darkTheme = isDarkMode
            ) {
                MelodifyApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MelodifyApp(viewModel: MainViewModel) {
    val context = LocalContext.current
    val currentThemePreset by viewModel.currentThemePreset.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val isThemeDialogOpen by viewModel.isThemeDialogOpen.collectAsStateWithLifecycle()

    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val isFullscreenOpen by viewModel.isFullscreenPlayerOpen.collectAsStateWithLifecycle()
    val currentTrack by viewModel.currentTrack.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val isBuffering by viewModel.isBuffering.collectAsStateWithLifecycle()
    val currentPosMs by viewModel.currentPositionMs.collectAsStateWithLifecycle()
    val durationMs by viewModel.durationMs.collectAsStateWithLifecycle()
    val shuffle by viewModel.shuffle.collectAsStateWithLifecycle()
    val repeatMode by viewModel.repeatMode.collectAsStateWithLifecycle()
    val playbackSpeed by viewModel.playbackSpeed.collectAsStateWithLifecycle()
    val isLyricsViewActive by viewModel.isLyricsViewActive.collectAsStateWithLifecycle()
    val currentLyrics by viewModel.currentLyrics.collectAsStateWithLifecycle()
    val isLoadingLyrics by viewModel.isLoadingLyrics.collectAsStateWithLifecycle()
    val lyricsOffsetMs by viewModel.lyricsOffsetMs.collectAsStateWithLifecycle()
    val isKaraokeMode by viewModel.isKaraokeMode.collectAsStateWithLifecycle()
    val activeLyricLine by viewModel.activeLyricLine.collectAsStateWithLifecycle()
    val isCrossfading by viewModel.isCrossfading.collectAsStateWithLifecycle()
    val crossfadeDurationMs by viewModel.crossfadeDurationMs.collectAsStateWithLifecycle()

    val featuredSongs by viewModel.featuredSongs.collectAsStateWithLifecycle()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsStateWithLifecycle()
    val likedSongs by viewModel.likedSongs.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val selectedPlaylist by viewModel.selectedPlaylist.collectAsStateWithLifecycle()
    val selectedPlaylistSongs by viewModel.selectedPlaylistSongs.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()

    val jamSession by viewModel.jamSession.collectAsStateWithLifecycle()
    val isJamDialogOpen by viewModel.isJamDialogOpen.collectAsStateWithLifecycle()
    val isCreatePlaylistDialogOpen by viewModel.isCreatePlaylistDialogOpen.collectAsStateWithLifecycle()
    val songForPlaylistPicker by viewModel.songForPlaylistPicker.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

    val isAiPlaylistDialogOpen by viewModel.isAiPlaylistDialogOpen.collectAsStateWithLifecycle()
    val aiPlaylistState by viewModel.aiPlaylistState.collectAsStateWithLifecycle()
    val selectedMoodArchetype by viewModel.selectedMoodArchetype.collectAsStateWithLifecycle()
    val customMoodPrompt by viewModel.customMoodPrompt.collectAsStateWithLifecycle()
    val aiEnergyLevel by viewModel.aiEnergyLevel.collectAsStateWithLifecycle()
    val aiHistoryInfluence by viewModel.aiHistoryInfluence.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearToast()
        }
    }

    // Handle back button when full screen player is open or playlist detail is open
    BackHandler(enabled = isFullscreenOpen) {
        viewModel.closeFullscreenPlayer()
    }
    BackHandler(enabled = !isFullscreenOpen && selectedPlaylist != null) {
        viewModel.closePlaylistDetail()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Column(modifier = Modifier.navigationBarsPadding()) {
                // Persistent Mini Player above bottom navigation
                if (currentTrack != null) {
                    MiniPlayer(
                        song = currentTrack!!,
                        isPlaying = isPlaying,
                        isBuffering = isBuffering,
                        currentPosMs = currentPosMs,
                        durationMs = durationMs,
                        onPlayerClick = { viewModel.openFullscreenPlayer() },
                        onTogglePlayPause = { viewModel.togglePlayPause() },
                        onNext = { viewModel.next() },
                        onPrevious = { viewModel.previous() },
                        onLikeToggle = { viewModel.toggleLike(currentTrack!!) },
                        activeLyricText = activeLyricLine?.text,
                        isCrossfading = isCrossfading
                    )
                }

                // Bottom Navigation Bar
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    NavigationBarItem(
                        selected = currentTab == AppTab.HOME && selectedPlaylist == null,
                        onClick = { viewModel.setTab(AppTab.HOME) },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == AppTab.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                                contentDescription = "Home"
                            )
                        },
                        label = { Text("Home", fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    NavigationBarItem(
                        selected = currentTab == AppTab.SEARCH,
                        onClick = { viewModel.setTab(AppTab.SEARCH) },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == AppTab.SEARCH) Icons.Filled.Search else Icons.Outlined.Search,
                                contentDescription = "Search"
                            )
                        },
                        label = { Text("Search", fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    NavigationBarItem(
                        selected = currentTab == AppTab.LIBRARY,
                        onClick = { viewModel.setTab(AppTab.LIBRARY) },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == AppTab.LIBRARY) Icons.Filled.LibraryMusic else Icons.Outlined.LibraryMusic,
                                contentDescription = "Library"
                            )
                        },
                        label = { Text("Library", fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    NavigationBarItem(
                        selected = currentTab == AppTab.LIKED,
                        onClick = { viewModel.setTab(AppTab.LIKED) },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == AppTab.LIKED) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Liked"
                            )
                        },
                        label = { Text("Liked", fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                AppTab.HOME -> {
                    HomeScreen(
                        featuredSongs = featuredSongs,
                        recentlyPlayed = recentlyPlayed,
                        currentTrack = currentTrack,
                        isPlaying = isPlaying,
                        jamSession = jamSession,
                        currentThemePreset = currentThemePreset,
                        onSongClick = { song, list -> viewModel.playSong(song, list) },
                        onLikeToggle = { viewModel.toggleLike(it) },
                        onAddToPlaylist = { viewModel.openAddToPlaylist(it) },
                        onOpenJam = { viewModel.openJamDialog() },
                        onOpenThemes = { viewModel.openThemeDialog() },
                        onSelectTheme = { viewModel.setThemePreset(it) },
                        onMoodSelected = { mood ->
                            viewModel.setTab(AppTab.SEARCH)
                            viewModel.onSearchQueryChanged(mood)
                        },
                        onOpenAiPlaylist = { viewModel.openAiPlaylistGenerator() }
                    )
                }
                AppTab.SEARCH -> {
                    SearchScreen(
                        searchQuery = searchQuery,
                        searchResults = searchResults,
                        isSearching = isSearching,
                        currentTrack = currentTrack,
                        isPlaying = isPlaying,
                        onQueryChange = { viewModel.onSearchQueryChanged(it) },
                        onSongClick = { song, list -> viewModel.playSong(song, list) },
                        onLikeToggle = { viewModel.toggleLike(it) },
                        onAddToPlaylist = { viewModel.openAddToPlaylist(it) }
                    )
                }
                AppTab.LIBRARY -> {
                    if (selectedPlaylist != null) {
                        PlaylistDetailScreen(
                            playlist = selectedPlaylist!!,
                            songs = selectedPlaylistSongs,
                            currentTrack = currentTrack,
                            isPlaying = isPlaying,
                            onBack = { viewModel.closePlaylistDetail() },
                            onSongClick = { song, list -> viewModel.playSong(song, list) },
                            onLikeToggle = { viewModel.toggleLike(it) },
                            onRemoveSong = { songId ->
                                selectedPlaylist?.let { pl ->
                                    viewModel.removeSongFromPlaylist(pl.id, songId)
                                }
                            },
                            onDeletePlaylist = {
                                selectedPlaylist?.let { pl ->
                                    viewModel.deletePlaylist(pl.id)
                                }
                            }
                        )
                    } else {
                        LibraryScreen(
                            playlists = playlists,
                            likedSongsCount = likedSongs.size,
                            currentThemePreset = currentThemePreset,
                            onOpenLikedSongs = { viewModel.setTab(AppTab.LIKED) },
                            onOpenPlaylist = { viewModel.openPlaylistDetail(it) },
                            onCreatePlaylistClick = { viewModel.openCreatePlaylistDialog() },
                            onOpenThemes = { viewModel.openThemeDialog() },
                            onOpenAiPlaylist = { viewModel.openAiPlaylistGenerator() }
                        )
                    }
                }
                AppTab.LIKED -> {
                    LikedSongsScreen(
                        likedSongs = likedSongs,
                        currentTrack = currentTrack,
                        isPlaying = isPlaying,
                        onSongClick = { song, list -> viewModel.playSong(song, list) },
                        onLikeToggle = { viewModel.toggleLike(it) },
                        onAddToPlaylist = { viewModel.openAddToPlaylist(it) }
                    )
                }
            }

            // Fullscreen Now Playing Overlay
            AnimatedVisibility(
                visible = isFullscreenOpen && currentTrack != null,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                currentTrack?.let { song ->
                    FullscreenPlayer(
                        song = song,
                        isPlaying = isPlaying,
                        isBuffering = isBuffering,
                        currentPosMs = currentPosMs,
                        durationMs = durationMs,
                        shuffle = shuffle,
                        repeatMode = repeatMode,
                        playbackSpeed = playbackSpeed,
                        isLyricsViewActive = isLyricsViewActive,
                        lyricsData = currentLyrics,
                        isLoadingLyrics = isLoadingLyrics,
                        jamSession = jamSession,
                        onClose = { viewModel.closeFullscreenPlayer() },
                        onTogglePlayPause = { viewModel.togglePlayPause() },
                        onSeekTo = { viewModel.seekTo(it) },
                        onNext = { viewModel.next() },
                        onPrevious = { viewModel.previous() },
                        onToggleShuffle = { viewModel.toggleShuffle() },
                        onToggleRepeat = { viewModel.toggleRepeat() },
                        onSpeedChange = { viewModel.setSpeed(it) },
                        onToggleLyrics = { viewModel.toggleLyricsView() },
                        onLikeToggle = { viewModel.toggleLike(song) },
                        onAddToPlaylist = { viewModel.openAddToPlaylist(song) },
                        onOpenJam = { viewModel.openJamDialog() },
                        onShareTrack = { songToShare ->
                            ShareHelper.shareTrack(context, songToShare)
                            viewModel.showToast("Sharing \"${songToShare.name}\"...")
                        },
                        lyricsOffsetMs = lyricsOffsetMs,
                        isKaraokeMode = isKaraokeMode,
                        onAdjustLyricsOffset = { viewModel.adjustLyricsOffset(it) },
                        onResetLyricsOffset = { viewModel.resetLyricsOffset() },
                        onToggleKaraokeMode = { viewModel.toggleKaraokeMode() },
                        onRefreshLyrics = { viewModel.refreshLyrics() },
                        isCrossfading = isCrossfading,
                        crossfadeDurationMs = crossfadeDurationMs,
                        onCrossfadeDurationChange = { viewModel.setCrossfadeDuration(it) }
                    )
                }
            }
        }
    }

    // Collaborative Jam Dialog
    if (isJamDialogOpen) {
        JamSessionDialog(
            currentSession = jamSession,
            onDismiss = { viewModel.closeJamDialog() },
            onHostJam = { viewModel.createJamSession() },
            onJoinJam = { viewModel.joinJamSession(it) },
            onLeaveJam = { viewModel.leaveJamSession() }
        )
    }

    // Create Playlist Dialog
    if (isCreatePlaylistDialogOpen) {
        CreatePlaylistDialog(
            onDismiss = { viewModel.closeCreatePlaylistDialog() },
            onCreate = { viewModel.createPlaylist(it) }
        )
    }

    // Add to Playlist Dialog
    songForPlaylistPicker?.let { song ->
        AddToPlaylistDialog(
            song = song,
            playlists = playlists,
            onDismiss = { viewModel.closeAddToPlaylist() },
            onSelectPlaylist = { pl -> viewModel.addSongToPlaylist(pl, song) },
            onCreateNewPlaylist = {
                viewModel.closeAddToPlaylist()
                viewModel.openCreatePlaylistDialog()
            }
        )
    }

    // App Theme Selector Dialog
    if (isThemeDialogOpen) {
        ThemeSelectorDialog(
            currentPreset = currentThemePreset,
            isDarkMode = isDarkMode,
            onSelectPreset = { viewModel.setThemePreset(it) },
            onToggleDarkMode = { viewModel.toggleDarkMode(it) },
            onDismiss = { viewModel.closeThemeDialog() }
        )
    }

    // AI Playlist Generator Dialog
    AiPlaylistGeneratorDialog(
        isOpen = isAiPlaylistDialogOpen,
        onDismiss = { viewModel.closeAiPlaylistGenerator() },
        uiState = aiPlaylistState,
        selectedMood = selectedMoodArchetype,
        customPrompt = customMoodPrompt,
        energyLevel = aiEnergyLevel,
        historyInfluence = aiHistoryInfluence,
        recentlyPlayed = recentlyPlayed,
        likedSongs = likedSongs,
        onSelectMood = { viewModel.selectMoodArchetype(it) },
        onCustomPromptChange = { viewModel.setCustomMoodPrompt(it) },
        onEnergyLevelChange = { viewModel.setAiEnergyLevel(it) },
        onHistoryInfluenceChange = { viewModel.setAiHistoryInfluence(it) },
        onGenerateClick = { viewModel.generateAiPlaylist() },
        onPlayPlaylist = { viewModel.playAiPlaylist(it) },
        onSaveToLibrary = { viewModel.saveAiPlaylistToLibrary(it) },
        onPlayTrack = { song, list -> viewModel.playSong(song, list) },
        onLikeToggle = { viewModel.toggleLike(it) }
    )
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}
