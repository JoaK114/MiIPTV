package com.app.mitvplayer.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ═══════════════════════════════════════════════════════
// Room entity for saved Xtream Codes accounts
// ═══════════════════════════════════════════════════════
@Entity(tableName = "xtream_accounts")
data class XtreamAccount(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val serverUrl: String,
    val username: String,
    val password: String,
    val name: String = "",
    val status: String = "Active",
    val expirationDate: Long = 0,
    val maxConnections: Int = 1,
    val activeCons: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis()
)

// ═══════════════════════════════════════════════════════
// API response models (JSON deserialization)
// ═══════════════════════════════════════════════════════

@Serializable
data class XtreamAuthResponse(
    @SerialName("user_info") val userInfo: XtreamUserInfo? = null,
    @SerialName("server_info") val serverInfo: XtreamServerInfo? = null
)

@Serializable
data class XtreamUserInfo(
    val username: String? = null,
    val password: String? = null,
    val status: String? = null,
    @SerialName("exp_date") val expDate: String? = null,
    @SerialName("is_trial") val isTrial: String? = null,
    @SerialName("active_cons") val activeCons: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("max_connections") val maxConnections: String? = null,
    @SerialName("allowed_output_formats") val allowedOutputFormats: List<String>? = null
)

@Serializable
data class XtreamServerInfo(
    val url: String? = null,
    val port: String? = null,
    @SerialName("https_port") val httpsPort: String? = null,
    @SerialName("server_protocol") val serverProtocol: String? = null,
    @SerialName("rtmp_port") val rtmpPort: String? = null,
    val timezone: String? = null,
    @SerialName("timestamp_now") val timestampNow: Long? = null,
    @SerialName("time_now") val timeNow: String? = null
)

@Serializable
data class XtreamCategory(
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("category_name") val categoryName: String? = null,
    @SerialName("parent_id") val parentId: Int? = null
)

@Serializable
data class XtreamLiveStream(
    val num: Int? = null,
    val name: String? = null,
    @SerialName("stream_type") val streamType: String? = null,
    @SerialName("stream_id") val streamId: Int? = null,
    @SerialName("stream_icon") val streamIcon: String? = null,
    @SerialName("epg_channel_id") val epgChannelId: String? = null,
    @SerialName("added") val added: String? = null,
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("category_ids") val categoryIds: List<Int>? = null,
    @SerialName("custom_sid") val customSid: String? = null,
    @SerialName("tv_archive") val tvArchive: Int? = null,
    @SerialName("direct_source") val directSource: String? = null,
    @SerialName("tv_archive_duration") val tvArchiveDuration: Int? = null
)

@Serializable
data class XtreamVodStream(
    val num: Int? = null,
    val name: String? = null,
    @SerialName("stream_type") val streamType: String? = null,
    @SerialName("stream_id") val streamId: Int? = null,
    @SerialName("stream_icon") val streamIcon: String? = null,
    val rating: String? = null,
    @SerialName("rating_5based") val rating5based: Double? = null,
    val added: String? = null,
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("category_ids") val categoryIds: List<Int>? = null,
    @SerialName("container_extension") val containerExtension: String? = null,
    @SerialName("direct_source") val directSource: String? = null
)

@Serializable
data class XtreamSeries(
    @SerialName("series_id") val seriesId: Int? = null,
    val name: String? = null,
    val cover: String? = null,
    val plot: String? = null,
    val cast: String? = null,
    val director: String? = null,
    val genre: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    val rating: String? = null,
    @SerialName("rating_5based") val rating5based: Double? = null,
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("category_ids") val categoryIds: List<Int>? = null
)

@Serializable
data class XtreamSeriesInfo(
    val seasons: List<XtreamSeason>? = null,
    val info: XtreamSeriesDetail? = null,
    val episodes: Map<String, List<XtreamEpisode>>? = null
)

@Serializable
data class XtreamSeason(
    @SerialName("season_number") val seasonNumber: Int? = null,
    val name: String? = null,
    val cover: String? = null,
    @SerialName("episode_count") val episodeCount: String? = null
)

@Serializable
data class XtreamSeriesDetail(
    val name: String? = null,
    val cover: String? = null,
    val plot: String? = null,
    val cast: String? = null,
    val director: String? = null,
    val genre: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    val rating: String? = null,
    @SerialName("rating_5based") val rating5based: Double? = null,
    @SerialName("category_id") val categoryId: String? = null
)

@Serializable
data class XtreamEpisode(
    val id: String? = null,
    @SerialName("episode_num") val episodeNum: Int? = null,
    val title: String? = null,
    @SerialName("container_extension") val containerExtension: String? = null,
    val info: XtreamEpisodeInfo? = null,
    @SerialName("season") val season: Int? = null
)

@Serializable
data class XtreamEpisodeInfo(
    @SerialName("movie_image") val movieImage: String? = null,
    val plot: String? = null,
    @SerialName("duration_secs") val durationSecs: Int? = null,
    val duration: String? = null,
    val rating: Double? = null
)
