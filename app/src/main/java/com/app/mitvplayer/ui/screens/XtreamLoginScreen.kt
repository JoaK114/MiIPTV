package com.app.mitvplayer.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.app.mitvplayer.data.models.XtreamAccount
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
fun XtreamLoginScreen(
    navController: NavController,
    viewModel: PlaylistViewModel
) {
    val xtreamState by viewModel.xtreamState.collectAsState()
    val savedAccounts by viewModel.xtreamAccounts.collectAsState()

    // 0 = saved accounts list, 1 = login form
    var currentView by remember { mutableIntStateOf(if (savedAccounts.isEmpty()) 1 else 0) }
    var serverUrl by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

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
                    if (currentView == 1 && savedAccounts.isNotEmpty()) currentView = 0
                    else navController.popBackStack()
                }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = if (currentView == 1) "Conectar Xtream Codes" else "Cuentas Xtream",
                style = MaterialTheme.typography.headlineMedium,
                color = TextWhite,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Status messages
        when (val state = xtreamState) {
            is PlaylistViewModel.XtreamState.Loading -> {
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
                            text = state.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = PrimaryIndigoLight
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            is PlaylistViewModel.XtreamState.Success -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.15f)),
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
                            text = state.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = SuccessGreen
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            is PlaylistViewModel.XtreamState.Error -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.15f)),
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
            else -> {}
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (currentView) {
            0 -> {
                // Saved accounts
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        TVButton(
                            text = "Nueva conexión",
                            icon = Icons.Default.Login,
                            onClick = { currentView = 1 }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    items(savedAccounts) { account ->
                        XtreamAccountCard(
                            account = account,
                            onConnect = {
                                viewModel.connectXtreamAccount(account)
                            },
                            onDelete = {
                                viewModel.deleteXtreamAccount(account.id)
                            }
                        )
                    }
                }
            }
            1 -> {
                // Login form
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = null,
                        tint = PrimaryIndigoLight,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Ingrese los datos de su proveedor",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextGray
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = { serverUrl = it },
                        label = { Text("URL del servidor") },
                        placeholder = { Text("http://ejemplo.com:8080") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = xtreamTextFieldColors(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Usuario") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = xtreamTextFieldColors(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Contraseña") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = xtreamTextFieldColors(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    TVButton(
                        text = if (xtreamState is PlaylistViewModel.XtreamState.Loading) "Conectando…" else "Conectar",
                        icon = Icons.Default.Login,
                        onClick = {
                            if (serverUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank()) {
                                viewModel.loginXtream(serverUrl.trim(), username.trim(), password.trim())
                            }
                        },
                        modifier = Modifier.fillMaxWidth(0.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun XtreamAccountCard(
    account: XtreamAccount,
    onConnect: () -> Unit,
    onDelete: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .dpadClickable(
                onClick = onConnect,
                onFocusChange = { isFocused = it }
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isFocused) CardFocused else CardBackground
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = if (isFocused) PrimaryIndigoLight else TextGray,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isFocused) PrimaryIndigoLight else TextWhite,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${account.serverUrl}  •  ${account.status}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextDimmed,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (account.expirationDate > 0) {
                    val expStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        .format(Date(account.expirationDate * 1000))
                    Text(
                        text = "Expira: $expStr",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextDimmed
                    )
                }
            }

            if (isFocused) {
                Spacer(modifier = Modifier.width(8.dp))
                TVButton(
                    text = "",
                    icon = Icons.Default.Delete,
                    onClick = onDelete
                )
            }
        }
    }
}

@Composable
private fun xtreamTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PrimaryIndigo,
    unfocusedBorderColor = TextDimmed,
    focusedLabelColor = PrimaryIndigoLight,
    unfocusedLabelColor = TextGray,
    cursorColor = PrimaryIndigoLight,
    focusedTextColor = TextWhite,
    unfocusedTextColor = TextWhite,
    focusedPlaceholderColor = TextDimmed,
    unfocusedPlaceholderColor = TextDimmed
)
