package com.app.mitvplayer.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.app.mitvplayer.data.M3UParser
import com.app.mitvplayer.data.dao.GroupCount
import com.app.mitvplayer.data.dao.SeriesInfo
import com.app.mitvplayer.data.models.Channel
import com.app.mitvplayer.ui.components.TVButton
import com.app.mitvplayer.ui.components.dpadClickable
import com.app.mitvplayer.ui.theme.*
import com.app.mitvplayer.viewmodel.PlaylistViewModel
import kotlinx.coroutines.delay
import java.net.URLEncoder

// ═══════════════════════════════════════════════════════
// Content tabs
// ═══════════════════════════════════════════════════════
private enum class ContentTab(
    val label: String,
    val contentType: String,
    val icon: ImageVector
) {
    TV("TV en Vivo", "tv", Icons.Default.LiveTv),
    MOVIES("Películas", "movie", Icons.Default.Movie),
    SERIES("Series", "series", Icons.Default.Tv)
}

// ═══════════════════════════════════════════════════════
// Adult content filter
// ═══════════════════════════════════════════════════════
private val BLOCKED_KEYWORDS = listOf(
    "xxx", "adult", "porn", "erotic", "18+", "+18",
    "sex", "nude", "playboy", "penthouse", "hustler",
    "brazzers", "naughty", "xvideos", "xnxx", "onlyfans",
    "hentai", "adultos", "adulto", "porno", "erótic",
    "caliente", "stripper", "milf", "fetish", "lust"
)

private fun isAdultContent(text: String): Boolean {
    val lower = text.lowercase()
    return BLOCKED_KEYWORDS.any { lower.contains(it) }
}

private fun filterSafeChannels(channels: List<Channel>): List<Channel> {
    return channels.filter { channel ->
        !isAdultContent(channel.name) &&
        !isAdultContent(channel.groupTitle ?: "") &&
        !isAdultContent(channel.url)
    }
}

// ═══════════════════════════════════════════════════════
// Main screen
// ═══════════════════════════════════════════════════════
@Composable
fun DefaultPlaylistScreen(
    navController: NavController,
    viewModel: PlaylistViewModel,
    playlistId: Long
) {
    // Load content type counts
    LaunchedEffect(playlistId) {
        viewModel.loadContentTypeCounts(playlistId)
        viewModel.loadGroupsForPlaylist(playlistId)
    }

    val contentTypeCounts by viewModel.contentTypeCounts.collectAsState()
    val refreshState by viewModel.refreshState.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val defaultPlaylist by viewModel.defaultPlaylist.collectAsState()

    var selectedTab by remember { mutableStateOf(ContentTab.TV) }
    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }

    // Debounced search
    LaunchedEffect(searchQuery) {
        if (searchQuery.isBlank()) {
            viewModel.clearSearchResults()
            return@LaunchedEffect
        }
        delay(300)
        viewModel.searchChannels(playlistId, searchQuery)
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
        // ── Top Bar ──
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 4.dp)
        ) {
            TVButton(
                text = "Mis Listas",
                icon = Icons.Default.ArrowBack,
                onClick = { navController.navigate("home") { popUpTo("default_playlist") { inclusive = true } } }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = defaultPlaylist?.name ?: "Mi Playlist",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val total = contentTypeCounts.values.sum()
                Text(
                    text = "$total canales",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextDimmed
                )
            }

            // Reload button
            ReloadButton(
                isRefreshing = refreshState is PlaylistViewModel.RefreshState.Loading,
                hasUrl = !defaultPlaylist?.url.isNullOrBlank(),
                onClick = { viewModel.refreshPlaylist(playlistId) }
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Search button
            SearchButton(
                isActive = showSearch,
                onClick = {
                    showSearch = !showSearch
                    if (!showSearch) {
                        searchQuery = ""
                        viewModel.clearSearchResults()
                    }
                }
            )
        }

        // ── Refresh status ──
        when (val state = refreshState) {
            is PlaylistViewModel.RefreshState.Loading -> {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    color = PrimaryIndigo,
                    trackColor = DarkSurface
                )
            }
            is PlaylistViewModel.RefreshState.Success -> {
                Text(
                    text = "✓ Actualizada: ${state.newChannelCount} canales",
                    style = MaterialTheme.typography.bodySmall,
                    color = SuccessGreen,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 2.dp)
                )
            }
            is PlaylistViewModel.RefreshState.Error -> {
                Text(
                    text = "❌ ${state.message}",
                    style = MaterialTheme.typography.bodySmall,
                    color = ErrorRed,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 2.dp)
                )
            }
            else -> {}
        }

        // ── Search bar ──
        if (showSearch) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar canales…") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = TextGray)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Limpiar", tint = TextGray)
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryIndigo,
                    unfocusedBorderColor = TextDimmed,
                    focusedLeadingIconColor = PrimaryIndigoLight,
                    cursorColor = PrimaryIndigoLight,
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedPlaceholderColor = TextDimmed,
                    unfocusedPlaceholderColor = TextDimmed
                ),
                shape = RoundedCornerShape(14.dp)
            )
        }

        // ── Content Tabs ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ContentTab.entries.forEach { tab ->
                val count = contentTypeCounts[tab.contentType] ?: 0
                if (count > 0) {
                    ContentTabChip(
                        tab = tab,
                        count = count,
                        isSelected = selectedTab == tab,
                        onClick = { selectedTab = tab }
                    )
                }
            }
        }

        // ── Content ──
        if (searchQuery.isNotBlank()) {
            SearchResultsContent(
                searchResults = searchResults,
                searchQuery = searchQuery,
                playlistId = playlistId,
                navController = navController
            )
        } else {
            when (selectedTab) {
                ContentTab.TV -> TVContent(viewModel, playlistId, navController)
                ContentTab.MOVIES -> MoviesContent(viewModel, playlistId, navController)
                ContentTab.SERIES -> SeriesContent(viewModel, playlistId, navController)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// Content Tab chip
// ═══════════════════════════════════════════════════════
@Composable
private fun ContentTabChip(
    tab: ContentTab,
    count: Int,
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
        label = "tabBg"
    )

    Card(
        modifier = Modifier.dpadClickable(
            onClick = onClick,
            onFocusChange = { isFocused = it }
        ),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Icon(
                imageVector = tab.icon,
                contentDescription = null,
                tint = if (isSelected || isFocused) TextWhite else TextGray,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = tab.label,
                style = MaterialTheme.typography.labelLarge,
                color = if (isSelected || isFocused) TextWhite else TextGray,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(8.dp))
            // Badge with count
            Box(
                modifier = Modifier
                    .background(
                        color = if (isSelected) TextWhite.copy(alpha = 0.2f) else TextDimmed.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = formatCount(count),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected || isFocused) TextWhite else TextDimmed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }
        }
    }
}

