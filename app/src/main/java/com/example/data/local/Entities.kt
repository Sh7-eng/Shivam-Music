package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.Song

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: String,
    val name: String,
    val artist: String,
    val album: String,
    val art: String,
    val downloadLink: String,
    val duration: Int,
    val isLiked: Boolean = false,
    val lastPlayedAt: Long? = null,
    val addedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): Song {
        return Song(
            id = id,
            name = name,
            artist = artist,
            album = album,
            art = art,
            downloadLink = downloadLink,
            duration = duration,
            isLiked = isLiked
        )
    }

    companion object {
        fun fromDomain(song: Song, isLiked: Boolean = song.isLiked, lastPlayedAt: Long? = null): SongEntity {
            return SongEntity(
                id = song.id,
                name = song.name,
                artist = song.artist,
                album = song.album,
                art = song.art,
                downloadLink = song.downloadLink,
                duration = song.duration,
                isLiked = isLiked,
                lastPlayedAt = lastPlayedAt
            )
        }
    }
}

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_songs",
    primaryKeys = ["playlistId", "songId"]
)
data class PlaylistSongCrossRef(
    val playlistId: String,
    val songId: String,
    val orderIndex: Int = 0
)
