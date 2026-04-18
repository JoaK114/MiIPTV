package com.app.mitvplayer.player

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import androidx.media3.exoplayer.smoothstreaming.SsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.datasource.HttpDataSource

private const val TAG = "TVPlayerManager"

data class TrackInfo(
    val index: Int,
    val groupIndex: Int,
    val label: String,
    val language: String?,
    val isSelected: Boolean
)

/**
 * Detected stream format type
 */
private enum class StreamType {
    HLS,          // .m3u8
    DASH,         // .mpd
    SMOOTH,       // .ism, .isml
    RTSP,         // rtsp://
    PROGRESSIVE,  // .mp4, .mkv, .avi, .ts, .flv, .mov, .webm, etc.
    UNKNOWN       // Let ExoPlayer auto-detect
}

class TVPlayerManager(private val context: Context) {

    private var exoPlayer: ExoPlayer? = null
    private var errorListener: ((String) -> Unit)? = null
    private var trackSelector: DefaultTrackSelector? = null

    // Fallback retry state
    private var currentUrl: String? = null
    private var currentReferrer: String? = null
    private var currentUserAgent: String? = null
    private var fallbackIndex: Int = 0
    private var isRetrying: Boolean = false

    /**
     * Ordered list of stream types to try on container/parsing errors.
     * HLS first because it's by far the most common IPTV format.
     */
    private val fallbackOrder = listOf(
        StreamType.HLS,
        StreamType.PROGRESSIVE,
        StreamType.DASH,
        StreamType.UNKNOWN
    )

    private val httpDataSourceFactory = DefaultHttpDataSource.Factory()
        .setUserAgent("MiTVPlayer/1.0 (Android)")
        .setConnectTimeoutMs(15_000)
        .setReadTimeoutMs(30_000)
        .setAllowCrossProtocolRedirects(true)

