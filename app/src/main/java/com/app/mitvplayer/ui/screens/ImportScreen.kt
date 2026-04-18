package com.app.mitvplayer.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.app.mitvplayer.ui.components.FileBrowser
import com.app.mitvplayer.ui.components.TVButton
import com.app.mitvplayer.ui.components.TVOptionCard
import com.app.mitvplayer.ui.theme.CardBackground
import com.app.mitvplayer.ui.theme.DarkBackground
import com.app.mitvplayer.ui.theme.ErrorRed
import com.app.mitvplayer.ui.theme.PrimaryIndigo
import com.app.mitvplayer.ui.theme.PrimaryIndigoLight
import com.app.mitvplayer.ui.theme.SuccessGreen
import com.app.mitvplayer.ui.theme.TextDimmed
import com.app.mitvplayer.ui.theme.TextGray
import com.app.mitvplayer.ui.theme.TextWhite
import com.app.mitvplayer.viewmodel.PlaylistViewModel

@Composable
fun ImportScreen(
    navController: NavController,
    viewModel: PlaylistViewModel
) {
    val importState by viewModel.importState.collectAsState()
    val context = LocalContext.current

    // 0 = options, 1 = URL input, 2 = file browser
    var currentView by remember { mutableIntStateOf(0) }
    var url by remember { mutableStateOf("") }
    var hasStoragePermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                android.os.Environment.isExternalStorageManager()
            } else {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

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
                onClick = {
                    if (currentView == 0) navController.popBackStack()
                    else currentView = 0
                }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = when (currentView) {
                    1 -> "Importar desde URL"
                    2 -> "Seleccionar archivo"
                    else -> "Importar Lista"
                },
                style = MaterialTheme.typography.headlineMedium,
                color = TextWhite,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Status messages
        AnimatedVisibility(
            visible = importState is PlaylistViewModel.ImportState.Loading,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PrimaryIndigo.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = PrimaryIndigoLight,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Importando lista…",
                        style = MaterialTheme.typography.bodyLarge,
                        color = PrimaryIndigoLight
                    )
                }
            }
        }

        val state = importState
        if (state is PlaylistViewModel.ImportState.Success) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = SuccessGreen.copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "¡Lista importada! ${state.channelCount} canales agregados",
                        style = MaterialTheme.typography.bodyLarge,
                        color = SuccessGreen
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (state is PlaylistViewModel.ImportState.Error) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = ErrorRed.copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "❌ ${state.message}",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = ErrorRed
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Content
        when (currentView) {
            0 -> ImportOptions(
                onUrlClick = { currentView = 1 },
                onFileClick = {
                    if (hasStoragePermission) {
                        currentView = 2
                    } else {
                        // Try to prompt for permission
                        currentView = 2
                    }
                },
                onXtreamClick = { navController.navigate("xtream_login") }
            )
            1 -> UrlInputView(
                url = url,
                onUrlChange = { url = it },
                onImport = {
                    if (url.isNotBlank()) {
                        viewModel.importFromUrl(url.trim())
                    }
                },
                isLoading = importState is PlaylistViewModel.ImportState.Loading
            )
            2 -> FileBrowser(
                onFileSelected = { content ->
                    viewModel.importFromContent(content, "Lista importada")
                }
            )
        }
    }
}

@Composable
private fun ImportOptions(
    onUrlClick: () -> Unit,
    onFileClick: () -> Unit,
    onXtreamClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "¿Cómo deseas importar la lista?",
            style = MaterialTheme.typography.titleLarge,
            color = TextGray,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            TVOptionCard(
                icon = Icons.Default.Language,
                title = "Desde URL",
                onClick = onUrlClick,
                modifier = Modifier.size(200.dp)
            )
            Spacer(modifier = Modifier.width(24.dp))
            TVOptionCard(
                icon = Icons.Default.Usb,
                title = "Desde USB",
                onClick = onFileClick,
                modifier = Modifier.size(200.dp)
            )
            Spacer(modifier = Modifier.width(24.dp))
            TVOptionCard(
                icon = Icons.Default.Cloud,
                title = "Xtream Codes",
                onClick = onXtreamClick,
                modifier = Modifier.size(200.dp)
            )
        }
    }
}

@Composable
private fun UrlInputView(
    url: String,
    onUrlChange: (String) -> Unit,
    onImport: () -> Unit,
    isLoading: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Ingresa la dirección de la lista M3U",
            style = MaterialTheme.typography.titleMedium,
            color = TextGray,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = url,
            onValueChange = onUrlChange,
            label = { Text("URL de la lista") },
            placeholder = { Text("https://ejemplo.com/lista.m3u") },
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
            text = if (isLoading) "Importando…" else "Importar Lista",
            onClick = onImport,
            modifier = Modifier.fillMaxWidth(0.5f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Formatos soportados: M3U, M3U8",
            style = MaterialTheme.typography.bodyMedium,
            color = TextDimmed
        )
    }
}
