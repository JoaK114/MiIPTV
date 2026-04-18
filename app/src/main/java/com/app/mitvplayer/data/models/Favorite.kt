package com.app.mitvplayer.data.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "favorites",
    foreignKeys = [
        ForeignKey(
            entity = Channel::class,
            parentColumns = ["id"],
            childColumns = ["channelId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("channelId", unique = true)]
)
data class Favorite(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val channelId: Long,
    val addedAt: Long = System.currentTimeMillis()
)
