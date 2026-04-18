package com.app.mitvplayer.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.app.mitvplayer.data.models.Channel
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {

    @Query("SELECT * FROM channels WHERE playlistId = :playlistId ORDER BY orderIndex ASC")
    fun getChannelsForPlaylist(playlistId: Long): Flow<List<Channel>>

    @Query("SELECT * FROM channels WHERE playlistId = :playlistId AND groupTitle = :group ORDER BY orderIndex ASC")
    fun getChannelsByGroup(playlistId: Long, group: String): Flow<List<Channel>>

    @Query("SELECT * FROM channels WHERE playlistId = :playlistId AND (groupTitle IS NULL OR groupTitle = '') ORDER BY orderIndex ASC")
    fun getChannelsWithoutGroup(playlistId: Long): Flow<List<Channel>>

    @Query("SELECT DISTINCT groupTitle FROM channels WHERE playlistId = :playlistId AND groupTitle IS NOT NULL ORDER BY groupTitle ASC")
    fun getGroupsForPlaylist(playlistId: Long): Flow<List<String>>

    @Query("""
        SELECT groupTitle, COUNT(*) as cnt 
        FROM channels 
        WHERE playlistId = :playlistId AND groupTitle IS NOT NULL AND groupTitle != '' 
        GROUP BY groupTitle 
        ORDER BY groupTitle ASC
    """)
    suspend fun getGroupsWithCounts(playlistId: Long): List<GroupCount>

    @Query("SELECT COUNT(*) FROM channels WHERE playlistId = :playlistId AND (groupTitle IS NULL OR groupTitle = '')")
    suspend fun getUncategorizedCount(playlistId: Long): Int

    @Query("SELECT COUNT(*) FROM channels WHERE playlistId = :playlistId")
    suspend fun getChannelCount(playlistId: Long): Int

    @Query("SELECT * FROM channels WHERE id = :channelId")
    suspend fun getChannelById(channelId: Long): Channel?

    @Query("""
        SELECT * FROM channels 
        WHERE playlistId = :playlistId 
        ORDER BY orderIndex ASC 
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getChannelsPaged(playlistId: Long, limit: Int, offset: Int): List<Channel>

    @Insert
    suspend fun insertChannels(channels: List<Channel>)

    @Query("DELETE FROM channels WHERE playlistId = :playlistId")
    suspend fun deleteChannelsForPlaylist(playlistId: Long)

    @Query("""
        SELECT * FROM channels 
        WHERE playlistId = :playlistId 
        AND LOWER(name) LIKE '%' || :query || '%'
        ORDER BY orderIndex ASC
        LIMIT :limit
    """)
    suspend fun searchChannels(playlistId: Long, query: String, limit: Int = 100): List<Channel>
}

data class GroupCount(
    val groupTitle: String,
    val cnt: Int
)
