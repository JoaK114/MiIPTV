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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.app.mitvplayer.data.dao.GroupCount
import com.app.mitvplayer.data.models.Channel
import com.app.mitvplayer.ui.components.TVButton
import com.app.mitvplayer.ui.components.dpadClickable
import com.app.mitvplayer.ui.theme.CardBackground
import com.app.mitvplayer.ui.theme.CardFocused
import com.app.mitvplayer.ui.theme.DarkBackground
import com.app.mitvplayer.ui.theme.DarkSurface
import com.app.mitvplayer.ui.theme.PrimaryIndigo
import com.app.mitvplayer.ui.theme.PrimaryIndigoLight
import com.app.mitvplayer.ui.theme.TextDimmed
import com.app.mitvplayer.ui.theme.TextGray
import com.app.mitvplayer.ui.theme.TextWhite
import com.app.mitvplayer.viewmodel.PlaylistViewModel
import kotlinx.coroutines.delay

// ═══════════════════════════════════════════════════════
// Content type filters
// ═══════════════════════════════════════════════════════
enum class ContentFilter(val label: String) {
    ALL("Todos"),
    LIVE_TV("TV en Vivo"),
    MOVIES("Películas"),
    SERIES("Series")
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

private fun isAdultGroup(group: String): Boolean = isAdultContent(group)

private fun filterSafeChannels(channels: List<Channel>): List<Channel> {
    return channels.filter { channel ->
        !isAdultContent(channel.name) &&
        !isAdultContent(channel.groupTitle ?: "") &&
        !isAdultContent(channel.url)
    }
}

// ═══════════════════════════════════════════════════════
// Episode parsing — extract season & episode numbers
// ═══════════════════════════════════════════════════════
private data class EpisodeInfo(
    val season: Int,
    val episode: Int,
    val label: String   // e.g. "T1:E3"
)

/**
 * Parse episode info from channel name.
 * Handles: S01E05, s1e5, 1x05, T1E5, Temporada 1 Episodio 5,
 * Cap 5, Capitulo 5, Ep 5, E05, Episode 5
 */
private fun parseEpisodeInfo(name: String): EpisodeInfo? {
    val lower = name.lowercase().trim()

    // Pattern: S01E05, s1e5
    Regex("""s(\d{1,3})\s*e(\d{1,4})""", RegexOption.IGNORE_CASE)
        .find(lower)?.let {
            val s = it.groupValues[1].toInt()
            val e = it.groupValues[2].toInt()
            return EpisodeInfo(s, e, "T${s}:E${e}")
        }

    // Pattern: 1x05
    Regex("""(\d{1,2})x(\d{1,4})""")
        .find(lower)?.let {
            val s = it.groupValues[1].toInt()
            val e = it.groupValues[2].toInt()
            return EpisodeInfo(s, e, "T${s}:E${e}")
        }

    // Pattern: T1E5, T01E05
    Regex("""t(\d{1,3})\s*e(\d{1,4})""", RegexOption.IGNORE_CASE)
        .find(lower)?.let {
            val s = it.groupValues[1].toInt()
            val e = it.groupValues[2].toInt()
            return EpisodeInfo(s, e, "T${s}:E${e}")
        }

    // Pattern: Temporada 1 ... Episodio 5 / Capitulo 5
    Regex("""temporada\s*(\d{1,3}).*?(?:episodio|capitulo|cap|ep)\s*(\d{1,4})""", RegexOption.IGNORE_CASE)
        .find(lower)?.let {
            val s = it.groupValues[1].toInt()
            val e = it.groupValues[2].toInt()
            return EpisodeInfo(s, e, "T${s}:E${e}")
        }

    // Pattern: Season 1 Episode 5
    Regex("""season\s*(\d{1,3}).*?episode\s*(\d{1,4})""", RegexOption.IGNORE_CASE)
        .find(lower)?.let {
            val s = it.groupValues[1].toInt()
            val e = it.groupValues[2].toInt()
            return EpisodeInfo(s, e, "T${s}:E${e}")
        }

    // Pattern: Ep 5, Ep. 5, Ep05, Episode 5, Episodio 5, Cap 5, Capitulo 5
    Regex("""(?:ep\.?\s*|episode\s*|episodio\s*|cap(?:itulo)?\.?\s*)(\d{1,4})""", RegexOption.IGNORE_CASE)
        .find(lower)?.let {
            val e = it.groupValues[1].toInt()
            return EpisodeInfo(1, e, "Ep ${e}")
        }

    // Pattern: just "E05" or "E5" standalone
    Regex("""\be(\d{1,4})\b""", RegexOption.IGNORE_CASE)
        .find(lower)?.let {
            val e = it.groupValues[1].toInt()
            return EpisodeInfo(1, e, "Ep ${e}")
        }

    return null
}

/**
 * Sort channels by episode info (season first, then episode)
 */
private fun sortByEpisode(channels: List<Channel>): List<Channel> {
    return channels.sortedWith(compareBy(
        { parseEpisodeInfo(it.name)?.season ?: Int.MAX_VALUE },
        { parseEpisodeInfo(it.name)?.episode ?: Int.MAX_VALUE },
        { it.name.lowercase() }
    ))
}

// ═══════════════════════════════════════════════════════
// Main screen — optimized with lazy group loading
// ═══════════════════════════════════════════════════════
@Composable
fun ChannelListScreen(
    navController: NavController,
    viewModel: PlaylistViewModel,
    playlistId: Long
) {
    // Load group metadata once on entry
    LaunchedEffect(playlistId) {
        viewModel.loadGroupsForPlaylist(playlistId)
    }

    val groupsWithCounts by viewModel.groupsWithCounts.collectAsState()
    val uncategorizedCount by viewModel.uncategorizedCount.collectAsState()
    val totalChannelCount by viewModel.totalChannelCount.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()

    var selectedFilter by remember { mutableStateOf(ContentFilter.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }

    // Debounced search
    LaunchedEffect(searchQuery) {
        if (searchQuery.isBlank()) {
            viewModel.clearSearchResults()
            return@LaunchedEffect
        }
        delay(300) // debounce
        viewModel.searchChannels(playlistId, searchQuery)
    }

    // Classify groups and filter by content type
    val classifiedGroups = remember(groupsWithCounts, uncategorizedCount) {
        val groups = groupsWithCounts
            .filter { !isAdultGroup(it.groupTitle) }
            .map { gc ->
                val type = classifyGroup(gc.groupTitle)
                ClassifiedGroup(gc.groupTitle, gc.cnt, type)
            }
            .sortedBy {
                when (it.contentType) {
                    ContentFilter.LIVE_TV -> "0_${it.name.lowercase()}"
                    ContentFilter.MOVIES -> "1_${it.name.lowercase()}"
                    ContentFilter.SERIES -> "2_${it.name.lowercase()}"
                    else -> "3_${it.name.lowercase()}"
                }
            }
            .toMutableList()

        // Add uncategorized if any
        if (uncategorizedCount > 0) {
            groups.add(ClassifiedGroup("Sin Categoría", uncategorizedCount, ContentFilter.LIVE_TV))
        }
        groups.toList()
    }

    val filteredGroups = remember(classifiedGroups, selectedFilter) {
        if (selectedFilter == ContentFilter.ALL) classifiedGroups
        else classifiedGroups.filter { it.contentType == selectedFilter }
    }

    val hasMovies = remember(classifiedGroups) {
        classifiedGroups.any { it.contentType == ContentFilter.MOVIES }
    }
    val hasSeries = remember(classifiedGroups) {
        classifiedGroups.any { it.contentType == ContentFilter.SERIES }
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
        // ── Header ──
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 8.dp)
        ) {
            TVButton(
                text = "Volver",
                icon = Icons.Default.ArrowBack,
                onClick = { navController.popBackStack() }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Canales",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextWhite,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$totalChannelCount canales · ${classifiedGroups.size} categorías",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextDimmed
                )
            }

            // Search button
            SearchIconButton(
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

        // ── Search bar ──
        if (showSearch) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar canales...") },
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

        // ── Filter tabs ──
        if (hasMovies || hasSeries) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ContentFilter.entries.forEach { filter ->
                    val hasContent = when (filter) {
                        ContentFilter.ALL -> true
                        ContentFilter.LIVE_TV -> classifiedGroups.any { it.contentType == ContentFilter.LIVE_TV }
                        ContentFilter.MOVIES -> hasMovies
                        ContentFilter.SERIES -> hasSeries
                    }
                    if (hasContent) {
                        FilterChip(
                            label = filter.label,
                            isSelected = selectedFilter == filter,
                            onClick = { selectedFilter = filter }
                        )
                    }
                }
            }
        }

