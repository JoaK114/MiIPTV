package com.app.mitvplayer.ui.screens

import android.view.KeyEvent
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import com.app.mitvplayer.data.models.Channel
import com.app.mitvplayer.player.TVPlayerManager
import com.app.mitvplayer.player.TrackInfo
import com.app.mitvplayer.ui.components.PlayerControls
import com.app.mitvplayer.viewmodel.PlaylistViewModel
import kotlinx.coroutines.delay

@Composable
fun PlayerScreen(
    navController: NavController,
    viewModel: PlaylistViewModel,
    playlistId: Long,
    initialChannelId: Long
) {
    val channels by viewModel.getChannelsForPlaylist(playlistId)
        .collectAsState(initial = emptyList())

    // Find initial index from channelId once channels are loaded
    var currentIndex by remember { mutableIntStateOf(0) }
    var hasInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(channels, initialChannelId) {
        if (channels.isNotEmpty() && !hasInitialized) {
            val idx = channels.indexOfFirst { it.id == initialChannelId }
            if (idx >= 0) {
                currentIndex = idx
            }
            hasInitialized = true
        }
    }

    var showOverlay by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Track info
    var audioTracks by remember { mutableStateOf<List<TrackInfo>>(emptyList()) }
    var subtitleTracks by remember { mutableStateOf<List<TrackInfo>>(emptyList()) }

    val context = LocalContext.current
    val playerManager = remember { TVPlayerManager(context) }
    val focusRequester = remember { FocusRequester() }

    // Setup error listener
    LaunchedEffect(Unit) {
        playerManager.setOnErrorListener { msg ->
            errorMessage = msg
        }
    }

    // Request focus for key events
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Play current channel
    LaunchedEffect(currentIndex, channels, hasInitialized) {
        if (channels.isNotEmpty() && currentIndex in channels.indices && hasInitialized) {
            val ch = channels[currentIndex]
            errorMessage = null
            isBuffering = true
            audioTracks = emptyList()
            subtitleTracks = emptyList()
            playerManager.playUrl(ch.url, ch.httpReferrer, ch.httpUserAgent)
        }
    }

    // Update player state and tracks periodically
    LaunchedEffect(Unit) {
        while (true) {
            val player = playerManager.player
            isPlaying = player.isPlaying
            isBuffering = player.playbackState == Player.STATE_BUFFERING
            currentPosition = player.currentPosition
            duration = player.duration.coerceAtLeast(0)

            // Refresh available tracks when player is ready
            if (player.playbackState == Player.STATE_READY) {
                audioTracks = playerManager.getAudioTracks()
                subtitleTracks = playerManager.getSubtitleTracks()
            }

            delay(500)
        }
    }

    // Auto-hide overlay
    LaunchedEffect(showOverlay) {
        if (showOverlay) {
            delay(8000)
            showOverlay = false
        }
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose { playerManager.release() }
    }

    // Helper lambdas
    val goToPrev = {
        if (channels.isNotEmpty() && currentIndex > 0) {
            currentIndex--
            showOverlay = true
        }
    }
    val goToNext = {
        if (channels.isNotEmpty() && currentIndex < channels.size - 1) {
            currentIndex++
            showOverlay = true
        }
    }
    val seekBack = {
        playerManager.seekRelative(-10_000)
        showOverlay = true
    }
    val seekFwd = {
        playerManager.seekRelative(10_000)
        showOverlay = true
    }
    val togglePlay = {
        playerManager.togglePlayPause()
        showOverlay = true
    }
    val goBack = { navController.popBackStack() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                showOverlay = !showOverlay
            }
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_UP,
                        KeyEvent.KEYCODE_CHANNEL_UP -> {
                            goToPrev()
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_DOWN,
                        KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                            goToNext()
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_LEFT,
                        KeyEvent.KEYCODE_MEDIA_REWIND -> {
                            seekBack()
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT,
                        KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                            seekFwd()
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_ENTER,
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                            if (showOverlay) {
                                togglePlay()
                            } else {
                                showOverlay = true
                            }
                            true
                        }
                        KeyEvent.KEYCODE_MEDIA_PLAY -> {
                            playerManager.player.play()
                            showOverlay = true
                            true
                        }
                        KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                            playerManager.player.pause()
                            showOverlay = true
                            true
                        }
                        KeyEvent.KEYCODE_BACK,
                        KeyEvent.KEYCODE_ESCAPE -> {
                            goBack()
                            true
                        }
                        else -> false
                    }
                } else false
            }
    ) {
        // ExoPlayer video surface
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = playerManager.player
                    useController = false
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { view ->
                view.player = playerManager.player
            },
            modifier = Modifier.fillMaxSize()
        )

        // Netflix-style overlay
        val currentChannel = channels.getOrNull(currentIndex)

        AnimatedVisibility(
            visible = showOverlay || isBuffering || errorMessage != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            PlayerControls(
                channelName = currentChannel?.name ?: "...",
                channelNumber = currentIndex + 1,
                totalChannels = channels.size,
                channelGroup = currentChannel?.groupTitle,
                channelLogo = currentChannel?.logoUrl,
                isPlaying = isPlaying,
                isBuffering = isBuffering,
                currentPosition = currentPosition,
                duration = duration,
                errorMessage = errorMessage,
                audioTracks = audioTracks,
                subtitleTracks = subtitleTracks,
                onPlayPause = { togglePlay() },
                onSeekBack = { seekBack() },
                onSeekForward = { seekFwd() },
                onPrevChannel = { goToPrev() },
                onNextChannel = { goToNext() },
                onBack = { goBack() },
                onSelectAudio = { track ->
                    playerManager.selectAudioTrack(track)
                    // Refresh tracks after selection
                    audioTracks = playerManager.getAudioTracks()
                },
                onSelectSubtitle = { track ->
                    playerManager.selectSubtitleTrack(track)
                    subtitleTracks = playerManager.getSubtitleTracks()
                }
            )
        }
    }
}
