package com.app.mitvplayer.data

import com.app.mitvplayer.data.dao.ChannelDao
import com.app.mitvplayer.data.dao.GroupCount
import com.app.mitvplayer.data.dao.PlaylistDao
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
                httpUserAgent = parsed.httpUserAgent
            )
        }

        // Insert in batches of 500 for large playlists
        channels.chunked(500).forEach { batch ->
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
