package com.app.mitvplayer.data

import com.app.mitvplayer.data.api.XtreamApiService
import com.app.mitvplayer.data.dao.ChannelDao
import com.app.mitvplayer.data.dao.XtreamDao
import com.app.mitvplayer.data.models.Channel
import com.app.mitvplayer.data.models.Playlist
import com.app.mitvplayer.data.models.XtreamAccount
import com.app.mitvplayer.data.models.XtreamAuthResponse
import com.app.mitvplayer.data.models.XtreamCategory
import com.app.mitvplayer.data.dao.PlaylistDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repository that bridges Xtream Codes API data into the existing
 * Playlist/Channel Room model, enabling unified display in the same UI.
 */
class XtreamRepository(
    private val xtreamDao: XtreamDao,
    private val playlistDao: PlaylistDao,
    private val channelDao: ChannelDao
) {
    fun getAllAccounts(): Flow<List<XtreamAccount>> = xtreamDao.getAllAccounts()

    suspend fun getAccountById(id: Long): XtreamAccount? = xtreamDao.getAccountById(id)

    /**
     * Authenticate with an Xtream Codes server and save/update the account.
     * Returns the account ID.
     */
    suspend fun loginAndSave(
        serverUrl: String,
        username: String,
        password: String
    ): Pair<Long, XtreamAuthResponse> = withContext(Dispatchers.IO) {
        val api = XtreamApiService(serverUrl, username, password)
        val authResponse = api.authenticate()
        val userInfo = authResponse.userInfo
            ?: throw Exception("Respuesta de autenticación inválida")

        if (userInfo.status?.lowercase() != "active") {
            throw Exception("Cuenta no activa. Estado: ${userInfo.status ?: "unknown"}")
        }

        // Check if account already exists
        val existing = xtreamDao.findAccount(serverUrl, username)
        val accountId = if (existing != null) {
            val updated = existing.copy(
                password = password,
                status = userInfo.status ?: "Active",
                expirationDate = userInfo.expDate?.toLongOrNull() ?: 0L,
                maxConnections = userInfo.maxConnections?.toIntOrNull() ?: 1,
                activeCons = userInfo.activeCons?.toIntOrNull() ?: 0,
                lastLoginAt = System.currentTimeMillis()
            )
            xtreamDao.updateAccount(updated)
            existing.id
        } else {
            val account = XtreamAccount(
                serverUrl = serverUrl,
                username = username,
                password = password,
                name = "$username@${extractHost(serverUrl)}",
                status = userInfo.status ?: "Active",
                expirationDate = userInfo.expDate?.toLongOrNull() ?: 0L,
                maxConnections = userInfo.maxConnections?.toIntOrNull() ?: 1,
                activeCons = userInfo.activeCons?.toIntOrNull() ?: 0
            )
            xtreamDao.insertAccount(account)
        }

        Pair(accountId, authResponse)
    }

    /**
     * Import all live streams from Xtream Codes into a local playlist.
     * Returns the playlist ID and channel count.
     */
    suspend fun importLiveStreams(account: XtreamAccount): Pair<Long, Int> = withContext(Dispatchers.IO) {
        val api = XtreamApiService(account.serverUrl, account.username, account.password)

        // Fetch categories for group naming
        val categories = try { api.getLiveCategories() } catch (_: Exception) { emptyList() }
        val categoryMap = categories.associate { (it.categoryId ?: "") to (it.categoryName ?: "Sin Categoría") }

        // Fetch all live streams
        val streams = api.getLiveStreams()

        // Create or find playlist
        val playlistName = "Xtream: ${account.name} (TV)"
        val playlist = Playlist(
            name = playlistName,
            url = account.serverUrl,
            channelCount = streams.size,
            sourceType = "xtream",
            lastUpdatedAt = System.currentTimeMillis()
        )
        val playlistId = playlistDao.insertPlaylist(playlist)

        // Convert streams to channels
        val channels = streams.mapIndexed { index, stream ->
            Channel(
                playlistId = playlistId,
                name = stream.name ?: "Canal ${index + 1}",
                url = api.buildLiveStreamUrl(stream.streamId ?: 0),
                logoUrl = stream.streamIcon,
                groupTitle = categoryMap[stream.categoryId] ?: "Sin Categoría",
                tvgId = stream.epgChannelId,
                tvgName = stream.name,
                orderIndex = index
            )
        }

        // Batch insert
        channels.chunked(500).forEach { batch ->
            channelDao.insertChannels(batch)
        }

        Pair(playlistId, channels.size)
    }

    /**
     * Import VOD streams from Xtream Codes into a local playlist.
     */
    suspend fun importVodStreams(account: XtreamAccount): Pair<Long, Int> = withContext(Dispatchers.IO) {
        val api = XtreamApiService(account.serverUrl, account.username, account.password)

        val categories = try { api.getVodCategories() } catch (_: Exception) { emptyList() }
        val categoryMap = categories.associate { (it.categoryId ?: "") to (it.categoryName ?: "Películas") }

        val streams = api.getVodStreams()

        val playlistName = "Xtream: ${account.name} (VOD)"
        val playlist = Playlist(
            name = playlistName,
            url = account.serverUrl,
            channelCount = streams.size,
            sourceType = "xtream",
            lastUpdatedAt = System.currentTimeMillis()
        )
        val playlistId = playlistDao.insertPlaylist(playlist)

        val channels = streams.mapIndexed { index, stream ->
            Channel(
                playlistId = playlistId,
                name = stream.name ?: "Película ${index + 1}",
                url = api.buildVodStreamUrl(
                    stream.streamId ?: 0,
                    stream.containerExtension ?: "mp4"
                ),
                logoUrl = stream.streamIcon,
                groupTitle = "VOD | ${categoryMap[stream.categoryId] ?: "Películas"}",
                orderIndex = index
            )
        }

        channels.chunked(500).forEach { batch ->
            channelDao.insertChannels(batch)
        }

        Pair(playlistId, channels.size)
    }

    /**
     * Import series from Xtream Codes into a local playlist.
     * Each episode becomes a channel.
     */
    suspend fun importSeries(account: XtreamAccount): Pair<Long, Int> = withContext(Dispatchers.IO) {
        val api = XtreamApiService(account.serverUrl, account.username, account.password)

        val categories = try { api.getSeriesCategories() } catch (_: Exception) { emptyList() }
        val categoryMap = categories.associate { (it.categoryId ?: "") to (it.categoryName ?: "Series") }

        val seriesList = api.getSeries()

        val playlistName = "Xtream: ${account.name} (Series)"
        val playlist = Playlist(
            name = playlistName,
            url = account.serverUrl,
            channelCount = 0, // Updated after loading episodes
            sourceType = "xtream",
            lastUpdatedAt = System.currentTimeMillis()
        )
        val playlistId = playlistDao.insertPlaylist(playlist)

        var totalEpisodes = 0

        // For each series, fetch episodes and add as channels
        for (series in seriesList) {
            val seriesId = series.seriesId ?: continue
            val seriesInfo = try {
                api.getSeriesInfo(seriesId)
            } catch (_: Exception) {
                continue
            }

            val episodes = seriesInfo.episodes ?: continue
            val groupName = "Series | ${categoryMap[series.categoryId] ?: "Series"} | ${series.name ?: "Serie"}"

            for ((_, episodeList) in episodes) {
                val channels = episodeList.mapIndexed { index, episode ->
                    val epNum = episode.episodeNum ?: (index + 1)
                    val season = episode.season ?: 1
                    Channel(
                        playlistId = playlistId,
                        name = "${series.name ?: "Serie"} S${season}E${epNum} - ${episode.title ?: "Episodio $epNum"}",
                        url = api.buildSeriesEpisodeUrl(
                            episode.id ?: "0",
                            episode.containerExtension ?: "mp4"
                        ),
                        logoUrl = episode.info?.movieImage ?: series.cover,
                        groupTitle = groupName,
                        orderIndex = totalEpisodes + index
                    )
                }

                if (channels.isNotEmpty()) {
                    channelDao.insertChannels(channels)
                    totalEpisodes += channels.size
                }
            }
        }

        // Update channel count
        playlistDao.updateLastUpdated(playlistId, System.currentTimeMillis(), totalEpisodes)

        Pair(playlistId, totalEpisodes)
    }

    suspend fun deleteAccount(id: Long) = xtreamDao.deleteAccount(id)

    private fun extractHost(url: String): String {
        return try {
            java.net.URL(url).host
        } catch (_: Exception) {
            url
        }
    }
}