    private val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)

    val player: ExoPlayer
        get() = exoPlayer ?: createPlayer().also { exoPlayer = it }

    private fun createPlayer(): ExoPlayer {
        val ts = DefaultTrackSelector(context).apply {
            setParameters(
                buildUponParameters()
                    .setMaxVideoSize(Int.MAX_VALUE, Int.MAX_VALUE)
                    .setMaxVideoBitrate(Int.MAX_VALUE)
                    .setForceHighestSupportedBitrate(true)
                    .setPreferredAudioLanguage("es")
                    .setRendererDisabled(C.TRACK_TYPE_TEXT, false)
            )
        }
        trackSelector = ts

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                5_000,    // min buffer
                120_000,  // max buffer (2 min for movies/series)
                2_000,    // playback buffer
                5_000     // rebuffer
            )
            .build()

        // DefaultMediaSourceFactory that auto-detects all formats
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

        return ExoPlayer.Builder(context)
            .setTrackSelector(ts)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .also { player ->
                player.addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        // Check if this is a container/parsing error that we can retry
                        if (isRetryableError(error) && tryNextFallback()) {
                            Log.d(TAG, "Retrying with fallback type ${fallbackOrder[fallbackIndex - 1]}")
                            return
                        }

                        // Reset retry state since we've exhausted all options
                        resetFallbackState()

                        // Try to extract HTTP status code from the cause chain
                        val httpStatusMessage = extractHttpStatusMessage(error)

                        val message = httpStatusMessage ?: when (error.errorCode) {
                            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ->
                                "Error de conexión. Verifique su internet."
                            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                                "Tiempo de espera agotado."
                            PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
                            PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED ->
                                "Formato de video no compatible. Probados todos los formatos."
                            PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED ->
                                "Error al leer el manifiesto del stream."
                            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ->
                                "Error al iniciar el decodificador."
                            PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED ->
                                "Codec de video no soportado por este dispositivo."
                            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
                                "El servidor rechazó la conexión."
                            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ->
                                "Canal no disponible."
                            PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED ->
                                "Conexión HTTP no permitida."
                            PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE ->
                                "Tipo de contenido HTTP no válido."
                            PlaybackException.ERROR_CODE_DRM_SYSTEM_ERROR ->
                                "Contenido con DRM no soportado."
                            PlaybackException.ERROR_CODE_DRM_CONTENT_ERROR ->
                                "Error de licencia DRM."
                            else -> "Error de reproducción (${error.errorCode})"
                        }
                        errorListener?.invoke(message)
                    }
                })
            }
    }

    /**
     * Check if the error is related to container parsing and can be retried
     * with a different source type.
     */
    private fun isRetryableError(error: PlaybackException): Boolean {
        return error.errorCode in listOf(
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED,
            PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE
        )
    }

    /**
     * Try the next fallback stream type. Returns true if a retry was started.
     */
    private fun tryNextFallback(): Boolean {
        val url = currentUrl ?: return false

        while (fallbackIndex < fallbackOrder.size) {
            val nextType = fallbackOrder[fallbackIndex]
            fallbackIndex++

            // Skip RTSP — doesn't apply for HTTP URLs
            // Skip the type that was already the detected type (already tried)
            val detectedType = detectStreamType(url)
            if (nextType == detectedType) continue

            Log.d(TAG, "Fallback retry #$fallbackIndex: trying $nextType for URL: $url")
            isRetrying = true

            val dsFactory = buildDataSourceFactory(currentReferrer, currentUserAgent)
            val mediaSource = buildMediaSourceForType(url, nextType, dsFactory)

            player.apply {
                stop()
                setMediaSource(mediaSource)
                prepare()
                play()
            }
            return true
        }

        return false
    }

    private fun resetFallbackState() {
        fallbackIndex = 0
        isRetrying = false
    }

    fun setOnErrorListener(listener: (String) -> Unit) {
        errorListener = listener
    }

    // ═══════════════════════════════════════════════════
    // Playback controls
    // ═══════════════════════════════════════════════════
    fun playUrl(url: String, referrer: String? = null, userAgent: String? = null) {
        // Store for fallback retries
        currentUrl = url.trim()
        currentReferrer = referrer
        currentUserAgent = userAgent
        resetFallbackState()

        val dsFactory = buildDataSourceFactory(referrer, userAgent)
        val mediaSource = buildMediaSource(url.trim(), dsFactory)
        player.apply {
            stop()
            setMediaSource(mediaSource)
            prepare()
            play()
        }
    }

    /**
     * Detect stream format from URL patterns.
     * Includes Xtream Codes API pattern detection.
     */
    private fun detectStreamType(url: String): StreamType {
        val lower = url.lowercase()
        val path = Uri.parse(url).path?.lowercase() ?: lower

        return when {
            // RTSP protocol
            lower.startsWith("rtsp://") -> StreamType.RTSP

            // HLS (.m3u8 or HLS indicators in URL)
            path.endsWith(".m3u8") ||
            lower.contains("/hls/") || lower.contains("/hls?") ||
            lower.contains("m3u8") ||
            lower.contains("/live/") && lower.contains(".m3u") -> StreamType.HLS

            // DASH (.mpd)
            path.endsWith(".mpd") ||
            lower.contains("/dash/") ||
            lower.contains("manifest.mpd") -> StreamType.DASH

            // Smooth Streaming (.ism, .isml)
            path.endsWith(".ism") || path.endsWith(".isml") ||
            path.contains(".ism/") || path.contains(".isml/") ||
            lower.contains("/smooth/") -> StreamType.SMOOTH

            // Progressive video files
            path.endsWith(".mp4") || path.endsWith(".mkv") ||
            path.endsWith(".avi") || path.endsWith(".ts") ||
            path.endsWith(".flv") || path.endsWith(".mov") ||
            path.endsWith(".webm") || path.endsWith(".wmv") ||
            path.endsWith(".mp3") || path.endsWith(".aac") ||
            path.endsWith(".ogg") || path.endsWith(".wav") ||
            path.endsWith(".3gp") || path.endsWith(".m4v") ||
            path.endsWith(".m4a") -> StreamType.PROGRESSIVE

            // Xtream Codes API pattern: /live/username/password/channelId
            // or /movie/username/password/movieId — these are almost always HLS
            isXtreamCodesUrl(path) -> StreamType.HLS

            // URLs with /live/ path commonly serve HLS in IPTV
            lower.contains("/live/") -> StreamType.HLS

            // URLs ending with a number (no extension) — common in IPTV, try HLS first
            path.matches(Regex(".*/\\d+$")) -> StreamType.HLS

            // Unknown — let ExoPlayer try with content type sniffing
            else -> StreamType.UNKNOWN
        }
    }

    /**
     * Detect Xtream Codes API URL patterns:
     * /live/username/password/channelId[.ext]
     * /movie/username/password/movieId[.ext]
     * /series/username/password/episodeId[.ext]
     */
    private fun isXtreamCodesUrl(path: String): Boolean {
        // Pattern: /(live|movie|series)/user/pass/id
        val segments = path.trimStart('/').split('/')
        if (segments.size >= 4) {
            val firstSegment = segments[0]
            if (firstSegment in listOf("live", "movie", "series")) {
                return true
            }
        }
        // Also match: /username/password/id (3-part pattern with numeric last segment)
        if (segments.size == 3) {
            val last = segments.last().split('.').first()
            if (last.all { it.isDigit() }) {
                return true
            }
        }
        return false
    }

    /**
     * Get the MIME type hint for a stream URL (helps ExoPlayer auto-detect)
     */
    private fun getMimeTypeHint(url: String, streamType: StreamType): String? {
        return when (streamType) {
            StreamType.HLS -> MimeTypes.APPLICATION_M3U8
            StreamType.DASH -> MimeTypes.APPLICATION_MPD
            StreamType.SMOOTH -> MimeTypes.APPLICATION_SS
            StreamType.PROGRESSIVE -> {
                val path = Uri.parse(url).path?.lowercase() ?: ""
                when {
                    path.endsWith(".mp4") || path.endsWith(".m4v") -> MimeTypes.VIDEO_MP4
                    path.endsWith(".mkv") -> MimeTypes.VIDEO_MATROSKA
                    path.endsWith(".webm") -> MimeTypes.VIDEO_WEBM
                    path.endsWith(".ts") -> MimeTypes.VIDEO_MP2T
                    path.endsWith(".flv") -> MimeTypes.VIDEO_FLV
                    path.endsWith(".3gp") -> MimeTypes.VIDEO_H263
                    path.endsWith(".mp3") -> MimeTypes.AUDIO_MPEG
                    path.endsWith(".aac") -> MimeTypes.AUDIO_AAC
                    path.endsWith(".ogg") -> MimeTypes.AUDIO_OGG
                    else -> null
                }
            }
            else -> null
        }
    }

    /**
     * Build a data source factory with optional custom headers.
     */
    private fun buildDataSourceFactory(
        referrer: String? = null,
        userAgent: String? = null
    ): DefaultDataSource.Factory {
        return if (referrer != null || userAgent != null) {
            val customHttp = DefaultHttpDataSource.Factory()
                .setUserAgent(userAgent ?: "MiTVPlayer/1.0 (Android)")
                .setConnectTimeoutMs(15_000)
                .setReadTimeoutMs(30_000)
                .setAllowCrossProtocolRedirects(true)

            if (referrer != null) {
                customHttp.setDefaultRequestProperties(
                    mapOf("Referer" to referrer, "Origin" to referrer)
                )
            }
            DefaultDataSource.Factory(context, customHttp)
        } else {
            dataSourceFactory
        }
    }

    /**
     * Build media source with auto-detected stream type.
     */
    private fun buildMediaSource(
        url: String,
        dsFactory: DefaultDataSource.Factory
    ): MediaSource {
        val streamType = detectStreamType(url)
        Log.d(TAG, "Detected stream type: $streamType for URL: $url")
        return buildMediaSourceForType(url, streamType, dsFactory)
    }

    /**
     * Build media source for a specific stream type.
     * Used both for initial playback and fallback retries.
     */
    private fun buildMediaSourceForType(
        url: String,
        streamType: StreamType,
        dsFactory: DefaultDataSource.Factory
    ): MediaSource {
        val mimeHint = getMimeTypeHint(url, streamType)
        val uri = Uri.parse(url)

        // Build MediaItem with MIME type hint when possible
        val mediaItemBuilder = MediaItem.Builder().setUri(uri)
        if (mimeHint != null) {
            mediaItemBuilder.setMimeType(mimeHint)
        }
        val mediaItem = mediaItemBuilder.build()

        // For UNKNOWN type, build a MediaItem without MIME hint for auto-detection
        val autoDetectItem = MediaItem.Builder().setUri(uri).build()

        return when (streamType) {
            StreamType.HLS -> {
                HlsMediaSource.Factory(dsFactory)
                    .setAllowChunklessPreparation(true)
                    .createMediaSource(mediaItem)
            }

            StreamType.DASH -> {
                DashMediaSource.Factory(dsFactory)
                    .createMediaSource(mediaItem)
            }

            StreamType.SMOOTH -> {
                SsMediaSource.Factory(dsFactory)
                    .createMediaSource(mediaItem)
            }

            StreamType.RTSP -> {
                RtspMediaSource.Factory()
                    .createMediaSource(mediaItem)
            }

            StreamType.PROGRESSIVE -> {
                ProgressiveMediaSource.Factory(dsFactory)
                    .createMediaSource(mediaItem)
            }

            StreamType.UNKNOWN -> {
                // Let DefaultMediaSourceFactory auto-detect using content type sniffing
                DefaultMediaSourceFactory(dsFactory)
                    .createMediaSource(autoDetectItem)
            }
        }
    }

    fun togglePlayPause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun seekRelative(deltaMs: Long) {
        val newPos = (player.currentPosition + deltaMs)
            .coerceIn(0, player.duration.coerceAtLeast(0))
        player.seekTo(newPos)
    }

    // ═══════════════════════════════════════════════════
    // Audio track management
    // ═══════════════════════════════════════════════════
    fun getAudioTracks(): List<TrackInfo> {
        val tracks = mutableListOf<TrackInfo>()
        for (group in player.currentTracks.groups) {
            if (group.type != C.TRACK_TYPE_AUDIO) continue
            for (i in 0 until group.length) {
                val format = group.getTrackFormat(i)
                val label = buildTrackLabel(format.label, format.language, "Audio ${tracks.size + 1}")
                tracks.add(
                    TrackInfo(
                        index = i,
                        groupIndex = tracks.size,
                        label = label,
                        language = format.language,
                        isSelected = group.isTrackSelected(i)
                    )
                )
            }
        }
        return tracks
    }

    fun selectAudioTrack(trackInfo: TrackInfo) {
        val ts = trackSelector ?: return
        for (group in player.currentTracks.groups) {
            if (group.type != C.TRACK_TYPE_AUDIO) continue
            val override = TrackSelectionOverride(group.mediaTrackGroup, trackInfo.index)
            ts.setParameters(
                ts.buildUponParameters()
                    .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                    .addOverride(override)
            )
            return
        }
    }

    // ═══════════════════════════════════════════════════
    // Subtitle track management
    // ═══════════════════════════════════════════════════
    fun getSubtitleTracks(): List<TrackInfo> {
        val tracks = mutableListOf<TrackInfo>()
        for (group in player.currentTracks.groups) {
            if (group.type != C.TRACK_TYPE_TEXT) continue
            for (i in 0 until group.length) {
                val format = group.getTrackFormat(i)
                val label = buildTrackLabel(format.label, format.language, "Subtítulo ${tracks.size + 1}")
                tracks.add(
                    TrackInfo(
                        index = i,
                        groupIndex = tracks.size,
                        label = label,
                        language = format.language,
                        isSelected = group.isTrackSelected(i)
                    )
                )
            }
        }
        return tracks
    }

    fun selectSubtitleTrack(trackInfo: TrackInfo?) {
        val ts = trackSelector ?: return
        if (trackInfo == null) {
            ts.setParameters(
                ts.buildUponParameters().setRendererDisabled(C.TRACK_TYPE_TEXT, true)
            )
            return
        }
        for (group in player.currentTracks.groups) {
            if (group.type != C.TRACK_TYPE_TEXT) continue
            val override = TrackSelectionOverride(group.mediaTrackGroup, trackInfo.index)
            ts.setParameters(
                ts.buildUponParameters()
                    .setRendererDisabled(C.TRACK_TYPE_TEXT, false)
                    .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                    .addOverride(override)
            )
            return
        }
    }

    fun areSubtitlesEnabled(): Boolean {
        val ts = trackSelector ?: return false
        return !ts.parameters.getRendererDisabled(C.TRACK_TYPE_TEXT)
    }

    // ═══════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════
    private fun buildTrackLabel(label: String?, language: String?, fallback: String): String {
        val langName = language?.let { getLanguageName(it) }
        return when {
            !label.isNullOrBlank() && langName != null -> "$label ($langName)"
            !label.isNullOrBlank() -> label
            langName != null -> langName
            else -> fallback
        }
    }

    private fun getLanguageName(code: String): String {
        return when (code.lowercase().take(2)) {
            "es" -> "Español"
            "en" -> "Inglés"
            "pt" -> "Portugués"
            "fr" -> "Francés"
            "de" -> "Alemán"
            "it" -> "Italiano"
            "ja" -> "Japonés"
            "ko" -> "Coreano"
            "zh" -> "Chino"
            "ru" -> "Ruso"
            "ar" -> "Árabe"
            "hi" -> "Hindi"
            "la" -> "Latino"
            "und" -> "Desconocido"
            else -> code.uppercase()
        }
    }

    fun release() {
        exoPlayer?.stop()
        exoPlayer?.release()
        exoPlayer = null
        errorListener = null
        trackSelector = null
        resetFallbackState()
        currentUrl = null
        currentReferrer = null
        currentUserAgent = null
    }

    /**
     * Extract HTTP status code from ExoPlayer exception chain
     * and return a user-friendly Spanish message.
     */
    private fun extractHttpStatusMessage(error: PlaybackException): String? {
        var cause: Throwable? = error.cause
        while (cause != null) {
            if (cause is HttpDataSource.InvalidResponseCodeException) {
                return when (cause.responseCode) {
                    401 -> "No autorizado (401). Verifique sus credenciales."
                    403 -> "Acceso prohibido (403). ¿Su suscripción está activa?"
                    404 -> "Canal no encontrado (404) en el servidor."
                    410 -> "Este contenido ya no está disponible (410)."
                    500 -> "Error interno del servidor (500)."
                    502 -> "Error de puerta de enlace (502)."
                    503 -> "Servidor temporalmente no disponible (503). Intente más tarde."
                    else -> "Error del servidor (${cause.responseCode})"
                }
            }
            cause = cause.cause
        }
        return null
    }

    /**
     * Set buffer duration preset (recreates the player).
     * @param bufferMs playback buffer in milliseconds (0 = minimum)
     */
    fun setBufferDuration(bufferMs: Int) {
        val wasPlaying = exoPlayer?.isPlaying == true
        val currentPos = exoPlayer?.currentPosition ?: 0
        val savedUrl = exoPlayer?.currentMediaItem?.localConfiguration?.uri?.toString()

        // Release old player
        exoPlayer?.stop()
        exoPlayer?.release()
        exoPlayer = null

        // Will be recreated on next access with new buffer settings
        // Store preference for createPlayer to use
        customBufferMs = bufferMs

        // Restart if was playing
        if (savedUrl != null && wasPlaying) {
            playUrl(savedUrl)
            player.seekTo(currentPos)
        }
    }

    private var customBufferMs: Int = 2000
}
