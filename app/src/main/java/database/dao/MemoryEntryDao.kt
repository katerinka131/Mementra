package com.example.mementra.database.dao

import androidx.room.*
import com.example.mementra.database.entities.MemoryEntryEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO для работы с записями воспоминаний
 */
@Dao
interface MemoryEntryDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemoryEntry(entry: MemoryEntryEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemoryEntries(entries: List<MemoryEntryEntity>): List<Long>
    
    @Update
    suspend fun updateMemoryEntry(entry: MemoryEntryEntity)
    
    @Delete
    suspend fun deleteMemoryEntry(entry: MemoryEntryEntity)
    
    @Query("DELETE FROM memory_entries WHERE entry_id = :entryId")
    suspend fun deleteMemoryEntryById(entryId: Long)
    
    @Query("DELETE FROM memory_entries WHERE memory_point_id = :memoryPointId")
    suspend fun deleteEntriesByMemoryPointId(memoryPointId: Long)
    
    @Query("SELECT * FROM memory_entries WHERE entry_id = :entryId")
    suspend fun getMemoryEntryById(entryId: Long): MemoryEntryEntity?
    
    @Query("SELECT * FROM memory_entries WHERE memory_point_id = :memoryPointId ORDER BY order_index, created_at")
    suspend fun getEntriesByMemoryPointId(memoryPointId: Long): List<MemoryEntryEntity>
    
    @Query("SELECT * FROM memory_entries WHERE memory_point_id = :memoryPointId ORDER BY order_index, created_at")
    fun getEntriesByMemoryPointIdFlow(memoryPointId: Long): Flow<List<MemoryEntryEntity>>
    
    @Query("SELECT * FROM memory_entries WHERE memory_point_id = :memoryPointId AND type = :type ORDER BY order_index, created_at")
    suspend fun getEntriesByType(memoryPointId: Long, type: String): List<MemoryEntryEntity>
    
    @Query("SELECT COUNT(*) FROM memory_entries WHERE memory_point_id = :memoryPointId")
    suspend fun getEntriesCount(memoryPointId: Long): Int
    
    @Query("SELECT COUNT(*) FROM memory_entries WHERE memory_point_id = :memoryPointId AND type = :type")
    suspend fun getEntriesCountByType(memoryPointId: Long, type: String): Int
}

