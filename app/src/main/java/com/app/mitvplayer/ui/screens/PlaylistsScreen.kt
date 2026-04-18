package com.app.mitvplayer.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.app.mitvplayer.ui.components.TVButton
import com.app.mitvplayer.ui.components.dpadClickable
import com.app.mitvplayer.ui.theme.CardBackground
import com.app.mitvplayer.ui.theme.CardFocused
import com.app.mitvplayer.ui.theme.DarkBackground
import com.app.mitvplayer.ui.theme.ErrorRed
import com.app.mitvplayer.ui.theme.PrimaryIndigo
import com.app.mitvplayer.ui.theme.PrimaryIndigoLight
import com.app.mitvplayer.ui.theme.SuccessGreen
import com.app.mitvplayer.ui.theme.TextDimmed
import com.app.mitvplayer.ui.theme.TextGray
import com.app.mitvplayer.ui.theme.TextWhite
import com.app.mitvplayer.viewmodel.PlaylistViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PlaylistsScreen(
    navController: NavController,
    viewModel: PlaylistViewModel
) {
    val playlists by viewModel.playlists.collectAsState()
    val refreshState by viewModel.refreshState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 32.dp, vertical = 24.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            TVButton(
                text = "Volver",
                icon = Icons.Default.ArrowBack,
                onClick = { navController.popBackStack() }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Mis Listas",
                style = MaterialTheme.typography.headlineMedium,
                color = TextWhite,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Refresh status banner
        when (val state = refreshState) {
            is PlaylistViewModel.RefreshState.Loading -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = PrimaryIndigo.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = PrimaryIndigoLight,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Actualizando lista…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PrimaryIndigoLight
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            is PlaylistViewModel.RefreshState.Success -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "✓ Lista actualizada: ${state.newChannelCount} canales",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = SuccessGreen
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            is PlaylistViewModel.RefreshState.Error -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "❌ ${state.message}",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = ErrorRed
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            else -> {}
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (playlists.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.PlaylistPlay,
                        contentDescription = null,
                        tint = TextGray,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No hay listas guardadas",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Importa una lista M3U para comenzar",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextDimmed
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    TVButton(
                        text = "Importar Lista",
                        onClick = { navController.navigate("import") }
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(playlists) { playlist ->
                    var showDeleteConfirm by remember { mutableStateOf(false) }
                    val isRefreshing = refreshState is PlaylistViewModel.RefreshState.Loading &&
                        (refreshState as PlaylistViewModel.RefreshState.Loading).playlistId == playlist.id

                    if (showDeleteConfirm) {
                        DeleteConfirmDialog(
                            playlistName = playlist.name,
                            onConfirm = {
                                viewModel.deletePlaylist(playlist.id)
                                showDeleteConfirm = false
                            },
                            onCancel = { showDeleteConfirm = false }
                        )
                    }

                    PlaylistCard(
                        name = playlist.name,
                        channelCount = playlist.channelCount,
                        date = playlist.createdAt,
                        lastUpdated = playlist.lastUpdatedAt,
                        hasUrl = !playlist.url.isNullOrBlank(),
                        isRefreshing = isRefreshing,
                        onClick = {
                            navController.navigate("playlist/${playlist.id}")
                        },
                        onRefresh = {
                            viewModel.refreshPlaylist(playlist.id)
                        },
                        onDelete = { showDeleteConfirm = true }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistCard(
    name: String,
    channelCount: Int,
    date: Long,
    lastUpdated: Long,
    hasUrl: Boolean,
    isRefreshing: Boolean,
    onClick: () -> Unit,
    onRefresh: () -> Unit,
    onDelete: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.02f else 1f,
        label = "scale"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) PrimaryIndigo else Color.Transparent,
        label = "border"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isFocused) CardFocused else CardBackground,
        label = "bg"
    )

    val dateStr = remember(date) {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(date))
    }
    val updatedStr = remember(lastUpdated) {
        val diff = System.currentTimeMillis() - lastUpdated
        val hours = diff / (3600 * 1000)
        val minutes = diff / (60 * 1000)
        when {
            minutes < 1 -> "Hace un momento"
            minutes < 60 -> "Hace ${minutes}min"
            hours < 24 -> "Hace ${hours}h"
            else -> {
                val days = hours / 24
                "Hace ${days}d"
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .dpadClickable(
                onClick = onClick,
                onFocusChange = { isFocused = it }
            )
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.Delete) {
                    onDelete()
                    true
                } else false
            },
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.PlaylistPlay,
                contentDescription = null,
                tint = if (isFocused) PrimaryIndigoLight else TextGray,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isFocused) PrimaryIndigoLight else TextWhite,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$channelCount canales  •  $dateStr",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextGray
                )
                Text(
                    text = "Actualizado: $updatedStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextDimmed
                )
            }

            // Refresh button for URL playlists
            if (hasUrl && isFocused) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = PrimaryIndigoLight,
                        strokeWidth = 2.dp
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                PrimaryIndigo.copy(alpha = 0.3f),
                                RoundedCornerShape(8.dp)
                            )
                            .dpadClickable(onClick = onRefresh),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Actualizar",
                            tint = PrimaryIndigoLight,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            if (isFocused) {
                Text(
                    text = "DEL borrar",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextDimmed
                )
            }
        }
    }
}

@Composable
private fun DeleteConfirmDialog(
    playlistName: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = ErrorRed,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "¿Eliminar lista?",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextWhite,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "\"$playlistName\" y sus canales\nserán eliminados permanentemente.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextGray
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    TVButton(
                        text = "Cancelar",
                        onClick = onCancel
                    )
                    TVButton(
                        text = "Eliminar",
                        icon = Icons.Default.Delete,
                        onClick = onConfirm
                    )
                }
            }
        }
    }
}
