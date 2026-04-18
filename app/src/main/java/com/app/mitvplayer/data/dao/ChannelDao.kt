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

    // ═══════════════════════════════════════════════════
    // Content type queries
    // ═══════════════════════════════════════════════════

    @Query("""
        SELECT * FROM channels 
        WHERE playlistId = :playlistId AND contentType = :contentType
        ORDER BY orderIndex ASC 
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getChannelsByContentTypePaged(
        playlistId: Long, contentType: String, limit: Int, offset: Int
    ): List<Channel>

    @Query("""
        SELECT DISTINCT groupTitle FROM channels 
        WHERE playlistId = :playlistId AND contentType = :contentType 
        AND groupTitle IS NOT NULL AND groupTitle != ''
        ORDER BY groupTitle ASC
    """)
    suspend fun getGroupsByContentType(playlistId: Long, contentType: String): List<String>

    @Query("""
        SELECT groupTitle, COUNT(*) as cnt 
        FROM channels 
        WHERE playlistId = :playlistId AND contentType = :contentType 
        AND groupTitle IS NOT NULL AND groupTitle != '' 
        GROUP BY groupTitle 
        ORDER BY groupTitle ASC
    """)
    suspend fun getGroupsWithCountsByContentType(playlistId: Long, contentType: String): List<GroupCount>

    @Query("""
        SELECT contentType, COUNT(*) as cnt 
        FROM channels 
        WHERE playlistId = :playlistId 
        GROUP BY contentType
    """)
    suspend fun getContentTypeCounts(playlistId: Long): List<ContentTypeCount>

    @Query("""
        SELECT * FROM channels 
        WHERE playlistId = :playlistId AND contentType = :contentType AND groupTitle = :group
        ORDER BY orderIndex ASC
    """)
    fun getChannelsByContentTypeAndGroup(
        playlistId: Long, contentType: String, group: String
    ): Flow<List<Channel>>

    // ═══════════════════════════════════════════════════
    // Series grouping queries
    // ═══════════════════════════════════════════════════

    /**
     * Get distinct series names for a playlist (for showing series "folders").
     */
    @Query("""
        SELECT DISTINCT seriesName FROM channels 
        WHERE playlistId = :playlistId AND contentType = 'series' 
        AND seriesName IS NOT NULL AND seriesName != ''
        ORDER BY seriesName ASC
    """)
    suspend fun getDistinctSeriesNames(playlistId: Long): List<String>

    /**
     * Get distinct series within a specific group.
     */
    @Query("""
        SELECT DISTINCT seriesName FROM channels 
        WHERE playlistId = :playlistId AND contentType = 'series' 
        AND groupTitle = :group
        AND seriesName IS NOT NULL AND seriesName != ''
        ORDER BY seriesName ASC
    """)
    suspend fun getDistinctSeriesNamesForGroup(playlistId: Long, group: String): List<String>

    /**
     * Get available seasons for a series.
     */
    @Query("""
        SELECT DISTINCT seasonNum FROM channels 
        WHERE playlistId = :playlistId AND seriesName = :seriesName 
        AND contentType = 'series'
        ORDER BY seasonNum ASC
    """)
    suspend fun getSeriesSeasons(playlistId: Long, seriesName: String): List<Int>

    /**
     * Get episodes for a series and season, ordered by episode number.
     */
    @Query("""
        SELECT * FROM channels 
        WHERE playlistId = :playlistId AND seriesName = :seriesName 
        AND seasonNum = :season AND contentType = 'series'
        ORDER BY episodeNum ASC, orderIndex ASC
    """)
    suspend fun getSeriesEpisodes(playlistId: Long, seriesName: String, season: Int): List<Channel>

    /**
     * Get all episodes for a series (all seasons), ordered.
     */
    @Query("""
        SELECT * FROM channels 
        WHERE playlistId = :playlistId AND seriesName = :seriesName 
        AND contentType = 'series'
        ORDER BY seasonNum ASC, episodeNum ASC, orderIndex ASC
    """)
    suspend fun getAllSeriesEpisodes(playlistId: Long, seriesName: String): List<Channel>

    /**
     * Get total episode count for a series.
     */
    @Query("""
        SELECT COUNT(*) FROM channels 
        WHERE playlistId = :playlistId AND seriesName = :seriesName 
        AND contentType = 'series'
    """)
    suspend fun getSeriesEpisodeCount(playlistId: Long, seriesName: String): Int

    /**
     * Get the first episode of a series (for cover image / poster).
     */
    @Query("""
        SELECT * FROM channels 
        WHERE playlistId = :playlistId AND seriesName = :seriesName 
        AND contentType = 'series'
        ORDER BY seasonNum ASC, episodeNum ASC
        LIMIT 1
    """)
    suspend fun getSeriesCoverChannel(playlistId: Long, seriesName: String): Channel?

    /**
     * Series info: name + episode count, for building the series folder list.
     */
    @Query("""
        SELECT seriesName, COUNT(*) as episodeCount, 
               MIN(logoUrl) as coverUrl,
               groupTitle
        FROM channels 
        WHERE playlistId = :playlistId AND contentType = 'series' 
        AND seriesName IS NOT NULL AND seriesName != ''
        GROUP BY seriesName
        ORDER BY seriesName ASC
    """)
    suspend fun getSeriesInfoList(playlistId: Long): List<SeriesInfo>

    /**
     * Series info within a group.
     */
    @Query("""
        SELECT seriesName, COUNT(*) as episodeCount, 
               MIN(logoUrl) as coverUrl,
               groupTitle
        FROM channels 
        WHERE playlistId = :playlistId AND contentType = 'series' 
        AND groupTitle = :group
        AND seriesName IS NOT NULL AND seriesName != ''
        GROUP BY seriesName
        ORDER BY seriesName ASC
    """)
    suspend fun getSeriesInfoForGroup(playlistId: Long, group: String): List<SeriesInfo>
}

data class GroupCount(
    val groupTitle: String,
    val cnt: Int
)

data class ContentTypeCount(
    val contentType: String,
    val cnt: Int
)

data class SeriesInfo(
    val seriesName: String,
    val episodeCount: Int,
    val coverUrl: String?,
    val groupTitle: String?
)
