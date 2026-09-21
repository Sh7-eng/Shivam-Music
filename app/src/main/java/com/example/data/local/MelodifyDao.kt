package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MelodifyDao {

    @Query("SELECT * FROM songs WHERE isLiked = 1 ORDER BY addedAt DESC")
    fun getLikedSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE lastPlayedAt IS NOT NULL ORDER BY lastPlayedAt DESC LIMIT :limit")
    fun getRecentlyPlayed(limit: Int = 30): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE id = :id LIMIT 1")
    suspend fun getSongById(id: String): SongEntity?

    @Query("SELECT * FROM songs WHERE name LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' OR album LIKE '%' || :query || '%' ORDER BY lastPlayedAt DESC, addedAt DESC LIMIT :limit")
    suspend fun searchLocalSongs(query: String, limit: Int = 15): List<SongEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSong(song: SongEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSongs(songs: List<SongEntity>)

    @Query("UPDATE songs SET isLiked = :isLiked WHERE id = :id")
    suspend fun updateLikeStatus(id: String, isLiked: Boolean)

    @Query("UPDATE songs SET lastPlayedAt = :timestamp WHERE id = :id")
    suspend fun updateLastPlayed(id: String, timestamp: Long)

    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: String)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun clearPlaylistSongs(playlistId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistSong(crossRef: PlaylistSongCrossRef)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: String, songId: String)

    @Query("""
        SELECT s.* FROM songs s
        INNER JOIN playlist_songs ps ON s.id = ps.songId
        WHERE ps.playlistId = :playlistId
        ORDER BY ps.orderIndex ASC
    """)
    fun getSongsForPlaylist(playlistId: String): Flow<List<SongEntity>>
}
