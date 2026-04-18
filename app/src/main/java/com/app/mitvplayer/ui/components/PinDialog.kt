package com.app.mitvplayer.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.mitvplayer.ui.theme.CardBackground
import com.app.mitvplayer.ui.theme.DarkSurface
import com.app.mitvplayer.ui.theme.ErrorRed
import com.app.mitvplayer.ui.theme.PrimaryIndigo
import com.app.mitvplayer.ui.theme.PrimaryIndigoLight
import com.app.mitvplayer.ui.theme.SuccessGreen
import com.app.mitvplayer.ui.theme.TextDimmed
import com.app.mitvplayer.ui.theme.TextGray
import com.app.mitvplayer.ui.theme.TextWhite
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * PIN Dialog for parental control.
 * D-Pad friendly on-screen numpad that works with TV remote.
 *
 * @param title Dialog title (e.g. "Ingrese PIN" or "Crear PIN")
 * @param onPinEntered Called when 4 digits are entered. Return true if PIN is correct.
 * @param onDismiss Called when dialog is dismissed.
 */
@Composable
fun PinDialog(
    title: String = "Ingrese PIN",
    subtitle: String? = null,
    onPinEntered: (String) -> Boolean,
    onDismiss: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var success by remember { mutableStateOf(false) }
    val shakeOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    // Auto-submit when 4 digits entered
    LaunchedEffect(pin) {
        if (pin.length == 4) {
            delay(200) // Brief pause for visual feedback
            val correct = onPinEntered(pin)
            if (correct) {
                success = true
                delay(300)
                onDismiss()
            } else {
                error = true
                // Shake animation
                scope.launch {
                    shakeOffset.animateTo(
                        targetValue = 0f,
                        animationSpec = keyframes {
                            durationMillis = 400
                            -10f at 50
                            10f at 100
                            -10f at 150
                            10f at 200
                            -5f at 250
                            5f at 300
                            0f at 400
                        }
                    )
                }
                delay(500)
                pin = ""
                error = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Lock icon
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = if (success) SuccessGreen else PrimaryIndigoLight,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Title
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextWhite,
                    fontWeight = FontWeight.Bold
                )

                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextGray
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // PIN dots
                Row(
                    modifier = Modifier.offset { IntOffset(shakeOffset.value.toInt(), 0) },
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    repeat(4) { index ->
                        val filled = index < pin.length
                        val dotColor = when {
                            success -> SuccessGreen
                            error -> ErrorRed
                            filled -> PrimaryIndigo
                            else -> DarkSurface
                        }
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                    }
                }

                if (error) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "PIN incorrecto",
                        style = MaterialTheme.typography.bodySmall,
                        color = ErrorRed
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Numpad (3x4 grid)
                val keys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("", "0", "⌫")
                )

                keys.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        row.forEach { key ->
                            if (key.isEmpty()) {
                                Spacer(modifier = Modifier.size(64.dp))
                            } else {
                                NumPadButton(
                                    key = key,
                                    onClick = {
                                        when (key) {
                                            "⌫" -> {
                                                if (pin.isNotEmpty()) {
                                                    pin = pin.dropLast(1)
                                                } else {
                                                    onDismiss()
                                                }
                                            }
                                            else -> {
                                                if (pin.length < 4) {
                                                    pin += key
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Presione ⌫ sin dígitos para cancelar",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextDimmed
                )
            }
        }
    }
}

@Composable
private fun NumPadButton(
    key: String,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val bgColor by animateColorAsState(
        targetValue = when {
            isFocused -> PrimaryIndigo
            key == "⌫" -> DarkSurface
            else -> DarkSurface.copy(alpha = 0.7f)
        },
        label = "numBg"
    )

    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .dpadClickable(
                onClick = onClick,
                onFocusChange = { isFocused = it }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (key == "⌫") {
            Icon(
                imageVector = Icons.Default.Backspace,
                contentDescription = "Borrar",
                tint = if (isFocused) TextWhite else TextGray,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Text(
                text = key,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = if (isFocused) TextWhite else TextGray
            )
        }
    }
}
