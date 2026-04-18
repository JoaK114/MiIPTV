package com.app.mitvplayer.ui.screens

import android.content.res.Configuration
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.app.mitvplayer.ui.components.TVMenuCard
import com.app.mitvplayer.ui.theme.Amber
import com.app.mitvplayer.ui.theme.DarkBackground
import com.app.mitvplayer.ui.theme.DarkSurface
import com.app.mitvplayer.ui.theme.SecondaryTeal
import com.app.mitvplayer.ui.theme.SuccessGreen
import com.app.mitvplayer.ui.theme.TextDimmed
import com.app.mitvplayer.ui.theme.TextWhite
import com.app.mitvplayer.util.NetworkUtils

@Composable
fun HomeScreen(navController: NavController) {
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    val isCompact = configuration.screenWidthDp < 600
    val horizontalPad = if (isCompact) 20.dp else 64.dp
    val verticalPad = if (isCompact) 24.dp else 40.dp
    val cardW = if (isCompact) 150.dp else 240.dp
    val cardH = if (isCompact) 130.dp else 200.dp
    val spacing = if (isCompact) 12.dp else 24.dp
    val iconSize = if (isCompact) 40.sp else 56.sp

    val isVpn = remember { NetworkUtils.isVpnActive(context) }
    val networkType = remember { NetworkUtils.getNetworkType(context) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DarkBackground,
                        Color(0xFF0F0F23),
                        DarkBackground
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = horizontalPad, vertical = verticalPad),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // VPN / Network status bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isVpn) {
                    Icon(
                        imageVector = Icons.Default.VpnLock,
                        contentDescription = "VPN Activa",
                        tint = SuccessGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = networkType,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isVpn) SuccessGreen else TextDimmed
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Logo and title
            Text(
                text = "\uD83D\uDCFA",
                fontSize = iconSize
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Mi TV Player",
                style = if (isCompact) MaterialTheme.typography.headlineMedium
                else MaterialTheme.typography.headlineLarge,
                color = TextWhite,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Tu centro de entretenimiento",
                style = MaterialTheme.typography.bodyLarge,
                color = TextDimmed
            )

            Spacer(modifier = Modifier.height(spacing * 1.5f))

            // Row 1: Playlists + Import
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TVMenuCard(
                    icon = Icons.Default.PlaylistPlay,
                    title = "Mis Listas",
                    subtitle = "Ver canales guardados",
                    onClick = { navController.navigate("playlists") },
                    width = cardW,
                    height = cardH
                )
                Spacer(modifier = Modifier.width(spacing))
                TVMenuCard(
                    icon = Icons.Default.Add,
                    title = "Importar",
                    subtitle = "Agregar nueva lista",
                    onClick = { navController.navigate("import") },
                    width = cardW,
                    height = cardH
                )
            }

            Spacer(modifier = Modifier.height(spacing))

            // Row 2: Favorites + Direct URL
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TVMenuCard(
                    icon = Icons.Default.Star,
                    title = "Favoritos",
                    subtitle = "Canales favoritos",
                    onClick = { navController.navigate("favorites") },
                    width = cardW,
                    height = cardH
                )
                Spacer(modifier = Modifier.width(spacing))
                TVMenuCard(
                    icon = Icons.Default.OndemandVideo,
                    title = "Ver Video",
                    subtitle = "Reproducir URL",
                    onClick = { navController.navigate("direct_url") },
                    width = cardW,
                    height = cardH
                )
            }

            Spacer(modifier = Modifier.height(spacing))

            // Row 3: Settings (centered)
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TVMenuCard(
                    icon = Icons.Default.Settings,
                    title = "Ajustes",
                    subtitle = "Configuración",
                    onClick = { navController.navigate("settings") },
                    width = cardW,
                    height = cardH
                )
            }

            Spacer(modifier = Modifier.height(spacing * 2))

            // Navigation hints
            if (!isCompact) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = DarkSurface.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "▲▼◄► Mover      ●  Seleccionar      ◄  Volver",
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextDimmed
                    )
                }
            }
        }
    }
}
