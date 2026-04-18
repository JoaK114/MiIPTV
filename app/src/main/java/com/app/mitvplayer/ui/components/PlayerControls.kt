package com.app.mitvplayer.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.app.mitvplayer.player.TrackInfo
import com.app.mitvplayer.ui.theme.CardBackground
import com.app.mitvplayer.ui.theme.DarkSurface
import com.app.mitvplayer.ui.theme.ErrorRed
import com.app.mitvplayer.ui.theme.PrimaryIndigo
import com.app.mitvplayer.ui.theme.PrimaryIndigoLight
import com.app.mitvplayer.ui.theme.TextDimmed
import com.app.mitvplayer.ui.theme.TextGray
import com.app.mitvplayer.ui.theme.TextWhite

@Composable
fun PlayerControls(
    channelName: String,
    channelNumber: Int,
    totalChannels: Int,
    channelGroup: String?,
    channelLogo: String?,
    isPlaying: Boolean,
    isBuffering: Boolean,
    currentPosition: Long,
    duration: Long,
    errorMessage: String?,
    audioTracks: List<TrackInfo>,
    subtitleTracks: List<TrackInfo>,
    onPlayPause: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onPrevChannel: () -> Unit,
    onNextChannel: () -> Unit,
    onBack: () -> Unit,
    onSelectAudio: (TrackInfo) -> Unit,
    onSelectSubtitle: (TrackInfo?) -> Unit,
    modifier: Modifier = Modifier
) {
    // Panel state: none, audio, subtitles
    var activePanel by remember { mutableStateOf<String?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        // ══════════════════════════════════════════════════
        // TOP BAR — Channel info + Back button
        // ══════════════════════════════════════════════════
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.85f),
                            Color.Black.copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    )
                )
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Back button
                ControlIconButton(
                    icon = Icons.Default.ArrowBack,
                    label = "Volver",
                    onClick = onBack
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Channel logo
                if (!channelLogo.isNullOrBlank()) {
                    Card(
                        modifier = Modifier.size(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface)
                    ) {
                        AsyncImage(
                            model = channelLogo,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(6.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }

                // Channel info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = channelName,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row {
                        Text(
                            text = "Canal $channelNumber de $totalChannels",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextDimmed
                        )
                        if (!channelGroup.isNullOrBlank()) {
                            Text(
                                text = "  •  $channelGroup",
                                style = MaterialTheme.typography.bodySmall,
                                color = PrimaryIndigoLight
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Audio tracks button
                if (audioTracks.size > 1) {
                    ControlIconButton(
                        icon = Icons.Default.Translate,
                        label = "Idioma",
                        isActive = activePanel == "audio",
                        onClick = {
                            activePanel = if (activePanel == "audio") null else "audio"
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // Subtitle button
                if (subtitleTracks.isNotEmpty()) {
                    ControlIconButton(
                        icon = Icons.Default.Subtitles,
                        label = "Subtítulos",
                        isActive = activePanel == "subs",
                        onClick = {
                            activePanel = if (activePanel == "subs") null else "subs"
                        }
                    )
                }
            }
        }

        // ══════════════════════════════════════════════════
        // CENTER — Buffering / Error / Play controls
        // ══════════════════════════════════════════════════
        if (errorMessage != null) {
            // Error display
            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(48.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.95f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = ErrorRed,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextWhite
                    )
                }
            }
        } else if (isBuffering) {
            // Buffering spinner
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(56.dp),
                    color = PrimaryIndigoLight,
                    strokeWidth = 3.dp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Cargando...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextWhite
                )
            }
        } else {
            // Playback controls (Netflix-style centered)
            Row(
                modifier = Modifier.align(Alignment.Center),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Previous channel
                ControlIconButton(
                    icon = Icons.Default.SkipPrevious,
                    label = "Anterior",
                    size = 44,
                    onClick = onPrevChannel
                )

                // Rewind 10s
                ControlIconButton(
                    icon = Icons.Default.Replay10,
                    label = "-10s",
                    size = 48,
                    onClick = onSeekBack
                )

                // Play / Pause (large)
                PlayPauseButton(
                    isPlaying = isPlaying,
                    onClick = onPlayPause
                )

                // Forward 10s
                ControlIconButton(
                    icon = Icons.Default.Forward10,
                    label = "+10s",
                    size = 48,
                    onClick = onSeekForward
                )

                // Next channel
                ControlIconButton(
                    icon = Icons.Default.SkipNext,
                    label = "Siguiente",
                    size = 44,
                    onClick = onNextChannel
                )
            }
        }

        // ══════════════════════════════════════════════════
        // BOTTOM BAR — Progress bar + time
        // ══════════════════════════════════════════════════
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.4f),
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Column {
                if (duration > 0) {
                    // Progress bar for VOD
                    val progress = (currentPosition.toFloat() / duration.toFloat())
                        .coerceIn(0f, 1f)

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = PrimaryIndigo,
                        trackColor = TextDimmed.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTime(currentPosition),
                            style = MaterialTheme.typography.labelMedium,
                            color = TextGray
                        )
                        Text(
                            text = formatTime(duration),
                            style = MaterialTheme.typography.labelMedium,
                            color = TextGray
                        )
                    }
                } else {
                    // Live indicator
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(ErrorRed)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "EN VIVO",
                            style = MaterialTheme.typography.labelLarge,
                            color = ErrorRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // ══════════════════════════════════════════════════
        // SIDE PANEL — Audio or Subtitle selection
        // ══════════════════════════════════════════════════
        AnimatedVisibility(
            visible = activePanel != null,
            modifier = Modifier.align(Alignment.CenterEnd),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .width(280.dp)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = CardBackground.copy(alpha = 0.95f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (activePanel == "audio") "Idioma de audio" else "Subtítulos",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    val tracks = if (activePanel == "audio") audioTracks else subtitleTracks

                    // "Off" option for subtitles
                    if (activePanel == "subs") {
                        val noSubSelected = subtitleTracks.none { it.isSelected }
                        TrackItem(
                            label = "Desactivados",
                            isSelected = noSubSelected,
                            onClick = {
                                onSelectSubtitle(null)
                                activePanel = null
                            }
                        )
                    }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(tracks) { track ->
                            TrackItem(
                                label = track.label,
                                isSelected = track.isSelected,
                                onClick = {
                                    if (activePanel == "audio") {
                                        onSelectAudio(track)
                                    } else {
                                        onSelectSubtitle(track)
                                    }
                                    activePanel = null
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// Control icon button (reusable)
// ═══════════════════════════════════════════════════════
@Composable
private fun ControlIconButton(
    icon: ImageVector,
    label: String,
    size: Int = 40,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.2f else 1f, label = "scale"
    )
    val bgColor by animateColorAsState(
        targetValue = when {
            isActive -> PrimaryIndigo
            isFocused -> PrimaryIndigo.copy(alpha = 0.4f)
            else -> Color.White.copy(alpha = 0.1f)
        },
        label = "bg"
    )

    Box(
        modifier = Modifier
            .size(size.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(bgColor)
            .dpadClickable(
                onClick = onClick,
                onFocusChange = { isFocused = it }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = TextWhite,
            modifier = Modifier.size((size * 0.55f).dp)
        )
    }
}

// ═══════════════════════════════════════════════════════
// Large play/pause button
// ═══════════════════════════════════════════════════════
@Composable
private fun PlayPauseButton(
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.15f else 1f, label = "ppScale"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isFocused) PrimaryIndigo else Color.White.copy(alpha = 0.15f),
        label = "ppBg"
    )

    Box(
        modifier = Modifier
            .size(72.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(bgColor)
            .dpadClickable(
                onClick = onClick,
                onFocusChange = { isFocused = it }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = if (isPlaying) "Pausar" else "Reproducir",
            tint = TextWhite,
            modifier = Modifier.size(40.dp)
        )
    }
}

// ═══════════════════════════════════════════════════════
// Track selection item
// ═══════════════════════════════════════════════════════
@Composable
private fun TrackItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val bgColor by animateColorAsState(
        targetValue = when {
            isSelected -> PrimaryIndigo.copy(alpha = 0.3f)
            isFocused -> PrimaryIndigo.copy(alpha = 0.15f)
            else -> Color.Transparent
        },
        label = "trackBg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .dpadClickable(
                onClick = onClick,
                onFocusChange = { isFocused = it }
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSelected) PrimaryIndigoLight else TextWhite,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = PrimaryIndigoLight,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}
