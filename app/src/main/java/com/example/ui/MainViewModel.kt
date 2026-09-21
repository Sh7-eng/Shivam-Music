package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.MelodifyDatabase
import com.example.data.model.AiGenerationUiState
import com.example.data.model.AiPlaylistResult
import com.example.data.model.JamSession
import com.example.data.model.LyricLine
import com.example.data.model.LyricsData
import com.example.data.model.MoodArchetype
import com.example.data.model.Playlist
import com.example.data.model.RepeatMode
import com.example.data.model.Song
import com.example.data.remote.AiPlaylistService
import com.example.data.remote.MelodifyApiService
import com.example.data.repository.MusicRepository
import com.example.player.MelodifyPlayer
import com.example.ui.theme.AppThemePreset
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

enum class AppTab {
    HOME, SEARCH, LIBRARY, LIKED
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = MelodifyDatabase.getDatabase(application)
    private val apiService = MelodifyApiService()
    val repository = MusicRepository(application.applicationContext, database.melodifyDao(), apiService)
    val player = MelodifyPlayer(application)
    private val aiPlaylistService = AiPlaylistService(repository)

    // Player States
    val currentTrack: StateFlow<Song?> = player.currentTrack
    val isPlaying: StateFlow<Boolean> = player.isPlaying
    val currentPositionMs: StateFlow<Long> = player.currentPositionMs
    val durationMs: StateFlow<Long> = player.durationMs
    val isBuffering: StateFlow<Boolean> = player.isBuffering
    val shuffle: StateFlow<Boolean> = player.shuffle
    val repeatMode: StateFlow<RepeatMode> = player.repeatMode
    val playbackSpeed: StateFlow<Float> = player.playbackSpeed
    val queue: StateFlow<List<Song>> = player.queue
    val isCrossfading: StateFlow<Boolean> = player.isCrossfading
    val crossfadeDurationMs: StateFlow<Int> = player.crossfadeDurationMs

    // Repository Flows
    val likedSongs: StateFlow<List<Song>> = repository.likedSongs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val recentlyPlayed: StateFlow<List<Song>> = repository.recentlyPlayed.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val playlists: StateFlow<List<Playlist>> = repository.playlists.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Featured / Charts
    private val _featuredSongs = MutableStateFlow<List<Song>>(MelodifyApiService.FEATURED_CHARTS)
    val featuredSongs: StateFlow<List<Song>> = _featuredSongs.asStateFlow()

    // Navigation & Modals
    private val _currentTab = MutableStateFlow(AppTab.HOME)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    private val _isFullscreenPlayerOpen = MutableStateFlow(false)
    val isFullscreenPlayerOpen: StateFlow<Boolean> = _isFullscreenPlayerOpen.asStateFlow()

    private val _isLyricsViewActive = MutableStateFlow(false)
    val isLyricsViewActive: StateFlow<Boolean> = _isLyricsViewActive.asStateFlow()

    private val _selectedPlaylist = MutableStateFlow<Playlist?>(null)
    val selectedPlaylist: StateFlow<Playlist?> = _selectedPlaylist.asStateFlow()

    private val _selectedPlaylistSongs = MutableStateFlow<List<Song>>(emptyList())
    val selectedPlaylistSongs: StateFlow<List<Song>> = _selectedPlaylistSongs.asStateFlow()

    private val _songForPlaylistPicker = MutableStateFlow<Song?>(null)
    val songForPlaylistPicker: StateFlow<Song?> = _songForPlaylistPicker.asStateFlow()

    private val _isJamDialogOpen = MutableStateFlow(false)
    val isJamDialogOpen: StateFlow<Boolean> = _isJamDialogOpen.asStateFlow()

    private val _isCreatePlaylistDialogOpen = MutableStateFlow(false)
    val isCreatePlaylistDialogOpen: StateFlow<Boolean> = _isCreatePlaylistDialogOpen.asStateFlow()

