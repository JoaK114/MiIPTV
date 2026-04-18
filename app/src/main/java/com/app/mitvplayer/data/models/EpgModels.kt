package com.app.mitvplayer.data.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "epg_programs",
    indices = [
        Index("channelEpgId"),
        Index("startTime"),
        Index("stopTime")
    ]
)
data class EpgProgram(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val channelEpgId: String,      // matches tvg-id from channel
    val startTime: Long,            // epoch millis
    val stopTime: Long,             // epoch millis
    val title: String,
    val description: String? = null,
    val category: String? = null,
    val icon: String? = null
)
