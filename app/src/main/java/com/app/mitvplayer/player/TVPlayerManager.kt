package com.app.mitvplayer.player

import android.content.Context
import android.net.Uri
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
                        // Try to extract HTTP status code from the cause chain
                        val httpStatusMessage = extractHttpStatusMessage(error)

                        val message = httpStatusMessage ?: when (error.errorCode) {
                            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ->
                                "Error de conexión. Verifique su internet."
                            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                                "Tiempo de espera agotado."
                            PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED ->
                                "Formato de video no soportado."
                            PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED ->
                                "Contenedor de video no compatible."
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

    fun setOnErrorListener(listener: (String) -> Unit) {
        errorListener = listener
    }

    // ═══════════════════════════════════════════════════
    // Playback controls
    // ═══════════════════════════════════════════════════
    fun playUrl(url: String, referrer: String? = null, userAgent: String? = null) {
        val mediaSource = buildMediaSource(url.trim(), referrer, userAgent)
        player.apply {
            stop()
            setMediaSource(mediaSource)
            prepare()
            play()
        }
    }

    /**
     * Detect stream format from URL patterns
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

            // Unknown — let ExoPlayer try with content type sniffing
            else -> StreamType.UNKNOWN
        }
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

    private fun buildMediaSource(
        url: String,
        referrer: String? = null,
        userAgent: String? = null
    ): MediaSource {
        // Build data source factory (per-channel if custom headers)
        val dsFactory = if (referrer != null || userAgent != null) {
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

        val streamType = detectStreamType(url)
        val mimeHint = getMimeTypeHint(url, streamType)
        val uri = Uri.parse(url)

        // Build MediaItem with MIME type hint when possible
        val mediaItemBuilder = MediaItem.Builder().setUri(uri)
        if (mimeHint != null) {
            mediaItemBuilder.setMimeType(mimeHint)
        }
        val mediaItem = mediaItemBuilder.build()

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
                    .createMediaSource(mediaItem)
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
        val currentUrl = exoPlayer?.currentMediaItem?.localConfiguration?.uri?.toString()

        // Release old player
        exoPlayer?.stop()
        exoPlayer?.release()
        exoPlayer = null

        // Will be recreated on next access with new buffer settings
        // Store preference for createPlayer to use
        customBufferMs = bufferMs

        // Restart if was playing
        if (currentUrl != null && wasPlaying) {
            playUrl(currentUrl)
            player.seekTo(currentPos)
        }
    }

    private var customBufferMs: Int = 2000
}