    // Themes
    private val _currentThemePreset = MutableStateFlow(AppThemePreset.TERRACOTTA)
    val currentThemePreset: StateFlow<AppThemePreset> = _currentThemePreset.asStateFlow()

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _isThemeDialogOpen = MutableStateFlow(false)
    val isThemeDialogOpen: StateFlow<Boolean> = _isThemeDialogOpen.asStateFlow()

    private val _jamSession = MutableStateFlow<JamSession?>(null)
    val jamSession: StateFlow<JamSession?> = _jamSession.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // AI Playlist Generator States
    private val _isAiPlaylistDialogOpen = MutableStateFlow(false)
    val isAiPlaylistDialogOpen: StateFlow<Boolean> = _isAiPlaylistDialogOpen.asStateFlow()

    private val _aiPlaylistState = MutableStateFlow<AiGenerationUiState>(AiGenerationUiState.Idle)
    val aiPlaylistState: StateFlow<AiGenerationUiState> = _aiPlaylistState.asStateFlow()

    private val _selectedMoodArchetype = MutableStateFlow<MoodArchetype>(AiPlaylistService.CURATED_MOODS[0])
    val selectedMoodArchetype: StateFlow<MoodArchetype> = _selectedMoodArchetype.asStateFlow()

    private val _customMoodPrompt = MutableStateFlow("")
    val customMoodPrompt: StateFlow<String> = _customMoodPrompt.asStateFlow()

    private val _aiEnergyLevel = MutableStateFlow(0.65f)
    val aiEnergyLevel: StateFlow<Float> = _aiEnergyLevel.asStateFlow()

    private val _aiHistoryInfluence = MutableStateFlow(0.75f)
    val aiHistoryInfluence: StateFlow<Float> = _aiHistoryInfluence.asStateFlow()

    // Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Song>>(emptyList())
    val searchResults: StateFlow<List<Song>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private var searchJob: Job? = null

    // Lyrics
    private val _currentLyrics = MutableStateFlow<LyricsData?>(null)
    val currentLyrics: StateFlow<LyricsData?> = _currentLyrics.asStateFlow()

    private val _isLoadingLyrics = MutableStateFlow(false)
    val isLoadingLyrics: StateFlow<Boolean> = _isLoadingLyrics.asStateFlow()

    private val _lyricsOffsetMs = MutableStateFlow(0L)
    val lyricsOffsetMs: StateFlow<Long> = _lyricsOffsetMs.asStateFlow()

    private val _isKaraokeMode = MutableStateFlow(false)
    val isKaraokeMode: StateFlow<Boolean> = _isKaraokeMode.asStateFlow()

