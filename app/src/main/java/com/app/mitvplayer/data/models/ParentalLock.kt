package com.app.mitvplayer.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores which groups are locked behind a parental PIN.
 * The master PIN is stored in SharedPreferences (hashed).
 */
@Entity(tableName = "parental_locks")
data class ParentalLock(
    @PrimaryKey val groupName: String,
    val lockedAt: Long = System.currentTimeMillis()
)
