package com.app.mitvplayer.ui.screens

import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import com.app.mitvplayer.player.TVPlayerManager
import com.app.mitvplayer.ui.components.TVButton
import com.app.mitvplayer.ui.theme.DarkBackground
import com.app.mitvplayer.ui.theme.PrimaryIndigo
import com.app.mitvplayer.ui.theme.PrimaryIndigoLight
import com.app.mitvplayer.ui.theme.TextDimmed
import com.app.mitvplayer.ui.theme.TextGray
import com.app.mitvplayer.ui.theme.TextWhite

@Composable
fun DirectUrlScreen(
    navController: NavController
) {
    val context = LocalContext.current
    var url by remember { mutableStateOf("") }
    var isPlaying by remember { mutableStateOf(false) }
    val playerManager = remember { TVPlayerManager(context) }

    DisposableEffect(Unit) {
        onDispose {
            playerManager.release()
        }
    }

    if (!isPlaying) {
        // URL input
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp, vertical = 24.dp)
        ) {
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
                    text = "Reproducir URL",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextWhite,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Ingresa la URL del video o stream",
                style = MaterialTheme.typography.titleMedium,
                color = TextGray
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("URL del video") },
                placeholder = { Text("https://ejemplo.com/video.mp4") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryIndigo,
                    unfocusedBorderColor = TextDimmed,
                    focusedLabelColor = PrimaryIndigoLight,
                    unfocusedLabelColor = TextGray,
                    cursorColor = PrimaryIndigoLight,
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            TVButton(
                text = "Reproducir",
                onClick = {
                    if (url.isNotBlank()) {
                        playerManager.playUrl(url.trim())
                        isPlaying = true
                    }
                },
                modifier = Modifier.fillMaxWidth(0.5f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Soporta: MP4, MKV, HLS (.m3u8), DASH",
                style = MaterialTheme.typography.bodyMedium,
                color = TextDimmed
            )
        }
    } else {
        // Full screen player
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { /* absorb taps */ }
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = playerManager.player
                        useController = true
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
        }
    }
}
