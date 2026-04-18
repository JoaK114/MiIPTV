package com.app.mitvplayer.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.app.mitvplayer.data.models.Channel
import com.app.mitvplayer.data.models.Favorite
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addFavorite(favorite: Favorite)

    @Query("DELETE FROM favorites WHERE channelId = :channelId")
    suspend fun removeFavorite(channelId: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE channelId = :channelId)")
    fun isFavorite(channelId: Long): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE channelId = :channelId)")
    suspend fun isFavoriteSync(channelId: Long): Boolean

    @Query("""
        SELECT c.* FROM channels c 
        INNER JOIN favorites f ON c.id = f.channelId 
        ORDER BY f.addedAt DESC
    """)
    fun getAllFavoriteChannels(): Flow<List<Channel>>

    @Query("SELECT COUNT(*) FROM favorites")
    fun getFavoriteCount(): Flow<Int>

    @Query("DELETE FROM favorites")
    suspend fun clearAll()
}
