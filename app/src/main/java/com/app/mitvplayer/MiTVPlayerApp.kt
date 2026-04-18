package com.app.mitvplayer

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.app.mitvplayer.worker.RefreshWorker
import java.util.concurrent.TimeUnit

class MiTVPlayerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initWorkManager()
    }

    private fun initWorkManager() {
        // Periodic refresh every 24 hours (playlists + EPG)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val refreshRequest = PeriodicWorkRequestBuilder<RefreshWorker>(
            24, TimeUnit.HOURS,
            15, TimeUnit.MINUTES // flex window
        )
            .setConstraints(constraints)
            .setInitialDelay(1, TimeUnit.HOURS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "playlist_epg_refresh",
            ExistingPeriodicWorkPolicy.KEEP,
            refreshRequest
        )
    }
}
