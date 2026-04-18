package com.app.mitvplayer.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.app.mitvplayer.data.models.ParentalLock
import kotlinx.coroutines.flow.Flow

@Dao
interface ParentalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun lockGroup(lock: ParentalLock)

    @Query("DELETE FROM parental_locks WHERE groupName = :groupName")
    suspend fun unlockGroup(groupName: String)

    @Query("SELECT EXISTS(SELECT 1 FROM parental_locks WHERE groupName = :groupName)")
    suspend fun isGroupLocked(groupName: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM parental_locks WHERE groupName = :groupName)")
    fun isGroupLockedFlow(groupName: String): Flow<Boolean>

    @Query("SELECT * FROM parental_locks ORDER BY groupName ASC")
    fun getAllLockedGroups(): Flow<List<ParentalLock>>

    @Query("SELECT groupName FROM parental_locks")
    suspend fun getLockedGroupNames(): List<String>

    @Query("DELETE FROM parental_locks")
    suspend fun clearAll()
}
