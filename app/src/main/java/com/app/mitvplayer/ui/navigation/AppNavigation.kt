package com.app.mitvplayer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.app.mitvplayer.ui.screens.ChannelListScreen
import com.app.mitvplayer.ui.screens.DirectUrlScreen
import com.app.mitvplayer.ui.screens.FavoritesScreen
import com.app.mitvplayer.ui.screens.HomeScreen
import com.app.mitvplayer.ui.screens.ImportScreen
import com.app.mitvplayer.ui.screens.PlayerScreen
import com.app.mitvplayer.ui.screens.PlaylistsScreen
import com.app.mitvplayer.ui.screens.SettingsScreen
import com.app.mitvplayer.ui.screens.XtreamLoginScreen
import com.app.mitvplayer.viewmodel.PlaylistViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val viewModel: PlaylistViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = "home"
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

        // ── New routes ──

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
