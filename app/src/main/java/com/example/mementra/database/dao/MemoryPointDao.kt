package com.example.mementra.database.dao

import androidx.room.*
import com.example.mementra.database.entities.MemoryPointEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO для работы с точками памяти
 */
@Dao
interface MemoryPointDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemoryPoint(memoryPoint: MemoryPointEntity): Long
    
    @Update
    suspend fun updateMemoryPoint(memoryPoint: MemoryPointEntity)
    
    @Delete
    suspend fun deleteMemoryPoint(memoryPoint: MemoryPointEntity)
    
    @Query("DELETE FROM memory_points WHERE point_id = :pointId")
    suspend fun deleteMemoryPointById(pointId: Long)
    
    @Query("SELECT * FROM memory_points WHERE point_id = :pointId")
    suspend fun getMemoryPointById(pointId: Long): MemoryPointEntity?
    
    @Query("SELECT * FROM memory_points WHERE user_id = :userId ORDER BY visit_date DESC")
    suspend fun getMemoryPointsByUser(userId: String): List<MemoryPointEntity>
    
    @Query("SELECT * FROM memory_points WHERE user_id = :userId ORDER BY visit_date DESC")
    fun getMemoryPointsByUserFlow(userId: String): Flow<List<MemoryPointEntity>>
    
    @Query("SELECT * FROM memory_points WHERE user_id = :userId AND is_favorite = 1 ORDER BY visit_date DESC")
    suspend fun getFavoriteMemoryPoints(userId: String): List<MemoryPointEntity>
    
    @Query("SELECT * FROM memory_points WHERE user_id = :userId AND is_favorite = 1 ORDER BY visit_date DESC")
    fun getFavoriteMemoryPointsFlow(userId: String): Flow<List<MemoryPointEntity>>
    
    @Query("UPDATE memory_points SET is_favorite = :isFavorite WHERE point_id = :pointId")
    suspend fun updateFavoriteStatus(pointId: Long, isFavorite: Boolean)
    
    @Query("SELECT COUNT(*) FROM memory_points WHERE user_id = :userId")
    suspend fun getMemoryPointsCount(userId: String): Int
    
    @Query("SELECT COUNT(*) FROM memory_points WHERE user_id = :userId AND is_favorite = 1")
    suspend fun getFavoriteMemoryPointsCount(userId: String): Int
}