    val activeLyricLine: StateFlow<LyricLine?> = combine(
        player.currentPositionMs,
        _currentLyrics,
        _lyricsOffsetMs
    ) { pos: Long, lyrics: LyricsData?, offset: Long ->
        if (lyrics == null || lyrics.lines.isEmpty()) {
            null
        } else {
            val effective = pos + offset
            var active: LyricLine? = null
            for (line in lyrics.lines) {
                if (effective >= line.timeMs) {
                    active = line
                } else {
                    break
                }
            }
            active
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        // Record recently played when song starts
        player.onTrackStartedListener = { song ->
            viewModelScope.launch {
                _lyricsOffsetMs.value = 0L
                repository.recordPlayed(song)
                fetchLyricsFor(song)
            }
        }

        // Fetch live Top Hits to supplement featured charts
        viewModelScope.launch {
            try {
                val liveHits = apiService.searchSongs("Top Hits", limit = 10)
                if (liveHits.isNotEmpty()) {
                    _featuredSongs.value = liveHits
                }
            } catch (e: Exception) {
                // Keep default featured charts
            }
        }
    }

    fun setTab(tab: AppTab) {
        _currentTab.value = tab
        if (_selectedPlaylist.value != null) {
            _selectedPlaylist.value = null
        }
    }

    fun openFullscreenPlayer() {
        _isFullscreenPlayerOpen.value = true
    }

    fun closeFullscreenPlayer() {
        _isFullscreenPlayerOpen.value = false
    }

    fun toggleLyricsView() {
        _isLyricsViewActive.value = !_isLyricsViewActive.value
    }

    fun playSong(song: Song, queue: List<Song> = listOf(song), index: Int = 0) {
        player.playTrack(song, queue, index)
    }

    fun togglePlayPause() {
        player.togglePlayPause()
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
    }

    fun next() {
        player.next()
    }

    fun previous() {
        player.previous()
    }

    fun toggleShuffle() {
        player.toggleShuffle()
    }

    fun toggleRepeat() {
        player.toggleRepeat()
    }

    fun setSpeed(speed: Float) {
        player.setSpeed(speed)
    }

    fun setCrossfadeDuration(durationMs: Int) {
        player.setCrossfadeDuration(durationMs)
        val msg = if (durationMs == 0) "Crossfade turned Off" else "Crossfade set to ${durationMs / 1000}s"
        showToast(msg)
    }

    fun toggleLike(song: Song) {
        viewModelScope.launch {
            val isNowLiked = repository.toggleLike(song)
            showToast(if (isNowLiked) "Added to Liked Songs" else "Removed from Liked Songs")
            // Update current track isLiked if it's the same song
            val current = currentTrack.value
            if (current != null && current.id == song.id) {
                // flow from dao will update or re-query
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }

        searchJob = viewModelScope.launch {
            delay(400)
            _isSearching.value = true
            val results = repository.searchSongs(query)
            _searchResults.value = results
            _isSearching.value = false
        }
    }

    fun refreshLyrics() {
        val song = currentTrack.value ?: return
        fetchLyricsFor(song)
        showToast("Refreshing lyrics...")
    }

    fun toggleKaraokeMode() {
        _isKaraokeMode.value = !_isKaraokeMode.value
    }

    fun adjustLyricsOffset(deltaMs: Long) {
        val newOffset = (_lyricsOffsetMs.value + deltaMs).coerceIn(-10000L, 10000L)
        _lyricsOffsetMs.value = newOffset
        val sign = if (newOffset >= 0) "+${newOffset}ms" else "${newOffset}ms"
        showToast("Lyrics sync: $sign")
    }

    fun resetLyricsOffset() {
        _lyricsOffsetMs.value = 0L
        showToast("Lyrics sync reset (0ms)")
    }

    private fun fetchLyricsFor(song: Song) {
        viewModelScope.launch {
            _isLoadingLyrics.value = true
            val data = repository.getLyrics(song)
            _currentLyrics.value = data
            _isLoadingLyrics.value = false
        }
    }

    // Playlist actions
    fun createPlaylist(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.createPlaylist(name)
            showToast("Playlist '$name' created!")
            _isCreatePlaylistDialogOpen.value = false
        }
    }

    fun openPlaylistDetail(playlist: Playlist) {
        _selectedPlaylist.value = playlist
        viewModelScope.launch {
            repository.getSongsForPlaylist(playlist.id).collect { songs ->
                _selectedPlaylistSongs.value = songs
            }
        }
    }

    fun closePlaylistDetail() {
        _selectedPlaylist.value = null
        _selectedPlaylistSongs.value = emptyList()
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
            closePlaylistDetail()
            showToast("Playlist deleted")
        }
    }

    fun openAddToPlaylist(song: Song) {
        _songForPlaylistPicker.value = song
    }

    fun closeAddToPlaylist() {
        _songForPlaylistPicker.value = null
    }

    fun addSongToPlaylist(playlist: Playlist, song: Song) {
        viewModelScope.launch {
            repository.addSongToPlaylist(playlist.id, song)
            closeAddToPlaylist()
            showToast("Added to ${playlist.name}")
        }
    }

    fun removeSongFromPlaylist(playlistId: String, songId: String) {
        viewModelScope.launch {
            repository.removeSongFromPlaylist(playlistId, songId)
            showToast("Song removed from playlist")
        }
    }

    // Jam Session
    fun openJamDialog() {
        _isJamDialogOpen.value = true
    }

    fun closeJamDialog() {
        _isJamDialogOpen.value = false
    }

    fun createJamSession() {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val code = "JAM-" + (1..4).map { chars.random() }.joinToString("")
        _jamSession.value = JamSession(code = code, isHost = true)
        showToast("Jam session hosted: $code")
        closeJamDialog()
    }

