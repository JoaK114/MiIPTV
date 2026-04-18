package com.app.mitvplayer.data.api

import com.app.mitvplayer.data.models.XtreamAuthResponse
import com.app.mitvplayer.data.models.XtreamCategory
import com.app.mitvplayer.data.models.XtreamEpisode
import com.app.mitvplayer.data.models.XtreamLiveStream
import com.app.mitvplayer.data.models.XtreamSeries
import com.app.mitvplayer.data.models.XtreamSeriesInfo
import com.app.mitvplayer.data.models.XtreamVodStream
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Xtream Codes API client.
 * Implements all endpoints of /player_api.php:
 * - Authentication & account info
 * - Live TV categories & streams
 * - VOD categories & streams
 * - Series categories, series list & episode info
 */
class XtreamApiService(
    private val serverUrl: String,
    private val username: String,
    private val password: String
) {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val baseUrl: String
        get() {
            val url = serverUrl.trimEnd('/')
            return if (url.endsWith("/player_api.php")) url
            else "$url/player_api.php"
        }

    private fun buildUrl(action: String? = null, extraParams: Map<String, String> = emptyMap()): String {
        val sb = StringBuilder(baseUrl)
        sb.append("?username=$username&password=$password")
        if (action != null) sb.append("&action=$action")
        extraParams.forEach { (k, v) -> sb.append("&$k=$v") }
        return sb.toString()
    }

    @Throws(XtreamApiException::class)
    private fun executeRequest(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "MiTVPlayer/1.0 (Android)")
            .build()

        try {
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                throw XtreamApiException(
                    when (response.code) {
                        401 -> "No autorizado. Verifique sus credenciales."
                        403 -> "Acceso prohibido. ¿Su suscripción está activa?"
                        404 -> "Servidor no encontrado. Verifique la URL."
                        500 -> "Error interno del servidor."
                        503 -> "Servidor temporalmente no disponible."
                        else -> "Error del servidor (${response.code})"
                    },
                    response.code
                )
            }

            return body
        } catch (e: IOException) {
            throw XtreamApiException(
                when {
                    e.message?.contains("UnknownHost", true) == true ->
                        "No se puede conectar. Verifique la URL y su conexión a internet."
                    e.message?.contains("timeout", true) == true ->
                        "Tiempo de espera agotado. Intente de nuevo."
                    e.message?.contains("Connection refused", true) == true ->
                        "Conexión rechazada por el servidor."
                    else -> "Error de red: ${e.message ?: "Error desconocido"}"
                },
                0
            )
        }
    }

    // ═══════════════════════════════════════════════════════
    // Authentication
    // ═══════════════════════════════════════════════════════
    fun authenticate(): XtreamAuthResponse {
        val body = executeRequest(buildUrl())
        return json.decodeFromString(body)
    }

    // ═══════════════════════════════════════════════════════
    // Live TV
    // ═══════════════════════════════════════════════════════
    fun getLiveCategories(): List<XtreamCategory> {
        val body = executeRequest(buildUrl("get_live_categories"))
        return json.decodeFromString(body)
    }

    fun getLiveStreams(categoryId: String? = null): List<XtreamLiveStream> {
        val params = if (categoryId != null) mapOf("category_id" to categoryId) else emptyMap()
        val body = executeRequest(buildUrl("get_live_streams", params))
        return json.decodeFromString(body)
    }

    fun buildLiveStreamUrl(streamId: Int, extension: String = "ts"): String {
        val url = serverUrl.trimEnd('/')
        return "$url/live/$username/$password/$streamId.$extension"
    }

    // ═══════════════════════════════════════════════════════
    // VOD (Movies)
    // ═══════════════════════════════════════════════════════
    fun getVodCategories(): List<XtreamCategory> {
        val body = executeRequest(buildUrl("get_vod_categories"))
        return json.decodeFromString(body)
    }

    fun getVodStreams(categoryId: String? = null): List<XtreamVodStream> {
        val params = if (categoryId != null) mapOf("category_id" to categoryId) else emptyMap()
        val body = executeRequest(buildUrl("get_vod_streams", params))
        return json.decodeFromString(body)
    }

    fun buildVodStreamUrl(streamId: Int, extension: String = "mp4"): String {
        val url = serverUrl.trimEnd('/')
        return "$url/movie/$username/$password/$streamId.$extension"
    }

    // ═══════════════════════════════════════════════════════
    // Series
    // ═══════════════════════════════════════════════════════
    fun getSeriesCategories(): List<XtreamCategory> {
        val body = executeRequest(buildUrl("get_series_categories"))
        return json.decodeFromString(body)
    }

    fun getSeries(categoryId: String? = null): List<XtreamSeries> {
        val params = if (categoryId != null) mapOf("category_id" to categoryId) else emptyMap()
        val body = executeRequest(buildUrl("get_series", params))
        return json.decodeFromString(body)
    }

    fun getSeriesInfo(seriesId: Int): XtreamSeriesInfo {
        val body = executeRequest(buildUrl("get_series_info", mapOf("series_id" to seriesId.toString())))
        return json.decodeFromString(body)
    }

    fun buildSeriesEpisodeUrl(streamId: String, extension: String = "mp4"): String {
        val url = serverUrl.trimEnd('/')
        return "$url/series/$username/$password/$streamId.$extension"
    }

    // ═══════════════════════════════════════════════════════
    // EPG (if supported by server)
    // ═══════════════════════════════════════════════════════
    fun getXmltvUrl(): String {
        val url = serverUrl.trimEnd('/')
        return "$url/xmltv.php?username=$username&password=$password"
    }

    fun getEpgShortUrl(streamId: Int, limit: Int = 10): String {
        return buildUrl("get_short_epg", mapOf(
            "stream_id" to streamId.toString(),
            "limit" to limit.toString()
        ))
    }
}

class XtreamApiException(message: String, val httpCode: Int = 0) : Exception(message)
