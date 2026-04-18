package com.app.mitvplayer.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.app.mitvplayer.data.AppDatabase
import com.app.mitvplayer.data.M3UParser
import com.app.mitvplayer.data.PlaylistRepository
import com.app.mitvplayer.data.XtreamRepository
import com.app.mitvplayer.data.dao.ContentTypeCount
import com.app.mitvplayer.data.dao.GroupCount
import com.app.mitvplayer.data.dao.SeriesInfo
import com.app.mitvplayer.data.epg.EpgParser
import com.app.mitvplayer.data.models.Channel
import com.app.mitvplayer.data.models.EpgProgram
import com.app.mitvplayer.data.models.Favorite
import com.app.mitvplayer.data.models.ParentalLock
import com.app.mitvplayer.data.models.Playlist
import com.app.mitvplayer.data.models.XtreamAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlaylistViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = PlaylistRepository(db.playlistDao(), db.channelDao())
    private val xtreamRepository = XtreamRepository(db.xtreamDao(), db.playlistDao(), db.channelDao())

    val playlists: StateFlow<List<Playlist>> = repository.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState

    private val _refreshState = MutableStateFlow<RefreshState>(RefreshState.Idle)
    val refreshState: StateFlow<RefreshState> = _refreshState

    // ── Cached groups data per playlist ──
    private val _groupsWithCounts = MutableStateFlow<List<GroupCount>>(emptyList())
    val groupsWithCounts: StateFlow<List<GroupCount>> = _groupsWithCounts

    private val _uncategorizedCount = MutableStateFlow(0)
    val uncategorizedCount: StateFlow<Int> = _uncategorizedCount

    private val _totalChannelCount = MutableStateFlow(0)
    val totalChannelCount: StateFlow<Int> = _totalChannelCount

    // ── Search results ──
    private val _searchResults = MutableStateFlow<List<Channel>>(emptyList())
    val searchResults: StateFlow<List<Channel>> = _searchResults

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    // ── Xtream Codes ──
    private val _xtreamState = MutableStateFlow<XtreamState>(XtreamState.Idle)
    val xtreamState: StateFlow<XtreamState> = _xtreamState

    val xtreamAccounts: StateFlow<List<XtreamAccount>> = xtreamRepository.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Favorites ──
    val favoriteChannels: StateFlow<List<Channel>> = db.favoriteDao().getAllFavoriteChannels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Parental Control ──
    private val _lockedGroups = MutableStateFlow<Set<String>>(emptySet())
    val lockedGroups: StateFlow<Set<String>> = _lockedGroups

    // ── EPG ──
    private val _epgState = MutableStateFlow<EpgState>(EpgState.Idle)
    val epgState: StateFlow<EpgState> = _epgState

    // ── Default Playlist ──
    private val _defaultPlaylistId = MutableStateFlow<Long?>(null)
    val defaultPlaylistId: StateFlow<Long?> = _defaultPlaylistId

    private val _defaultPlaylist = MutableStateFlow<Playlist?>(null)
    val defaultPlaylist: StateFlow<Playlist?> = _defaultPlaylist

    // ── Content Type Counts ──
    private val _contentTypeCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val contentTypeCounts: StateFlow<Map<String, Int>> = _contentTypeCounts

    // ── Series data ──
    private val _seriesInfoList = MutableStateFlow<List<SeriesInfo>>(emptyList())
    val seriesInfoList: StateFlow<List<SeriesInfo>> = _seriesInfoList

    private val _seriesSeasons = MutableStateFlow<List<Int>>(emptyList())
    val seriesSeasons: StateFlow<List<Int>> = _seriesSeasons

    private val _seriesEpisodes = MutableStateFlow<List<Channel>>(emptyList())
    val seriesEpisodes: StateFlow<List<Channel>> = _seriesEpisodes

    init {
        // Load locked groups on startup
        viewModelScope.launch(Dispatchers.IO) {
            _lockedGroups.value = db.parentalDao().getLockedGroupNames().toSet()
        }
        // Load default playlist on startup
        viewModelScope.launch(Dispatchers.IO) {
            val defaultPlaylist = repository.getDefaultPlaylist()
            _defaultPlaylistId.value = defaultPlaylist?.id
            _defaultPlaylist.value = defaultPlaylist
        }
    }

    // ═══════════════════════════════════════════════════════
    // Default Playlist
    // ═══════════════════════════════════════════════════════
    fun setDefaultPlaylist(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setDefaultPlaylist(id)
            val playlist = repository.getPlaylistById(id)
            _defaultPlaylistId.value = id
            _defaultPlaylist.value = playlist
        }
    }

    fun clearDefaultPlaylist() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearDefaultPlaylist()
            _defaultPlaylistId.value = null
            _defaultPlaylist.value = null
        }
    }

    fun isDefaultPlaylist(id: Long): Boolean = _defaultPlaylistId.value == id

    // ═══════════════════════════════════════════════════════
    // Content Type Counts (for tab badges)
    // ═══════════════════════════════════════════════════════
    fun loadContentTypeCounts(playlistId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val counts = repository.getContentTypeCounts(playlistId)
            _contentTypeCounts.value = counts.associate { it.contentType to it.cnt }
        }
    }

    fun getGroupsWithCountsByContentType(playlistId: Long, contentType: String): List<GroupCount> {
        // This is called synchronously within a launched coroutine
        var result = emptyList<GroupCount>()
        // Use non-suspend approach via cached data or direct call
        return result
    }

    fun loadGroupsForContentType(playlistId: Long, contentType: String, callback: (List<GroupCount>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val groups = repository.getGroupsWithCountsByContentType(playlistId, contentType)
            callback(groups)
        }
    }

    fun getChannelsByContentTypeAndGroup(playlistId: Long, contentType: String, group: String): Flow<List<Channel>> =
        repository.getChannelsByContentTypeAndGroup(playlistId, contentType, group)

    // ═══════════════════════════════════════════════════════
    // Series
    // ═══════════════════════════════════════════════════════
    fun loadSeriesInfo(playlistId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            _seriesInfoList.value = repository.getSeriesInfoList(playlistId)
        }
    }

    fun loadSeriesInfoForGroup(playlistId: Long, group: String, callback: (List<SeriesInfo>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.getSeriesInfoForGroup(playlistId, group)
            callback(result)
        }
    }

    fun loadSeriesSeasons(playlistId: Long, seriesName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _seriesSeasons.value = repository.getSeriesSeasons(playlistId, seriesName)
        }
    }

    fun loadSeriesEpisodes(playlistId: Long, seriesName: String, season: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            _seriesEpisodes.value = repository.getSeriesEpisodes(playlistId, seriesName, season)
        }
    }

    fun loadAllSeriesEpisodes(playlistId: Long, seriesName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _seriesEpisodes.value = repository.getAllSeriesEpisodes(playlistId, seriesName)
        }
    }

    // ═══════════════════════════════════════════════════════
    // M3U Import
    // ═══════════════════════════════════════════════════════
    fun importFromUrl(url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _importState.value = ImportState.Loading("Conectando…", 0f)
            try {
                val connection = java.net.URL(url).openConnection().apply {
                    connectTimeout = 15000
                    readTimeout = 60000
                    setRequestProperty("User-Agent", "MiTVPlayer/1.0")
                }

                _importState.value = ImportState.Loading("Descargando lista…", 0.1f)
                val content = connection.getInputStream().bufferedReader().readText()

                _importState.value = ImportState.Loading("Procesando canales…", 0.4f)
                val result = M3UParser.parse(content)

                if (result.channels.isEmpty()) {
                    _importState.value = ImportState.Error("No se encontraron canales en la lista")
                    return@launch
                }

                _importState.value = ImportState.Loading(
                    "Guardando ${result.channels.size} canales…", 0.6f
                )

                val name = result.playlistName ?: extractNameFromUrl(url)
                val playlistId = repository.importPlaylist(name, result, url)

                // Auto-load EPG if URL found in playlist
                if (!result.epgUrl.isNullOrBlank()) {
                    loadEpgInBackground(result.epgUrl)
                }

                _importState.value = ImportState.Success(playlistId, result.channels.size)
            } catch (e: Exception) {
                _importState.value = ImportState.Error(
                    when {
                        e.message?.contains("UnknownHost") == true -> "No se puede conectar. Verifique la URL y su conexión a internet."
                        e.message?.contains("timeout") == true -> "Tiempo de espera agotado. Intente de nuevo."
                        else -> "Error: ${e.message ?: "Error desconocido"}"
                    }
                )
            }
        }
    }

    fun importFromContent(content: String, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _importState.value = ImportState.Loading("Procesando canales…", 0.2f)
            try {
                val result = M3UParser.parse(content)

                if (result.channels.isEmpty()) {
                    _importState.value = ImportState.Error("No se encontraron canales en el archivo")
                    return@launch
                }

                _importState.value = ImportState.Loading(
                    "Guardando ${result.channels.size} canales…", 0.6f
                )

                val finalName = result.playlistName ?: name
                val playlistId = repository.importPlaylist(finalName, result)

                if (!result.epgUrl.isNullOrBlank()) {
                    loadEpgInBackground(result.epgUrl)
                }

                _importState.value = ImportState.Success(playlistId, result.channels.size)
            } catch (e: Exception) {
                _importState.value = ImportState.Error("Error al procesar el archivo: ${e.message}")
            }
        }
    }

    // ═══════════════════════════════════════════════════════
    // Playlist Refresh
    // ═══════════════════════════════════════════════════════
    fun refreshPlaylist(playlistId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            _refreshState.value = RefreshState.Loading(playlistId)
            try {
                val playlist = repository.getPlaylistById(playlistId)
                if (playlist?.url.isNullOrBlank()) {
                    _refreshState.value = RefreshState.Error("Esta lista no tiene URL de origen")
                    return@launch
                }

                val connection = java.net.URL(playlist!!.url).openConnection().apply {
                    connectTimeout = 15000
                    readTimeout = 60000
                    setRequestProperty("User-Agent", "MiTVPlayer/1.0")
                }
                val content = connection.getInputStream().bufferedReader().readText()
                val result = M3UParser.parse(content)

                if (result.channels.isEmpty()) {
                    _refreshState.value = RefreshState.Error("No se encontraron canales al actualizar")
                    return@launch
                }

                val newCount = repository.refreshPlaylist(playlistId, result)
                _refreshState.value = RefreshState.Success(playlistId, newCount)
            } catch (e: Exception) {
                _refreshState.value = RefreshState.Error(
                    when {
                        e.message?.contains("UnknownHost") == true -> "Sin conexión. Intente más tarde."
                        e.message?.contains("timeout") == true -> "Tiempo de espera agotado."
                        else -> "Error al actualizar: ${e.message ?: "Error desconocido"}"
                    }
                )
            }
        }
    }

    // ═══════════════════════════════════════════════════════
    // Groups & Channels
    // ═══════════════════════════════════════════════════════
    fun loadGroupsForPlaylist(playlistId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val groups = repository.getGroupsWithCounts(playlistId)
            _groupsWithCounts.value = groups
            _uncategorizedCount.value = repository.getUncategorizedCount(playlistId)
            _totalChannelCount.value = groups.sumOf { it.cnt } + _uncategorizedCount.value
        }
    }

    fun getChannelsForGroup(playlistId: Long, group: String): Flow<List<Channel>> =
        repository.getChannelsByGroup(playlistId, group)

    fun getChannelsWithoutGroup(playlistId: Long): Flow<List<Channel>> =
        repository.getChannelsWithoutGroup(playlistId)

    fun getChannelsForPlaylist(playlistId: Long): Flow<List<Channel>> =
        repository.getChannelsForPlaylist(playlistId)

    fun getGroupsForPlaylist(playlistId: Long): Flow<List<String>> =
        repository.getGroupsForPlaylist(playlistId)

    fun searchChannels(playlistId: Long, query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (query.isBlank()) {
                _searchResults.value = emptyList()
                _isSearching.value = false
                return@launch
            }
            _isSearching.value = true
            val results = repository.searchChannels(playlistId, query, limit = 150)
            _searchResults.value = results
            _isSearching.value = false
        }
    }

    fun clearSearchResults() {
        _searchResults.value = emptyList()
        _isSearching.value = false
    }

    suspend fun getChannelById(channelId: Long): Channel? =
        repository.getChannelById(channelId)

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            // If deleting the default, clear default
            if (_defaultPlaylistId.value == playlistId) {
                _defaultPlaylistId.value = null
                _defaultPlaylist.value = null
            }
            repository.deletePlaylist(playlistId)
        }
    }

    fun deleteAllPlaylists() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAllPlaylists()
            _defaultPlaylistId.value = null
            _defaultPlaylist.value = null
        }
    }

    // ═══════════════════════════════════════════════════════
    // Xtream Codes
    // ═══════════════════════════════════════════════════════
    fun loginXtream(serverUrl: String, username: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _xtreamState.value = XtreamState.Loading("Conectando al servidor…")
            try {
                val (accountId, authResponse) = xtreamRepository.loginAndSave(serverUrl, username, password)
                _xtreamState.value = XtreamState.Loading("Importando canales en vivo…")

                val account = xtreamRepository.getAccountById(accountId)!!

                val (livePlaylistId, liveCount) = xtreamRepository.importLiveStreams(account)
                _xtreamState.value = XtreamState.Loading("Importando películas… ($liveCount canales en vivo)")

                val (vodPlaylistId, vodCount) = try {
                    xtreamRepository.importVodStreams(account)
                } catch (_: Exception) { Pair(0L, 0) }

                _xtreamState.value = XtreamState.Loading("Importando series…")
                val (seriesPlaylistId, seriesCount) = try {
                    xtreamRepository.importSeries(account)
                } catch (_: Exception) { Pair(0L, 0) }

                val total = liveCount + vodCount + seriesCount
                _xtreamState.value = XtreamState.Success(
                    "¡Conectado! $total contenidos importados ($liveCount TV, $vodCount VOD, $seriesCount Series)"
                )
            } catch (e: Exception) {
                _xtreamState.value = XtreamState.Error(e.message ?: "Error de conexión")
            }
        }
    }

    fun connectXtreamAccount(account: XtreamAccount) {
        loginXtream(account.serverUrl, account.username, account.password)
    }

    fun deleteXtreamAccount(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            xtreamRepository.deleteAccount(id)
        }
    }

    fun resetXtreamState() {
        _xtreamState.value = XtreamState.Idle
    }

    // ═══════════════════════════════════════════════════════
    // Favorites
    // ═══════════════════════════════════════════════════════
    fun toggleFavorite(channelId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val dao = db.favoriteDao()
            if (dao.isFavoriteSync(channelId)) {
                dao.removeFavorite(channelId)
            } else {
                dao.addFavorite(Favorite(channelId = channelId))
            }
        }
    }

    fun isFavorite(channelId: Long): Flow<Boolean> =
        db.favoriteDao().isFavorite(channelId)

    // ═══════════════════════════════════════════════════════
    // Parental Controls
    // ═══════════════════════════════════════════════════════
    private val prefs = application.getSharedPreferences("mitvplayer_prefs", Context.MODE_PRIVATE)

    fun hasParentalPin(): Boolean = prefs.getString("parental_pin", null) != null

    fun setParentalPin(pin: String) {
        prefs.edit().putString("parental_pin", pin.hashCode().toString()).apply()
    }

    fun verifyParentalPin(pin: String): Boolean {
        val stored = prefs.getString("parental_pin", null) ?: return false
        return pin.hashCode().toString() == stored
    }

    fun removeParentalPin() {
        prefs.edit().remove("parental_pin").apply()
        viewModelScope.launch(Dispatchers.IO) {
            db.parentalDao().clearAll()
            _lockedGroups.value = emptySet()
        }
    }

    fun toggleGroupLock(groupName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val dao = db.parentalDao()
            if (dao.isGroupLocked(groupName)) {
                dao.unlockGroup(groupName)
            } else {
                dao.lockGroup(ParentalLock(groupName = groupName))
            }
            _lockedGroups.value = dao.getLockedGroupNames().toSet()
        }
    }

    fun isGroupLocked(groupName: String): Boolean = _lockedGroups.value.contains(groupName)

    // ═══════════════════════════════════════════════════════
    // EPG
    // ═══════════════════════════════════════════════════════
    fun loadEpg(url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _epgState.value = EpgState.Loading
            try {
                var count = 0
                EpgParser.parseFromUrl(url).collect { batch ->
                    db.epgDao().insertPrograms(batch)
                    count += batch.size
                    _epgState.value = EpgState.Loading // Keep showing loading
                }
                _epgState.value = EpgState.Success("$count programas EPG cargados")
            } catch (e: Exception) {
                _epgState.value = EpgState.Error("Error al cargar EPG: ${e.message}")
            }
        }
    }

    private fun loadEpgInBackground(url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                EpgParser.parseFromUrl(url).collect { batch ->
                    db.epgDao().insertPrograms(batch)
                }
            } catch (_: Exception) { /* best effort */ }
        }
    }

    suspend fun getCurrentEpgProgram(epgId: String): EpgProgram? {
        if (epgId.isBlank()) return null
        return db.epgDao().getCurrentProgram(epgId, System.currentTimeMillis())
    }

    suspend fun getNextEpgPrograms(epgId: String, limit: Int = 3): List<EpgProgram> {
        if (epgId.isBlank()) return emptyList()
        return db.epgDao().getNextPrograms(epgId, System.currentTimeMillis(), limit)
    }

    // ═══════════════════════════════════════════════════════
    // Buffer settings
    // ═══════════════════════════════════════════════════════
    fun getBufferPresetMs(): Int = prefs.getInt("buffer_preset_ms", 2000)

    fun setBufferPresetMs(ms: Int) {
        prefs.edit().putInt("buffer_preset_ms", ms).apply()
    }

    // ═══════════════════════════════════════════════════════
    // Reset helpers
    // ═══════════════════════════════════════════════════════
    fun resetImportState() {
        _importState.value = ImportState.Idle
    }

    fun resetRefreshState() {
        _refreshState.value = RefreshState.Idle
    }

    private fun extractNameFromUrl(url: String): String {
        return try {
            val path = java.net.URL(url).path
            val fileName = path.substringAfterLast("/").substringBeforeLast(".")
            if (fileName.isNotBlank()) fileName else "Lista importada"
        } catch (e: Exception) {
            "Lista importada"
        }
    }

    // ═══════════════════════════════════════════════════════
    // State classes
    // ═══════════════════════════════════════════════════════
    sealed class ImportState {
        object Idle : ImportState()
        data class Loading(val message: String, val progress: Float) : ImportState()
        data class Success(val playlistId: Long, val channelCount: Int) : ImportState()
        data class Error(val message: String) : ImportState()
    }

    sealed class RefreshState {
        object Idle : RefreshState()
        data class Loading(val playlistId: Long) : RefreshState()
        data class Success(val playlistId: Long, val newChannelCount: Int) : RefreshState()
        data class Error(val message: String) : RefreshState()
    }

    sealed class XtreamState {
        object Idle : XtreamState()
        data class Loading(val message: String) : XtreamState()
        data class Success(val message: String) : XtreamState()
        data class Error(val message: String) : XtreamState()
    }

    sealed class EpgState {
        object Idle : EpgState()
        object Loading : EpgState()
        data class Success(val message: String) : EpgState()
        data class Error(val message: String) : EpgState()
    }
}