    fun joinJamSession(code: String) {
        if (code.isBlank()) return
        _jamSession.value = JamSession(code = code.uppercase(), isHost = false)
        showToast("Joined Jam session: ${code.uppercase()}")
        closeJamDialog()
    }

    fun leaveJamSession() {
        _jamSession.value = null
        showToast("Left Jam session")
        closeJamDialog()
    }

    fun openCreatePlaylistDialog() {
        _isCreatePlaylistDialogOpen.value = true
    }

    fun closeCreatePlaylistDialog() {
        _isCreatePlaylistDialogOpen.value = false
    }

    // Themes
    fun openThemeDialog() {
        _isThemeDialogOpen.value = true
    }

    fun closeThemeDialog() {
        _isThemeDialogOpen.value = false
    }

    fun setThemePreset(preset: AppThemePreset) {
        _currentThemePreset.value = preset
        showToast("Switched to ${preset.title}")
    }

    fun toggleDarkMode(isDark: Boolean) {
        _isDarkMode.value = isDark
    }

    fun showToast(message: String) {
        _toastMessage.value = message
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun openAiPlaylistGenerator(initialMood: MoodArchetype? = null) {
        initialMood?.let {
            _selectedMoodArchetype.value = it
            _aiEnergyLevel.value = it.defaultEnergy
        }
        _isAiPlaylistDialogOpen.value = true
    }

    fun closeAiPlaylistGenerator() {
        _isAiPlaylistDialogOpen.value = false
    }

    fun selectMoodArchetype(mood: MoodArchetype) {
        _selectedMoodArchetype.value = mood
        _aiEnergyLevel.value = mood.defaultEnergy
    }

    fun setCustomMoodPrompt(prompt: String) {
        _customMoodPrompt.value = prompt
    }

    fun setAiEnergyLevel(level: Float) {
        _aiEnergyLevel.value = level
    }

    fun setAiHistoryInfluence(influence: Float) {
        _aiHistoryInfluence.value = influence
    }

    fun resetAiGeneratorState() {
        _aiPlaylistState.value = AiGenerationUiState.Idle
    }

    fun generateAiPlaylist() {
        viewModelScope.launch {
            _aiPlaylistState.value = AiGenerationUiState.Generating("Shivam AI is synthesizing your listening profile & emotional mood...")
            try {
                val history = recentlyPlayed.value
                val liked = likedSongs.value
                val mood = _selectedMoodArchetype.value
                val customPrompt = _customMoodPrompt.value
                val energy = _aiEnergyLevel.value
                val influence = _aiHistoryInfluence.value

                val result = aiPlaylistService.generateMoodPlaylist(
                    listeningHistory = history,
                    likedSongs = liked,
                    mood = mood,
                    userCustomThoughts = customPrompt,
                    energyLevel = energy,
                    historyInfluence = influence,
                    onProgress = { step ->
                        _aiPlaylistState.value = AiGenerationUiState.Generating(step)
                    }
                )
                _aiPlaylistState.value = AiGenerationUiState.Success(result)
                showToast("✨ Shivam AI curated \"${result.title}\"")
            } catch (e: Exception) {
                _aiPlaylistState.value = AiGenerationUiState.Error(e.message ?: "Failed to generate playlist. Please try again.")
            }
        }
    }

    fun playAiPlaylist(result: AiPlaylistResult) {
        if (result.songs.isNotEmpty()) {
            playSong(result.songs.first(), result.songs)
            showToast("Playing Shivam AI Playlist: ${result.title}")
        }
    }

    fun saveAiPlaylistToLibrary(result: AiPlaylistResult) {
        viewModelScope.launch {
            try {
                val playlistId = repository.createPlaylist(result.title)
                for (song in result.songs) {
                    repository.addSongToPlaylist(playlistId, song)
                }
                showToast("Saved \"${result.title}\" to your Library!")
            } catch (e: Exception) {
                showToast("Failed to save playlist")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        player.release()
    }
}