        // ── Content ──
        if (totalChannelCount == 0 && groupsWithCounts.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.LiveTv,
                        contentDescription = null,
                        tint = TextGray,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No hay canales", style = MaterialTheme.typography.titleLarge, color = TextGray)
                }
            }
        } else if (searchQuery.isNotBlank()) {
            // ── Search results ──
            val safeResults = remember(searchResults) { filterSafeChannels(searchResults) }

            if (safeResults.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = TextGray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Sin resultados para \"$searchQuery\"",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextGray
                        )
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
                            items(
                                items = safeResults,
                                key = { it.id }
                            ) { channel ->
                                val isMovie = classifyGroup(channel.groupTitle ?: "") != ContentFilter.LIVE_TV
                                ChannelCard(
                                    channel = channel,
                                    isPortrait = isMovie,
                                    episodeLabel = parseEpisodeInfo(channel.name)?.label,
                                    isSeries = classifyGroup(channel.groupTitle ?: "") == ContentFilter.SERIES,
                                    onClick = {
                                        navController.navigate("player/$playlistId/${channel.id}")
                                    }
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // ── Groups with lazy-loaded channels ──
            LazyColumn(
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                filteredGroups.forEach { group ->
                    item(key = "h_${group.name}") {
                        CategoryHeader(
                            name = group.name,
                            count = group.count,
                            contentType = group.contentType
                        )
                    }
                    item(key = "r_${group.name}") {
                        // Each group loads its own channels lazily
                        GroupChannelRow(
                            viewModel = viewModel,
                            playlistId = playlistId,
                            groupName = group.name,
                            contentType = group.contentType,
                            isPortrait = group.contentType == ContentFilter.MOVIES || group.contentType == ContentFilter.SERIES,
                            navController = navController
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// Classified group (lightweight, no channel list in memory)
// ═══════════════════════════════════════════════════════
private data class ClassifiedGroup(
    val name: String,
    val count: Int,
    val contentType: ContentFilter
)

// ═══════════════════════════════════════════════════════
// Lazily loaded group row — channels load only when visible
// ═══════════════════════════════════════════════════════
@Composable
private fun GroupChannelRow(
    viewModel: PlaylistViewModel,
    playlistId: Long,
    groupName: String,
    contentType: ContentFilter,
    isPortrait: Boolean,
    navController: NavController
) {
    val channelsFlow = remember(playlistId, groupName) {
        if (groupName == "Sin Categoría") {
            viewModel.getChannelsWithoutGroup(playlistId)
        } else {
            viewModel.getChannelsForGroup(playlistId, groupName)
        }
    }
    val rawChannels by channelsFlow.collectAsState(initial = emptyList())
    val channels = remember(rawChannels) {
        val safe = filterSafeChannels(rawChannels)
        if (contentType == ContentFilter.SERIES) sortByEpisode(safe) else safe
    }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = channels,
            key = { it.id }
        ) { channel ->
            val epInfo = if (contentType == ContentFilter.SERIES) {
                parseEpisodeInfo(channel.name)
            } else null
            ChannelCard(
                channel = channel,
                isPortrait = isPortrait,
                episodeLabel = epInfo?.label,
                isSeries = contentType == ContentFilter.SERIES,
                onClick = {
                    // Navigate using channel ID — no indexOf needed!
                    navController.navigate("player/$playlistId/${channel.id}")
                }
            )
        }
    }
}

// ═══════════════════════════════════════════════════════
// Search icon button
// ═══════════════════════════════════════════════════════
@Composable
private fun SearchIconButton(
    isActive: Boolean,
    onClick: () -> Unit
) {
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

// ═══════════════════════════════════════════════════════
// Category header
// ═══════════════════════════════════════════════════════
@Composable
private fun CategoryHeader(
    name: String,
    count: Int,
    contentType: ContentFilter
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
            imageVector = when (contentType) {
                ContentFilter.MOVIES -> Icons.Default.Movie
                ContentFilter.SERIES -> Icons.Default.Tv
                else -> Icons.Default.LiveTv
            },
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
            text = if (contentType == ContentFilter.SERIES) "$count episodios" else "$count",
            style = MaterialTheme.typography.labelLarge,
            color = TextDimmed
        )
    }
}

// ═══════════════════════════════════════════════════════
// Channel card with logo + episode badge
// ═══════════════════════════════════════════════════════
@Composable
private fun ChannelCard(
    channel: Channel,
    isPortrait: Boolean,
    episodeLabel: String? = null,
    isSeries: Boolean = false,
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

            // ── Episode badge (top-left) ──
            if (episodeLabel != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .background(
                            color = PrimaryIndigo.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = episodeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }

            // ── Bottom name overlay ──
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            )
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

            // ── Play icon on focus ──
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

// ═══════════════════════════════════════════════════════
// Filter chip
// ═══════════════════════════════════════════════════════
@Composable
private fun FilterChip(
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
        label = "chipBg"
    )

    Card(
        modifier = Modifier
            .dpadClickable(
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

// ═══════════════════════════════════════════════════════
// Content classification
// ═══════════════════════════════════════════════════════
private fun classifyGroup(group: String): ContentFilter {
    val lower = group.lowercase()
    return when {
        lower.contains("pelicula") || lower.contains("película") ||
        lower.contains("movie") || lower.contains("cine") ||
        lower.contains("film") || (lower.contains("vod") && lower.contains("movie")) ||
        lower.contains("peli ") || lower.contains("peliculas") -> ContentFilter.MOVIES

        lower.contains("serie") || lower.contains("novela") ||
        lower.contains("temporada") || lower.contains("season") ||
        (lower.contains("show") && !lower.contains("en vivo")) -> ContentFilter.SERIES

        else -> ContentFilter.LIVE_TV
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
