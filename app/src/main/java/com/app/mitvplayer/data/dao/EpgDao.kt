package com.app.mitvplayer.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.app.mitvplayer.data.models.EpgProgram
import kotlinx.coroutines.flow.Flow

@Dao
interface EpgDao {

    @Query("""
        SELECT * FROM epg_programs 
        WHERE channelEpgId = :epgId 
        AND startTime <= :now AND stopTime > :now 
        LIMIT 1
    """)
    suspend fun getCurrentProgram(epgId: String, now: Long): EpgProgram?

    @Query("""
        SELECT * FROM epg_programs 
        WHERE channelEpgId = :epgId 
        AND startTime <= :now AND stopTime > :now 
        LIMIT 1
    """)
    fun getCurrentProgramFlow(epgId: String, now: Long): Flow<EpgProgram?>

    @Query("""
        SELECT * FROM epg_programs 
        WHERE channelEpgId = :epgId 
        AND startTime >= :from AND startTime < :to 
        ORDER BY startTime ASC
    """)
    suspend fun getUpcoming(epgId: String, from: Long, to: Long): List<EpgProgram>

    @Query("""
        SELECT * FROM epg_programs 
        WHERE channelEpgId = :epgId 
        AND stopTime > :now 
        ORDER BY startTime ASC 
        LIMIT :limit
    """)
    suspend fun getNextPrograms(epgId: String, now: Long, limit: Int = 5): List<EpgProgram>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrograms(programs: List<EpgProgram>)

    @Query("DELETE FROM epg_programs WHERE stopTime < :before")
    suspend fun deleteOldPrograms(before: Long)

    @Query("DELETE FROM epg_programs")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM epg_programs")
    suspend fun getProgramCount(): Int
}
