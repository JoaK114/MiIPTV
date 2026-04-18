package com.app.mitvplayer.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val url: String? = null,
    val channelCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val epgUrl: String? = null,
    val lastUpdatedAt: Long = System.currentTimeMillis(),
    val sourceType: String = "file", // "url" or "file"
    val isDefault: Boolean = false
)
