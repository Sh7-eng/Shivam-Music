package com.example.data.model

data class Song(
    val id: String,
    val name: String,
    val artist: String,
    val album: String = "Unknown Album",
    val art: String = "",
    val downloadLink: String = "",
    val duration: Int = 0,
    val durationMs: Long = (duration * 1000).toLong(),
    val isLiked: Boolean = false
)

data class LyricLine(
    val timeMs: Long,
    val text: String,
    val endTimeMs: Long = 0L
)

data class LyricsData(
    val isSynced: Boolean = false,
    val lines: List<LyricLine> = emptyList(),
    val plainLyrics: String? = null,
    val source: String = "LRCLIB Real-time Sync",
    val syncOffsetMs: Long = 0L
)

data class Playlist(
    val id: String,
    val name: String,
    val songs: List<Song> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

enum class RepeatMode {
    OFF, ALL, ONE
}

data class JamSession(
    val code: String,
    val isHost: Boolean,
    val participantsCount: Int = 1
)
