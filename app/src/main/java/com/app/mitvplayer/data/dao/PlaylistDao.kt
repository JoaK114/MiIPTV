package com.app.mitvplayer.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.app.mitvplayer.data.models.Playlist
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<Playlist>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylistById(id: Long): Playlist?

    @Insert
    suspend fun insertPlaylist(playlist: Playlist): Long

    @Update
    suspend fun updatePlaylist(playlist: Playlist)

    @Query("UPDATE playlists SET lastUpdatedAt = :timestamp, channelCount = :channelCount WHERE id = :id")
    suspend fun updateLastUpdated(id: Long, timestamp: Long, channelCount: Int)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: Long)

    @Query("DELETE FROM playlists")
    suspend fun deleteAllPlaylists()

    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    suspend fun getAllPlaylistsSync(): List<Playlist>

    // ── Default playlist management ──

    @Query("SELECT * FROM playlists WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultPlaylist(): Playlist?

    @Query("UPDATE playlists SET isDefault = 0")
    suspend fun clearDefaultPlaylist()

    @Query("UPDATE playlists SET isDefault = 1 WHERE id = :id")
    suspend fun setDefaultPlaylist(id: Long)
}
