package com.app.mitvplayer.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.app.mitvplayer.data.models.XtreamAccount
import kotlinx.coroutines.flow.Flow

@Dao
interface XtreamDao {

    @Query("SELECT * FROM xtream_accounts ORDER BY lastLoginAt DESC")
    fun getAllAccounts(): Flow<List<XtreamAccount>>

    @Query("SELECT * FROM xtream_accounts WHERE id = :id")
    suspend fun getAccountById(id: Long): XtreamAccount?

    @Insert
    suspend fun insertAccount(account: XtreamAccount): Long

    @Update
    suspend fun updateAccount(account: XtreamAccount)

    @Query("DELETE FROM xtream_accounts WHERE id = :id")
    suspend fun deleteAccount(id: Long)

    @Query("UPDATE xtream_accounts SET lastLoginAt = :timestamp WHERE id = :id")
    suspend fun updateLastLogin(id: Long, timestamp: Long)

    @Query("SELECT * FROM xtream_accounts WHERE serverUrl = :url AND username = :user LIMIT 1")
    suspend fun findAccount(url: String, user: String): XtreamAccount?
}
