package com.app.mitvplayer.data

import com.app.mitvplayer.data.dao.ChannelDao
import com.app.mitvplayer.data.dao.ContentTypeCount
import com.app.mitvplayer.data.dao.GroupCount
import com.app.mitvplayer.data.dao.PlaylistDao
import com.app.mitvplayer.data.dao.SeriesInfo
import com.app.mitvplayer.data.models.Channel
import com.app.mitvplayer.data.models.Playlist
import kotlinx.coroutines.flow.Flow

class PlaylistRepository(
    private val playlistDao: PlaylistDao,
    private val channelDao: ChannelDao
) {
    fun getAllPlaylists(): Flow<List<Playlist>> = playlistDao.getAllPlaylists()

    suspend fun getPlaylistById(id: Long): Playlist? = playlistDao.getPlaylistById(id)

    fun getChannelsForPlaylist(playlistId: Long): Flow<List<Channel>> =
        channelDao.getChannelsForPlaylist(playlistId)

    fun getChannelsByGroup(playlistId: Long, group: String): Flow<List<Channel>> =
        channelDao.getChannelsByGroup(playlistId, group)

    fun getChannelsWithoutGroup(playlistId: Long): Flow<List<Channel>> =
        channelDao.getChannelsWithoutGroup(playlistId)

    fun getGroupsForPlaylist(playlistId: Long): Flow<List<String>> =
        channelDao.getGroupsForPlaylist(playlistId)

    suspend fun getGroupsWithCounts(playlistId: Long): List<GroupCount> =
        channelDao.getGroupsWithCounts(playlistId)

    suspend fun getUncategorizedCount(playlistId: Long): Int =
        channelDao.getUncategorizedCount(playlistId)

    suspend fun getChannelById(channelId: Long): Channel? =
        channelDao.getChannelById(channelId)

    suspend fun searchChannels(playlistId: Long, query: String, limit: Int = 100): List<Channel> =
        channelDao.searchChannels(playlistId, query.lowercase(), limit)

    // ═══════════════════════════════════════════════════════
    // Default playlist
    // ═══════════════════════════════════════════════════════

    suspend fun getDefaultPlaylist(): Playlist? = playlistDao.getDefaultPlaylist()

    suspend fun setDefaultPlaylist(id: Long) {
        playlistDao.clearDefaultPlaylist()
        playlistDao.setDefaultPlaylist(id)
    }

    suspend fun clearDefaultPlaylist() {
        playlistDao.clearDefaultPlaylist()
    }

    // ═══════════════════════════════════════════════════════
    // Content type queries
    // ═══════════════════════════════════════════════════════

    suspend fun getContentTypeCounts(playlistId: Long): List<ContentTypeCount> =
        channelDao.getContentTypeCounts(playlistId)

    suspend fun getGroupsWithCountsByContentType(playlistId: Long, contentType: String): List<GroupCount> =
        channelDao.getGroupsWithCountsByContentType(playlistId, contentType)

    fun getChannelsByContentTypeAndGroup(playlistId: Long, contentType: String, group: String): Flow<List<Channel>> =
        channelDao.getChannelsByContentTypeAndGroup(playlistId, contentType, group)

    // ═══════════════════════════════════════════════════════
    // Series queries
    // ═══════════════════════════════════════════════════════

    suspend fun getSeriesInfoList(playlistId: Long): List<SeriesInfo> =
        channelDao.getSeriesInfoList(playlistId)

    suspend fun getSeriesInfoForGroup(playlistId: Long, group: String): List<SeriesInfo> =
        channelDao.getSeriesInfoForGroup(playlistId, group)

    suspend fun getSeriesSeasons(playlistId: Long, seriesName: String): List<Int> =
        channelDao.getSeriesSeasons(playlistId, seriesName)

    suspend fun getSeriesEpisodes(playlistId: Long, seriesName: String, season: Int): List<Channel> =
        channelDao.getSeriesEpisodes(playlistId, seriesName, season)

    suspend fun getAllSeriesEpisodes(playlistId: Long, seriesName: String): List<Channel> =
        channelDao.getAllSeriesEpisodes(playlistId, seriesName)

    suspend fun getSeriesEpisodeCount(playlistId: Long, seriesName: String): Int =
        channelDao.getSeriesEpisodeCount(playlistId, seriesName)

    // ═══════════════════════════════════════════════════════
    // Import & Refresh
    // ═══════════════════════════════════════════════════════

    suspend fun importPlaylist(
        name: String,
        parseResult: M3UParser.ParseResult,
        sourceUrl: String? = null
    ): Long {
        val playlist = Playlist(
            name = name,
            url = sourceUrl,
            channelCount = parseResult.channels.size,
            epgUrl = parseResult.epgUrl,
            lastUpdatedAt = System.currentTimeMillis(),
            sourceType = if (sourceUrl != null) "url" else "file"
        )
        val playlistId = playlistDao.insertPlaylist(playlist)

        insertChannelsBatched(playlistId, parseResult.channels)

        return playlistId
    }

    /**
     * Refresh an existing URL playlist: re-download, re-parse, and replace channels.
     * Returns the new channel count.
     */
    suspend fun refreshPlaylist(playlistId: Long, parseResult: M3UParser.ParseResult): Int {
        // Delete old channels
        channelDao.deleteChannelsForPlaylist(playlistId)

        // Insert new channels
        insertChannelsBatched(playlistId, parseResult.channels)

        // Update playlist metadata
        playlistDao.updateLastUpdated(
            id = playlistId,
            timestamp = System.currentTimeMillis(),
            channelCount = parseResult.channels.size
        )

        return parseResult.channels.size
    }

    /**
     * Check if a playlist should be auto-refreshed (older than given hours).
     */
    fun shouldRefreshPlaylist(playlist: Playlist, maxAgeHours: Int = 24): Boolean {
        if (playlist.sourceType != "url" || playlist.url.isNullOrBlank()) return false
        val ageMs = System.currentTimeMillis() - playlist.lastUpdatedAt
        val maxAgeMs = maxAgeHours.toLong() * 3600 * 1000
        return ageMs > maxAgeMs
    }

    private suspend fun insertChannelsBatched(playlistId: Long, parsedChannels: List<M3UParser.ParsedChannel>) {
        val channels = parsedChannels.mapIndexed { index, parsed ->
            Channel(
                playlistId = playlistId,
                name = parsed.name,
                url = parsed.url,
                logoUrl = parsed.logoUrl,
                groupTitle = parsed.groupTitle,
                tvgId = parsed.tvgId,
                tvgName = parsed.tvgName,
                duration = parsed.duration,
                orderIndex = index,
                httpReferrer = parsed.httpReferrer,
                httpUserAgent = parsed.httpUserAgent,
                contentType = parsed.contentType,
                seriesName = parsed.seriesName,
                seasonNum = parsed.seasonNum,
                episodeNum = parsed.episodeNum
            )
        }

        // Insert in batches of 1000 for large playlists (optimized for 50K+)
        channels.chunked(1000).forEach { batch ->
            channelDao.insertChannels(batch)
        }
    }

    suspend fun deletePlaylist(playlistId: Long) {
        playlistDao.deletePlaylist(playlistId)
    }

    suspend fun deleteAllPlaylists() {
        playlistDao.deleteAllPlaylists()
    }
}
