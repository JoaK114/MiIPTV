package com.app.mitvplayer.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.app.mitvplayer.data.M3UParser
import com.app.mitvplayer.data.models.Channel
import com.app.mitvplayer.ui.components.TVButton
import com.app.mitvplayer.ui.components.dpadClickable
import com.app.mitvplayer.ui.theme.*
import com.app.mitvplayer.viewmodel.PlaylistViewModel

@Composable
fun SeriesDetailScreen(
    navController: NavController,
    viewModel: PlaylistViewModel,
    playlistId: Long,
    seriesName: String
) {
    val seasons by viewModel.seriesSeasons.collectAsState()
    val episodes by viewModel.seriesEpisodes.collectAsState()

    var selectedSeason by remember { mutableStateOf<Int?>(null) }

    // Load seasons
    LaunchedEffect(playlistId, seriesName) {
        viewModel.loadSeriesSeasons(playlistId, seriesName)
    }

    // Load episodes when season is selected or when seasons arrive
    LaunchedEffect(seasons) {
        if (seasons.isNotEmpty() && selectedSeason == null) {
            selectedSeason = seasons.first()
        }
    }

    LaunchedEffect(selectedSeason) {
        val season = selectedSeason ?: return@LaunchedEffect
        if (season == 0 && seasons.size == 1) {
            // All in one "season" — load all
            viewModel.loadAllSeriesEpisodes(playlistId, seriesName)
        } else {
            viewModel.loadSeriesEpisodes(playlistId, seriesName, season)
        }
    }

    // Get cover from first episode
    val coverUrl = remember(episodes) {
        episodes.firstOrNull()?.logoUrl
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DarkBackground, Color(0xFF0D0D1F), DarkBackground)
                )
            )
    ) {
        // ── Header with series info ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            // Background poster (blurred effect)
            if (!coverUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(coverUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    alpha = 0.3f
                )
            }

            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                DarkBackground.copy(alpha = 0.7f),
                                DarkBackground
                            )
                        )
                    )
            )

            // Content
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // Small poster
                if (!coverUrl.isNullOrBlank()) {
                    Card(
                        modifier = Modifier
                            .width(100.dp)
                            .height(150.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(coverUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = seriesName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.width(20.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    // Back button
                    TVButton(
                        text = "Volver",
                        icon = Icons.Default.ArrowBack,
                        onClick = { navController.popBackStack() }
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = seriesName,
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    val totalEps = episodes.size
                    val seasonCount = seasons.size
                    Text(
                        text = buildString {
                            if (seasonCount > 1 || (seasonCount == 1 && seasons.first() != 0)) {
                                append("$seasonCount temporada${if (seasonCount > 1) "s" else ""} · ")
                            }
                            append("$totalEps episodio${if (totalEps != 1) "s" else ""}")
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextDimmed
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Play all button
                    if (episodes.isNotEmpty()) {
                        TVButton(
                            text = "Reproducir",
                            icon = Icons.Default.PlayArrow,
                            onClick = {
                                val firstEp = episodes.first()
                                navController.navigate("player/$playlistId/${firstEp.id}")
                            }
                        )
                    }
                }
            }
        }

        // ── Season selector ──
        if (seasons.size > 1 || (seasons.size == 1 && seasons.first() != 0)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                seasons.forEach { season ->
                    SeasonChip(
                        label = if (season == 0) "Todos" else "Temporada $season",
                        isSelected = selectedSeason == season,
                        onClick = { selectedSeason = season }
                    )
                }
            }
        }

        // ── Episode list ──
        if (episodes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryIndigo)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(
                    items = episodes,
                    key = { it.id }
                ) { episode ->
                    EpisodeCard(
                        episode = episode,
                        onClick = {
                            navController.navigate("player/$playlistId/${episode.id}")
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SeasonChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val bgColor by animateColorAsState(
        targetValue = when {
            isSelected -> PrimaryIndigo
            isFocused -> PrimaryIndigo.copy(alpha = 0.3f)
            else -> DarkSurface
        },
        label = "seasonBg"
    )

    Card(
        modifier = Modifier.dpadClickable(
            onClick = onClick,
            onFocusChange = { isFocused = it }
        ),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(20.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            style = MaterialTheme.typography.labelLarge,
            color = if (isSelected || isFocused) TextWhite else TextGray,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun EpisodeCard(
    episode: Channel,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.02f else 1f, label = "scale"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) PrimaryIndigo else Color.Transparent, label = "border"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isFocused) CardFocused else CardBackground, label = "bg"
    )

    val episodeInfo = remember(episode.name) {
        M3UParser.parseEpisodeInfo(episode.name)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
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
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Episode number badge
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = PrimaryIndigo.copy(alpha = if (isFocused) 0.4f else 0.2f),
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = episodeInfo?.label ?: "E${episode.episodeNum}",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isFocused) PrimaryIndigoLight else TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Thumbnail
            if (!episode.logoUrl.isNullOrBlank()) {
                Card(
                    modifier = Modifier
                        .width(90.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(episode.logoUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
            }

            // Name
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = episode.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isFocused) PrimaryIndigoLight else TextWhite,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (episode.groupTitle != null) {
                    Text(
                        text = episode.groupTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextDimmed,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Play icon
            if (isFocused) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Reproducir",
                    tint = PrimaryIndigoLight,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