private fun formatCount(count: Int): String =
    if (count >= 1000) "${count / 1000}.${(count % 1000) / 100}K" else count.toString()

// ═══════════════════════════════════════════════════════
// TV Content — channels in horizontal rows per group
// ═══════════════════════════════════════════════════════
@Composable
private fun TVContent(viewModel: PlaylistViewModel, playlistId: Long, navController: NavController) {
    var groups by remember { mutableStateOf<List<GroupCount>>(emptyList()) }

    LaunchedEffect(playlistId) {
        viewModel.loadGroupsForContentType(playlistId, "tv") { groups = it }
    }

    if (groups.isEmpty()) {
        EmptyContent("No hay canales de TV")
    } else {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            groups.filter { !isAdultContent(it.groupTitle) }.forEach { group ->
                item(key = "tv_h_${group.groupTitle}") {
                    GroupHeader(name = group.groupTitle, count = group.cnt, icon = Icons.Default.LiveTv)
                }
                item(key = "tv_r_${group.groupTitle}") {
                    GroupChannelRow(
                        viewModel = viewModel,
                        playlistId = playlistId,
                        groupName = group.groupTitle,
                        contentType = "tv",
                        isPortrait = false,
                        navController = navController
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// Movies Content — movie channels in portrait cards
// ═══════════════════════════════════════════════════════
@Composable
private fun MoviesContent(viewModel: PlaylistViewModel, playlistId: Long, navController: NavController) {
    var groups by remember { mutableStateOf<List<GroupCount>>(emptyList()) }

    LaunchedEffect(playlistId) {
        viewModel.loadGroupsForContentType(playlistId, "movie") { groups = it }
    }

    if (groups.isEmpty()) {
        EmptyContent("No hay películas")
    } else {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            groups.filter { !isAdultContent(it.groupTitle) }.forEach { group ->
                item(key = "mov_h_${group.groupTitle}") {
                    GroupHeader(name = group.groupTitle, count = group.cnt, icon = Icons.Default.Movie)
                }
                item(key = "mov_r_${group.groupTitle}") {
                    GroupChannelRow(
                        viewModel = viewModel,
                        playlistId = playlistId,
                        groupName = group.groupTitle,
                        contentType = "movie",
                        isPortrait = true,
                        navController = navController
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// Series Content — shows series FOLDERS, not episodes
// ═══════════════════════════════════════════════════════
@Composable
private fun SeriesContent(viewModel: PlaylistViewModel, playlistId: Long, navController: NavController) {
    var groups by remember { mutableStateOf<List<GroupCount>>(emptyList()) }

    LaunchedEffect(playlistId) {
        viewModel.loadGroupsForContentType(playlistId, "series") { groups = it }
    }

    if (groups.isEmpty()) {
        EmptyContent("No hay series")
    } else {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            groups.filter { !isAdultContent(it.groupTitle) }.forEach { group ->
                item(key = "ser_h_${group.groupTitle}") {
                    GroupHeader(
                        name = group.groupTitle,
                        count = group.cnt,
                        icon = Icons.Default.Tv,
                        subtitle = "series"
                    )
                }
                item(key = "ser_r_${group.groupTitle}") {
                    SeriesGroupRow(
                        viewModel = viewModel,
                        playlistId = playlistId,
                        groupName = group.groupTitle,
                        navController = navController
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// Series group — shows folder cards for each series
// ═══════════════════════════════════════════════════════
@Composable
private fun SeriesGroupRow(
    viewModel: PlaylistViewModel,
    playlistId: Long,
    groupName: String,
    navController: NavController
) {
    var seriesList by remember { mutableStateOf<List<SeriesInfo>>(emptyList()) }

    LaunchedEffect(playlistId, groupName) {
        viewModel.loadSeriesInfoForGroup(playlistId, groupName) { seriesList = it }
    }

    if (seriesList.isEmpty()) return

    LazyRow(
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = seriesList.filter { !isAdultContent(it.seriesName) },
            key = { "${groupName}_${it.seriesName}" }
        ) { series ->
            SeriesFolderCard(
                seriesInfo = series,
                onClick = {
                    val encoded = URLEncoder.encode(series.seriesName, "UTF-8")
                    navController.navigate("series/$playlistId/$encoded")
                }
            )
        }
    }
}

// ═══════════════════════════════════════════════════════
// Series folder card — shows poster + name + episode count
// ═══════════════════════════════════════════════════════
@Composable
private fun SeriesFolderCard(
    seriesInfo: SeriesInfo,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.1f else 1f, label = "scale"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) PrimaryIndigo else Color.Transparent, label = "border"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isFocused) CardFocused else CardBackground, label = "bg"
    )

    Card(
        modifier = Modifier
            .width(140.dp)
            .height(210.dp)
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
        Box(modifier = Modifier.fillMaxSize()) {
            // Poster / cover
            if (!seriesInfo.coverUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(seriesInfo.coverUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = seriesInfo.seriesName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Placeholder with initials
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    getCategoryColor(seriesInfo.seriesName).copy(alpha = 0.4f),
                                    CardBackground
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = seriesInfo.seriesName.take(2).uppercase(),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite.copy(alpha = 0.7f)
                    )
                }
            }

            // Folder icon badge (top-right)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = PrimaryIndigoLight,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${seriesInfo.episodeCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimaryIndigoLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }

            // Bottom name overlay
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                        )
                    )
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Column {
                    Text(
                        text = seriesInfo.seriesName,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isFocused) PrimaryIndigoLight else TextWhite,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "${seriesInfo.episodeCount} episodios",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextDimmed,
                        textAlign = TextAlign.Center,
                        fontSize = 10.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Play icon on focus
            if (isFocused) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .align(Alignment.Center)
                        .background(
                            color = PrimaryIndigo.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(20.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = "Ver serie",
                        tint = TextWhite,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// Shared composables
// ═══════════════════════════════════════════════════════

@Composable
private fun GroupChannelRow(
    viewModel: PlaylistViewModel,
    playlistId: Long,
    groupName: String,
    contentType: String,
    isPortrait: Boolean,
    navController: NavController
) {
    val channelsFlow = remember(playlistId, groupName, contentType) {
        viewModel.getChannelsByContentTypeAndGroup(playlistId, contentType, groupName)
    }
    val rawChannels by channelsFlow.collectAsState(initial = emptyList())
    val channels = remember(rawChannels) { filterSafeChannels(rawChannels) }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = channels,
            key = { it.id }
        ) { channel ->
            ChannelCard(
                channel = channel,
                isPortrait = isPortrait,
                onClick = { navController.navigate("player/$playlistId/${channel.id}") }
            )
        }
    }
}

@Composable
private fun GroupHeader(
    name: String,
    count: Int,
    icon: ImageVector,
    subtitle: String? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(getCategoryColor(name))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = getCategoryColor(name),
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.titleLarge,
            color = TextWhite,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = if (subtitle == "series") "$count eps" else "$count",
            style = MaterialTheme.typography.labelLarge,
            color = TextDimmed
        )
    }
}

@Composable
private fun ChannelCard(
    channel: Channel,
    isPortrait: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.1f else 1f, label = "scale"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) PrimaryIndigo else Color.Transparent, label = "border"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isFocused) CardFocused else CardBackground, label = "bg"
    )

    val cardWidth: Dp = if (isPortrait) 130.dp else 185.dp
    val cardHeight: Dp = if (isPortrait) 195.dp else 115.dp

    Card(
        modifier = Modifier
            .width(cardWidth)
            .height(cardHeight)
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
        Box(modifier = Modifier.fillMaxSize()) {
            if (!channel.logoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(channel.logoUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = channel.name,
                    contentScale = if (isPortrait) ContentScale.Crop else ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(if (isPortrait) 0.dp else 16.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    getCategoryColor(channel.groupTitle).copy(alpha = 0.3f),
                                    CardBackground
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = channel.name.take(2).uppercase(),
                        fontSize = if (isPortrait) 32.sp else 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite.copy(alpha = 0.7f)
                    )
                }
            }

            // Bottom name overlay
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isFocused) PrimaryIndigoLight else TextWhite,
                    fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Medium,
                    maxLines = if (isPortrait) 2 else 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Play icon on focus
            if (isFocused) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .align(Alignment.Center)
                        .background(
                            color = PrimaryIndigo.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(20.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Reproducir",
                        tint = TextWhite,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultsContent(
    searchResults: List<Channel>,
    searchQuery: String,
    playlistId: Long,
    navController: NavController
) {
    val safeResults = remember(searchResults) { filterSafeChannels(searchResults) }

    if (safeResults.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Search, contentDescription = null, tint = TextGray, modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text("Sin resultados para \"$searchQuery\"", style = MaterialTheme.typography.titleMedium, color = TextGray)
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item(key = "search_header") {
                Text(
                    text = "${safeResults.size} resultados",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextDimmed,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
            }
            item(key = "search_results") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items = safeResults, key = { it.id }) { channel ->
                        ChannelCard(
                            channel = channel,
                            isPortrait = channel.contentType != "tv",
                            onClick = { navController.navigate("player/$playlistId/${channel.id}") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyContent(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.LiveTv, contentDescription = null, tint = TextGray, modifier = Modifier.size(80.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(message, style = MaterialTheme.typography.titleLarge, color = TextGray)
        }
    }
}

@Composable
private fun ReloadButton(isRefreshing: Boolean, hasUrl: Boolean, onClick: () -> Unit) {
    if (!hasUrl) return

    var isFocused by remember { mutableStateOf(false) }
    val bgColor by animateColorAsState(
        targetValue = when {
            isFocused -> PrimaryIndigo.copy(alpha = 0.3f)
            else -> DarkSurface
        },
        label = "reloadBg"
    )

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .dpadClickable(
                onClick = onClick,
                onFocusChange = { isFocused = it }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isRefreshing) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = PrimaryIndigoLight,
                strokeWidth = 2.dp
            )
        } else {
            Icon(Icons.Default.Refresh, contentDescription = "Recargar", tint = TextWhite, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun SearchButton(isActive: Boolean, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val bgColor by animateColorAsState(
        targetValue = when {
            isActive -> PrimaryIndigo
            isFocused -> PrimaryIndigo.copy(alpha = 0.3f)
            else -> DarkSurface
        },
        label = "searchBg"
    )

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .dpadClickable(
                onClick = onClick,
                onFocusChange = { isFocused = it }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isActive) Icons.Default.Close else Icons.Default.Search,
            contentDescription = if (isActive) "Cerrar búsqueda" else "Buscar",
            tint = TextWhite,
            modifier = Modifier.size(24.dp)
        )
    }
}

private fun getCategoryColor(group: String?): Color {
    if (group == null) return Color(0xFF6366F1)
    val hash = group.hashCode()
    val colors = listOf(
        Color(0xFF6366F1), Color(0xFF14B8A6), Color(0xFFF59E0B),
        Color(0xFFEF4444), Color(0xFF22C55E), Color(0xFF3B82F6),
        Color(0xFFA855F7), Color(0xFFEC4899), Color(0xFF06B6D4),
        Color(0xFFF97316)
    )
    return colors[kotlin.math.abs(hash) % colors.size]
}
