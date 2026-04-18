package com.app.mitvplayer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.app.mitvplayer.ui.screens.ChannelListScreen
import com.app.mitvplayer.ui.screens.DefaultPlaylistScreen
import com.app.mitvplayer.ui.screens.DirectUrlScreen
import com.app.mitvplayer.ui.screens.FavoritesScreen
import com.app.mitvplayer.ui.screens.HomeScreen
import com.app.mitvplayer.ui.screens.ImportScreen
import com.app.mitvplayer.ui.screens.PlayerScreen
import com.app.mitvplayer.ui.screens.PlaylistsScreen
import com.app.mitvplayer.ui.screens.SeriesDetailScreen
import com.app.mitvplayer.ui.screens.SettingsScreen
import com.app.mitvplayer.ui.screens.XtreamLoginScreen
import com.app.mitvplayer.viewmodel.PlaylistViewModel
import java.net.URLDecoder

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val viewModel: PlaylistViewModel = viewModel()

    // Dynamic start destination based on default playlist
    val defaultPlaylistId by viewModel.defaultPlaylistId.collectAsState()
    val startDest = if (defaultPlaylistId != null) "default_playlist" else "home"

    NavHost(
        navController = navController,
        startDestination = startDest
    ) {
        composable("home") {
            HomeScreen(navController = navController)
        }

        composable("playlists") {
            PlaylistsScreen(
                navController = navController,
                viewModel = viewModel
            )
        }

        // ── Default playlist view (3 tabs: TV / Movies / Series) ──
        composable("default_playlist") {
            val pid = defaultPlaylistId
            if (pid != null) {
                DefaultPlaylistScreen(
                    navController = navController,
                    viewModel = viewModel,
                    playlistId = pid
                )
            } else {
                // Fallback if no default set
                HomeScreen(navController = navController)
            }
        }

        composable(
            route = "playlist/{playlistId}",
            arguments = listOf(
                navArgument("playlistId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getLong("playlistId") ?: 0L
            ChannelListScreen(
                navController = navController,
                viewModel = viewModel,
                playlistId = playlistId
            )
        }

        composable(
            route = "player/{playlistId}/{channelId}",
            arguments = listOf(
                navArgument("playlistId") { type = NavType.LongType },
                navArgument("channelId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getLong("playlistId") ?: 0L
            val channelId = backStackEntry.arguments?.getLong("channelId") ?: 0L
            PlayerScreen(
                navController = navController,
                viewModel = viewModel,
                playlistId = playlistId,
                initialChannelId = channelId
            )
        }

        // ── Series detail (folder view) ──
        composable(
            route = "series/{playlistId}/{seriesName}",
            arguments = listOf(
                navArgument("playlistId") { type = NavType.LongType },
                navArgument("seriesName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getLong("playlistId") ?: 0L
            val seriesNameEncoded = backStackEntry.arguments?.getString("seriesName") ?: ""
            val seriesName = try {
                URLDecoder.decode(seriesNameEncoded, "UTF-8")
            } catch (_: Exception) { seriesNameEncoded }

            SeriesDetailScreen(
                navController = navController,
                viewModel = viewModel,
                playlistId = playlistId,
                seriesName = seriesName
            )
        }

        composable("import") {
            ImportScreen(
                navController = navController,
                viewModel = viewModel
            )
        }

        composable("direct_url") {
            DirectUrlScreen(navController = navController)
        }

        composable("settings") {
            SettingsScreen(
                navController = navController,
                viewModel = viewModel
            )
        }

        composable("xtream_login") {
            XtreamLoginScreen(
                navController = navController,
                viewModel = viewModel
            )
        }

        composable("favorites") {
            FavoritesScreen(
                navController = navController,
                viewModel = viewModel
            )
        }
    }
}
