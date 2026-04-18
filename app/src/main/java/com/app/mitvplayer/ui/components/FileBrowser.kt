package com.app.mitvplayer.ui.components

import android.os.Environment
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.SdStorage
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.app.mitvplayer.ui.theme.CardBackground
import com.app.mitvplayer.ui.theme.CardFocused
import com.app.mitvplayer.ui.theme.DarkBackground
import com.app.mitvplayer.ui.theme.PrimaryIndigo
import com.app.mitvplayer.ui.theme.PrimaryIndigoLight
import com.app.mitvplayer.ui.theme.SuccessGreen
import com.app.mitvplayer.ui.theme.TextGray
import com.app.mitvplayer.ui.theme.TextWhite
import java.io.File

data class FileEntry(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val isM3U: Boolean = false
)

@Composable
fun FileBrowser(
    onFileSelected: (String) -> Unit
) {
    var currentPath by remember { mutableStateOf<String?>(null) }
    var entries by remember { mutableStateOf<List<FileEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Scan storage volumes
    LaunchedEffect(currentPath) {
        isLoading = true
        entries = if (currentPath == null) {
            getStorageVolumes()
        } else {
            getDirectoryEntries(currentPath!!)
        }
        isLoading = false
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Path info
        if (currentPath != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                TVButton(
                    text = "Atrás",
                    icon = Icons.Default.FolderOpen,
                    onClick = {
                        val parent = File(currentPath!!).parent
                        if (parent != null && parent != "/") {
                            currentPath = parent
                        } else {
                            currentPath = null
                        }
                    }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = currentPath?.substringAfterLast("/") ?: "",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (entries.isEmpty() && !isLoading) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.SdStorage,
                    contentDescription = null,
                    tint = TextGray,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No se encontraron archivos",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextGray
                )
                Text(
                    text = "Conecta un USB con archivos .m3u",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextGray
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(entries) { entry ->
                    FileItemCard(
                        entry = entry,
                        onClick = {
                            if (entry.isDirectory) {
                                currentPath = entry.path
                            } else if (entry.isM3U) {
                                try {
                                    val content = File(entry.path).readText()
                                    onFileSelected(content)
                                } catch (e: Exception) {
                                    // Could not read file
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FileItemCard(
    entry: FileEntry,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val bgColor by animateColorAsState(
        targetValue = if (isFocused) CardFocused else CardBackground,
        label = "bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) PrimaryIndigo else Color.Transparent,
        label = "border"
    )
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.02f else 1f,
        label = "scale"
    )

    val icon: ImageVector = when {
        entry.isDirectory -> Icons.Default.Folder
        entry.isM3U -> Icons.Default.Description
        else -> Icons.Default.Description
    }
    val tint = when {
        entry.isDirectory -> PrimaryIndigoLight
        entry.isM3U -> SuccessGreen
        else -> TextGray
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .dpadClickable(
                onClick = onClick,
                onFocusChange = { isFocused = it }
            ),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = entry.name,
                style = MaterialTheme.typography.bodyLarge,
                color = TextWhite,
                fontWeight = if (entry.isM3U) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (entry.isM3U) {
                Text(
                    text = "M3U",
                    style = MaterialTheme.typography.labelSmall,
                    color = SuccessGreen,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun getStorageVolumes(): List<FileEntry> {
    val volumes = mutableListOf<FileEntry>()

    // Internal storage
    val internal = Environment.getExternalStorageDirectory()
    if (internal.exists()) {
        volumes.add(
            FileEntry(
                name = "Almacenamiento interno",
                path = internal.absolutePath,
                isDirectory = true
            )
        )
    }

    // USB / SD card volumes
    val storageDir = File("/storage")
    if (storageDir.exists()) {
        storageDir.listFiles()?.forEach { file ->
            if (file.isDirectory &&
                file.name != "emulated" &&
                file.name != "self" &&
                file.canRead()
            ) {
                volumes.add(
                    FileEntry(
                        name = "USB: ${file.name}",
                        path = file.absolutePath,
                        isDirectory = true
                    )
                )
            }
        }
    }

    return volumes
}

private fun getDirectoryEntries(path: String): List<FileEntry> {
    val dir = File(path)
    if (!dir.exists() || !dir.canRead()) return emptyList()

    return dir.listFiles()
        ?.filter { !it.isHidden }
        ?.map { file ->
            val ext = file.extension.lowercase()
            FileEntry(
                name = file.name,
                path = file.absolutePath,
                isDirectory = file.isDirectory,
                isM3U = ext == "m3u" || ext == "m3u8"
            )
        }
        ?.sortedWith(
            compareByDescending<FileEntry> { it.isDirectory }
                .thenByDescending { it.isM3U }
                .thenBy { it.name.lowercase() }
        )
        ?: emptyList()
}
