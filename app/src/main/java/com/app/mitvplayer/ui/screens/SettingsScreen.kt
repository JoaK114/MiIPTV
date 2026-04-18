package com.app.mitvplayer.ui.screens

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.app.mitvplayer.ui.components.PinDialog
import com.app.mitvplayer.ui.components.TVButton
import com.app.mitvplayer.ui.components.dpadClickable
import com.app.mitvplayer.ui.theme.Amber
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
import com.app.mitvplayer.updater.UpdateChecker
import com.app.mitvplayer.viewmodel.PlaylistViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: PlaylistViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showClearConfirm by remember { mutableStateOf(false) }
    var clearDone by remember { mutableStateOf(false) }
    var showPinSetup by remember { mutableStateOf(false) }
    var showPinVerify by remember { mutableStateOf(false) }
    var pinAction by remember { mutableStateOf("") } // "setup", "remove"
    var updateStatus by remember { mutableStateOf("") }

    // Buffer presets
    val bufferPresets = listOf(0, 500, 1000, 2000, 5000)
    val bufferLabels = listOf("Sin buffer", "0.5s", "1s", "2s (recomendado)", "5s")
    var currentBuffer by remember { mutableIntStateOf(viewModel.getBufferPresetMs()) }

    if (showClearConfirm) {
        ClearDataConfirmDialog(
            onConfirm = {
                viewModel.deleteAllPlaylists()
                showClearConfirm = false
                clearDone = true
            },
            onCancel = { showClearConfirm = false }
        )
        return
    }

    if (showPinSetup) {
        PinDialog(
            title = "Crear PIN parental",
            subtitle = "Ingrese un PIN de 4 dígitos",
            onPinEntered = { pin ->
                viewModel.setParentalPin(pin)
                showPinSetup = false
                true
            },
            onDismiss = { showPinSetup = false }
        )
        return
    }

    if (showPinVerify) {
        PinDialog(
            title = "Verificar PIN",
            subtitle = "Ingrese su PIN actual",
            onPinEntered = { pin ->
                val correct = viewModel.verifyParentalPin(pin)
                if (correct && pinAction == "remove") {
                    viewModel.removeParentalPin()
                }
                showPinVerify = false
                correct
            },
            onDismiss = { showPinVerify = false }
        )
        return
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
                onClick = { navController.popBackStack() }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Ajustes",
                style = MaterialTheme.typography.headlineMedium,
                color = TextWhite,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ── Buffer de video ──
            item {
                SectionTitle("Reproductor")
            }

            item {
                val bufferIndex = bufferPresets.indexOf(currentBuffer).coerceAtLeast(0)
                SettingsItem(
                    icon = Icons.Default.Speed,
                    title = "Buffer de video",
                    subtitle = bufferLabels[bufferIndex],
                    onClick = {
                        val nextIndex = (bufferIndex + 1) % bufferPresets.size
                        currentBuffer = bufferPresets[nextIndex]
                        viewModel.setBufferPresetMs(currentBuffer)
                    }
                )
            }

            // ── Parental Controls ──
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionTitle("Control Parental")
            }

            item {
                val hasPin = viewModel.hasParentalPin()
                SettingsItem(
                    icon = if (hasPin) Icons.Default.Lock else Icons.Default.LockOpen,
                    title = if (hasPin) "PIN configurado" else "Configurar PIN",
                    subtitle = if (hasPin) "Toque para eliminar el PIN"
                    else "Proteger categorías con PIN de 4 dígitos",
                    onClick = {
                        if (hasPin) {
                            pinAction = "remove"
                            showPinVerify = true
                        } else {
                            showPinSetup = true
                        }
                    }
                )
            }

            // ── Updates ──
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionTitle("Actualizaciones")
            }

            item {
                SettingsItem(
                    icon = Icons.Default.SystemUpdate,
                    title = "Buscar actualizaciones",
                    subtitle = if (updateStatus.isNotBlank()) updateStatus else "Verificar si hay una nueva versión",
                    onClick = {
                        updateStatus = "Buscando…"
                        scope.launch {
                            val checker = UpdateChecker(context)
                            val update = checker.checkForUpdate("1.0.0")
                            updateStatus = if (update != null) {
                                "¡Versión ${update.version} disponible!"
                            } else {
                                "Ya tiene la última versión"
                            }
                        }
                    }
                )
            }

            // ── Data ──
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionTitle("Datos")
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Delete,
                    title = "Borrar todos los datos",
                    subtitle = "Eliminar todas las listas y canales guardados",
                    onClick = { showClearConfirm = true },
                    isDestructive = true
                )
            }

            if (clearDone) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = SuccessGreen.copy(alpha = 0.15f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "✅ Todos los datos han sido eliminados",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = SuccessGreen
                        )
                    }
                }
            }

            // ── Info ──
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionTitle("Información")
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "Acerca de",
                    subtitle = "Mi TV Player v1.0.0",
                    onClick = { }
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.LiveTv,
                    title = "Formatos soportados",
                    subtitle = "M3U, M3U8, MP4, MKV, HLS, DASH, Xtream Codes",
                    onClick = { }
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Gamepad,
                    title = "Controles del reproductor",
                    subtitle = "▲▼ Canal  ◄► Avanzar  OK Play/Pausa  ← Volver",
                    onClick = { }
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = PrimaryIndigoLight,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }
    val bgColor by animateColorAsState(
        targetValue = if (isFocused) CardFocused else CardBackground,
        label = "bg"
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            isFocused && isDestructive -> ErrorRed
            isFocused -> PrimaryIndigo
            else -> Color.Transparent
        },
        label = "border"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .dpadClickable(
                onClick = onClick,
                onFocusChange = { isFocused = it }
            ),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = when {
                    isDestructive && isFocused -> ErrorRed
                    isFocused -> PrimaryIndigoLight
                    else -> TextGray
                },
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = when {
                        isDestructive && isFocused -> ErrorRed
                        isFocused -> PrimaryIndigoLight
                        else -> TextWhite
                    },
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextGray
                )
            }
        }
    }
}

@Composable
private fun ClearDataConfirmDialog(
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
                    text = "¿Borrar TODOS los datos?",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextWhite,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Se eliminarán todas las listas y canales.\nEsta acción no se puede deshacer.",
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
                        text = "Borrar",
                        icon = Icons.Default.Delete,
                        onClick = onConfirm
                    )
                }
            }
        }
    }
}
