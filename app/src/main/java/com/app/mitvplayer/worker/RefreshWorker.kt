package com.app.mitvplayer.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.app.mitvplayer.data.AppDatabase
import com.app.mitvplayer.data.M3UParser
import com.app.mitvplayer.data.PlaylistRepository
import com.app.mitvplayer.data.epg.EpgParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * WorkManager worker that periodically:
 * 1. Refreshes URL-based playlists older than 24 hours
 * 2. Refreshes EPG data if EPG URL is available
 * 3. Cleans up expired EPG entries
 */
class RefreshWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getInstance(applicationContext)
            val repository = PlaylistRepository(db.playlistDao(), db.channelDao())
            val epgDao = db.epgDao()

            // 1. Refresh URL-based playlists
            val allPlaylists = db.playlistDao().getAllPlaylistsSync()
            for (playlist in allPlaylists) {
                if (repository.shouldRefreshPlaylist(playlist)) {
                    try {
                        val url = playlist.url ?: continue
                        val connection = java.net.URL(url).openConnection().apply {
                            connectTimeout = 15_000
                            readTimeout = 30_000
                            setRequestProperty("User-Agent", "MiTVPlayer/1.0")
                        }
                        val content = connection.getInputStream().bufferedReader().readText()
                        val result = M3UParser.parse(content)
                        if (result.channels.isNotEmpty()) {
                            repository.refreshPlaylist(playlist.id, result)
                        }

                        // Refresh EPG if available
                        val epgUrl = result.epgUrl ?: playlist.epgUrl
                        if (!epgUrl.isNullOrBlank()) {
                            refreshEpg(epgUrl, epgDao)
                        }
                    } catch (_: Exception) {
                        // Continue with other playlists
                    }
                }
            }

            // 2. Clean up expired EPG entries (older than 24h)
            val cutoff = System.currentTimeMillis() - (24 * 3600 * 1000)
            epgDao.deleteOldPrograms(cutoff)

            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private suspend fun refreshEpg(epgUrl: String, epgDao: com.app.mitvplayer.data.dao.EpgDao) {
        try {
            EpgParser.parseFromUrl(epgUrl).collect { batch ->
                epgDao.insertPrograms(batch)
            }
        } catch (_: Exception) {
            // EPG refresh is best-effort
        }
    }
}
