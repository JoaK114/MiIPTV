package com.app.mitvplayer.data.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "channels",
    foreignKeys = [
        ForeignKey(
            entity = Playlist::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("playlistId"),
        Index("contentType"),
        Index("seriesName")
    ]
)
data class Channel(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playlistId: Long,
    val name: String,
    val url: String,
    val logoUrl: String? = null,
    val groupTitle: String? = null,
    val tvgId: String? = null,
    val tvgName: String? = null,
    val duration: Long = -1,
    val orderIndex: Int = 0,
    val httpReferrer: String? = null,
    val httpUserAgent: String? = null,
    val contentType: String = "tv",      // "tv", "movie", "series"
    val seriesName: String? = null,       // Clean series name (e.g. "Breaking Bad")
    val seasonNum: Int = 0,               // Season number
    val episodeNum: Int = 0               // Episode number
)
