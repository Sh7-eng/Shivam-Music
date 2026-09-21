package com.example.data.repository

import android.content.Context
import com.example.data.local.MelodifyDao
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistSongCrossRef
import com.example.data.local.SongEntity
import com.example.data.model.LyricsData
import com.example.data.model.Playlist
import com.example.data.model.Song
import com.example.data.remote.MelodifyApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class MusicRepository(
    private val context: Context,
    private val dao: MelodifyDao,
    private val apiService: MelodifyApiService
) {
    val likedSongs: Flow<List<Song>> = dao.getLikedSongs().map { entities ->
        entities.map { it.toDomain() }
    }

    val recentlyPlayed: Flow<List<Song>> = dao.getRecentlyPlayed(30).map { entities ->
        entities.map { it.toDomain() }
    }

    val playlists: Flow<List<Playlist>> = dao.getAllPlaylists().map { entities ->
        entities.map { entity ->
            Playlist(
                id = entity.id,
                name = entity.name,
                createdAt = entity.createdAt
            )
        }
    }

    suspend fun searchSongs(query: String, limit: Int = 15): List<Song> {
        val cleanQ = query.trim()
        if (cleanQ.isBlank()) return emptyList()

        val remoteSongs = apiService.searchSongs(cleanQ, limit)
        if (remoteSongs.isNotEmpty()) {
            dao.upsertSongs(remoteSongs.map { SongEntity.fromDomain(it) })
            return remoteSongs
        }

        // Fallback to local DB songs matching query
        val localEntities = dao.searchLocalSongs(cleanQ, limit)
        if (localEntities.isNotEmpty()) {
            return localEntities.map { it.toDomain() }
        }

        // Fallback to curated catalog
        return MelodifyApiService.FEATURED_CHARTS.filter {
            it.name.contains(cleanQ, ignoreCase = true) ||
            it.artist.contains(cleanQ, ignoreCase = true) ||
            it.album.contains(cleanQ, ignoreCase = true)
        }
    }

    suspend fun getSongDetails(id: String): Song? {
        val local = dao.getSongById(id)
        if (local != null && local.downloadLink.isNotBlank()) {
            return local.toDomain()
        }
        val remote = apiService.getSongById(id)
        if (remote != null) {
            dao.upsertSong(SongEntity.fromDomain(remote))
        }
        return remote
    }

    suspend fun toggleLike(song: Song): Boolean {
        val existing = dao.getSongById(song.id)
        val newLiked = !(existing?.isLiked ?: song.isLiked)
        val entity = SongEntity.fromDomain(song, isLiked = newLiked)
        dao.upsertSong(entity)
        return newLiked
    }

    suspend fun recordPlayed(song: Song) {
        val existing = dao.getSongById(song.id)
        val entity = SongEntity.fromDomain(
            song = song,
            isLiked = existing?.isLiked ?: song.isLiked,
            lastPlayedAt = System.currentTimeMillis()
        )
        dao.upsertSong(entity)
    }

    suspend fun createPlaylist(name: String): String {
        val id = "pl_${UUID.randomUUID().toString().take(8)}"
        dao.insertPlaylist(PlaylistEntity(id = id, name = name.trim()))
        return id
    }

    suspend fun deletePlaylist(playlistId: String) {
        dao.clearPlaylistSongs(playlistId)
        dao.deletePlaylist(playlistId)
    }

    suspend fun addSongToPlaylist(playlistId: String, song: Song) {
        dao.upsertSong(SongEntity.fromDomain(song))
        dao.insertPlaylistSong(
            PlaylistSongCrossRef(
                playlistId = playlistId,
                songId = song.id,
                orderIndex = (System.currentTimeMillis() % 100000).toInt()
            )
        )
    }

    suspend fun removeSongFromPlaylist(playlistId: String, songId: String) {
        dao.removeSongFromPlaylist(playlistId, songId)
    }

    fun getSongsForPlaylist(playlistId: String): Flow<List<Song>> {
        return dao.getSongsForPlaylist(playlistId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getLyrics(song: Song): LyricsData {
        return apiService.getLyrics(
            trackName = song.name,
            artistName = song.artist,
            albumName = song.album,
            durationSec = song.duration
        )
    }
}
