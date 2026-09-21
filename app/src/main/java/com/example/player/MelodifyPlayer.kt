package com.example.player

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import android.util.Log
import com.example.data.model.RepeatMode
import com.example.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class MelodifyPlayer(private val context: Context) {

    private val TAG = "MelodifyPlayer"
    private var mediaPlayer: MediaPlayer? = null
    private var outgoingPlayer: MediaPlayer? = null
    private val playerScope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null
    private var crossfadeJob: Job? = null
    private var activeSessionId = 0L
    private var isAutoCrossfadingTriggered = false

    private val _currentTrack = MutableStateFlow<Song?>(null)
    val currentTrack: StateFlow<Song?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _isCrossfading = MutableStateFlow(false)
    val isCrossfading: StateFlow<Boolean> = _isCrossfading.asStateFlow()

    // Crossfade duration in milliseconds: 0 = Off, 2000, 4000 (default), 6000, 8000, 12000
    private val _crossfadeDurationMs = MutableStateFlow(4000)
    val crossfadeDurationMs: StateFlow<Int> = _crossfadeDurationMs.asStateFlow()

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private val _queueIndex = MutableStateFlow(0)
    val queueIndex: StateFlow<Int> = _queueIndex.asStateFlow()

    private val _shuffle = MutableStateFlow(false)
    val shuffle: StateFlow<Boolean> = _shuffle.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    var onTrackStartedListener: ((Song) -> Unit)? = null

    fun setCrossfadeDuration(durationMs: Int) {
        _crossfadeDurationMs.value = durationMs.coerceAtLeast(0)
    }

    fun playTrack(song: Song, newQueue: List<Song> = listOf(song), index: Int = 0) {
        _queue.value = newQueue
        _queueIndex.value = index.coerceIn(0, (newQueue.size - 1).coerceAtLeast(0))
        prepareAndPlay(song)
    }

    private fun prepareAndPlay(song: Song) {
        isAutoCrossfadingTriggered = false
        val current = mediaPlayer
        val shouldCrossfade = _crossfadeDurationMs.value > 0 && current != null && _isPlaying.value

        activeSessionId++
        val sessionId = activeSessionId

        _currentTrack.value = song
        _isBuffering.value = true
        _errorMessage.value = null

        if (shouldCrossfade) {
            crossfadeJob?.cancel()
            outgoingPlayer?.runCatching {
                stop()
                release()
            }
            outgoingPlayer = current
            mediaPlayer = null
        } else {
            stopProgressLoop()
            cancelCrossfade()
            releasePlayer()
        }

        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(song.downloadLink)
                setOnPreparedListener { mp ->
                    if (sessionId != activeSessionId) {
                        mp.runCatching { release() }
                        return@setOnPreparedListener
                    }
                    _isBuffering.value = false
                    _durationMs.value = mp.duration.toLong().coerceAtLeast((song.duration * 1000).toLong())
                    applySpeed(_playbackSpeed.value, mp)
                    mediaPlayer = mp

                    if (shouldCrossfade && outgoingPlayer != null) {
                        mp.setVolume(0f, 0f)
                        mp.start()
                        _isPlaying.value = true
                        startProgressLoop()
                        onTrackStartedListener?.invoke(song)
                        startCrossfadeAnimation(outgoingPlayer, mp)
                    } else {
                        mp.setVolume(1f, 1f)
                        mp.start()
                        _isPlaying.value = true
                        startProgressLoop()
                        onTrackStartedListener?.invoke(song)
                    }
                }
                setOnCompletionListener { mp ->
                    if (sessionId == activeSessionId) {
                        handleTrackCompletion()
                    } else {
                        mp.runCatching { release() }
                    }
                }
                setOnErrorListener { mp, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    if (sessionId == activeSessionId) {
                        _isBuffering.value = false
                        _isPlaying.value = false
                        _isCrossfading.value = false
                        _errorMessage.value = "Playback error occurred. Trying to recover..."
                    }
                    mp.runCatching { release() }
                    false
                }
                prepareAsync()
            }
            if (!shouldCrossfade) {
                mediaPlayer = player
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start playback for ${song.name}", e)
            _isBuffering.value = false
            _isPlaying.value = false
            _isCrossfading.value = false
            _errorMessage.value = "Unable to stream track."
        }
    }

    private fun startCrossfadeAnimation(outgoing: MediaPlayer?, incoming: MediaPlayer) {
        crossfadeJob?.cancel()
        _isCrossfading.value = true

        crossfadeJob = playerScope.launch {
            val totalFadeMs = _crossfadeDurationMs.value.coerceAtLeast(300)
            val stepMs = 40L
            val totalSteps = (totalFadeMs / stepMs).toInt().coerceAtLeast(8)

            for (step in 1..totalSteps) {
                if (!isActive) break
                val fraction = (step.toFloat() / totalSteps.toFloat()).coerceIn(0f, 1f)
                val outVol = cos(fraction * PI / 2.0).toFloat().coerceIn(0f, 1f)
                val inVol = sin(fraction * PI / 2.0).toFloat().coerceIn(0f, 1f)

                try {
                    outgoing?.setVolume(outVol, outVol)
                    incoming.setVolume(inVol, inVol)
                } catch (e: Exception) {
                    // Ignore transient audio adjustment exceptions
                }
                delay(stepMs)
            }

            outgoing?.runCatching {
                stop()
                release()
            }
            if (outgoingPlayer == outgoing) {
                outgoingPlayer = null
            }
            try {
                incoming.setVolume(1f, 1f)
            } catch (e: Exception) { }

            _isCrossfading.value = false
        }
    }

    private fun cancelCrossfade() {
        crossfadeJob?.cancel()
        crossfadeJob = null
        outgoingPlayer?.runCatching {
            stop()
            release()
        }
        outgoingPlayer = null
        _isCrossfading.value = false
    }

    fun togglePlayPause() {
        val player = mediaPlayer ?: run {
            _currentTrack.value?.let { prepareAndPlay(it) }
            return
        }

        if (player.isPlaying) {
            player.pause()
            outgoingPlayer?.runCatching { pause() }
            _isPlaying.value = false
            stopProgressLoop()
        } else {
            player.start()
            outgoingPlayer?.runCatching { start() }
            _isPlaying.value = true
            startProgressLoop()
        }
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.let { player ->
            cancelCrossfade()
            val target = positionMs.coerceIn(0, _durationMs.value.coerceAtLeast(1L)).toInt()
            player.seekTo(target)
            _currentPositionMs.value = target.toLong()

            val total = _durationMs.value
            val fadeMs = _crossfadeDurationMs.value.toLong()
            if (target < total - fadeMs) {
                isAutoCrossfadingTriggered = false
            }
        }
    }

    fun next() {
        val q = _queue.value
        if (q.isEmpty()) return

        val nextIndex = if (_shuffle.value) {
            Random.nextInt(q.size)
        } else {
            (_queueIndex.value + 1) % q.size
        }
        _queueIndex.value = nextIndex
        prepareAndPlay(q[nextIndex])
    }

    fun previous() {
        val q = _queue.value
        if (q.isEmpty()) return

        if (_currentPositionMs.value > 3000L) {
            seekTo(0)
            return
        }

        val prevIndex = if (_queueIndex.value > 0) _queueIndex.value - 1 else q.size - 1
        _queueIndex.value = prevIndex
        prepareAndPlay(q[prevIndex])
    }

    fun toggleShuffle() {
        _shuffle.value = !_shuffle.value
    }

    fun toggleRepeat() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
    }

    fun setSpeed(speed: Float) {
        _playbackSpeed.value = speed
        applySpeed(speed, mediaPlayer)
    }

    private fun applySpeed(speed: Float, targetPlayer: MediaPlayer? = mediaPlayer) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                targetPlayer?.let { player ->
                    if (player.isPlaying || _isBuffering.value.not()) {
                        val params = player.playbackParams ?: PlaybackParams()
                        params.speed = speed
                        player.playbackParams = params
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error applying playback speed", e)
            }
        }
    }

    private fun handleTrackCompletion() {
        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                seekTo(0)
                mediaPlayer?.start()
                _isPlaying.value = true
                startProgressLoop()
            }
            RepeatMode.ALL -> {
                next()
            }
            RepeatMode.OFF -> {
                val q = _queue.value
                if (_queueIndex.value < q.size - 1) {
                    next()
                } else {
                    _isPlaying.value = false
                    _currentPositionMs.value = 0L
                    stopProgressLoop()
                }
            }
        }
    }

    private fun startProgressLoop() {
        progressJob?.cancel()
        progressJob = playerScope.launch {
            while (isActive) {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        val current = player.currentPosition.toLong()
                        _currentPositionMs.value = current
                        val total = _durationMs.value
                        val fadeMs = _crossfadeDurationMs.value.toLong()

                        if (!isAutoCrossfadingTriggered && fadeMs > 0 && total > fadeMs * 2 && current >= (total - fadeMs)) {
                            val q = _queue.value
                            val hasNext = when (_repeatMode.value) {
                                RepeatMode.ONE -> true
                                RepeatMode.ALL -> q.isNotEmpty()
                                RepeatMode.OFF -> _queueIndex.value < q.size - 1
                            }
                            if (hasNext) {
                                isAutoCrossfadingTriggered = true
                                next()
                            }
                        }
                    }
                }
                delay(150)
            }
        }
    }

    private fun stopProgressLoop() {
        progressJob?.cancel()
        progressJob = null
    }

    fun release() {
        stopProgressLoop()
        cancelCrossfade()
        releasePlayer()
    }

    private fun releasePlayer() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            // ignore
        } finally {
            mediaPlayer = null
        }
    }
}
