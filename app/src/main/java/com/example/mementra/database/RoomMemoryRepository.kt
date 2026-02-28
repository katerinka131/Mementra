package com.example.mementra.database

import com.example.mementra.database.dao.MemoryEntryDao
import com.example.mementra.database.dao.MemoryPointDao
import com.example.mementra.database.entities.MemoryEntryEntity
import com.example.mementra.database.entities.MemoryPointEntity
import com.example.mementra.database.models.MemoryEntry
import com.example.mementra.database.models.MemoryPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository для работы с воспоминаниями через Room Database
 * Заменяет старый MemoryPointRepository
 */
class RoomMemoryRepository(
    private val memoryPointDao: MemoryPointDao,
    private val memoryEntryDao: MemoryEntryDao
) {
    
    // ==================== MemoryPoint операции ====================
    
    /**
     * Добавить точку памяти
     */
    suspend fun addMemoryPoint(memoryPoint: MemoryPoint): Long {
        val entity = MemoryPointEntity(
            pointId = memoryPoint.pointId,
            userId = memoryPoint.userId,
            title = memoryPoint.title,
            description = memoryPoint.description,
            latitude = memoryPoint.latitude,
            longitude = memoryPoint.longitude,
            address = memoryPoint.address,
            createdAt = System.currentTimeMillis(),
            visitDate = memoryPoint.visitDate,
            isFavorite = memoryPoint.isFavorite
        )
        return memoryPointDao.insertMemoryPoint(entity)
    }
    
    /**
     * Получить все точки пользователя
     */
    suspend fun getMemoryPoints(userId: String): List<MemoryPoint> {
        return memoryPointDao.getMemoryPointsByUser(userId).map { it.toMemoryPoint() }
    }
    
    /**
     * Получить все точки пользователя (Flow для реактивного обновления)
     */
    fun getMemoryPointsFlow(userId: String): Flow<List<MemoryPoint>> {
        return memoryPointDao.getMemoryPointsByUserFlow(userId).map { list ->
            list.map { it.toMemoryPoint() }
        }
    }
    
    /**
     * Получить избранные воспоминания
     */
    suspend fun getFavoriteMemoryPoints(userId: String): List<MemoryPoint> {
        return memoryPointDao.getFavoriteMemoryPoints(userId).map { it.toMemoryPoint() }
    }
    
    /**
     * Получить избранные воспоминания (Flow)
     */
    fun getFavoriteMemoryPointsFlow(userId: String): Flow<List<MemoryPoint>> {
        return memoryPointDao.getFavoriteMemoryPointsFlow(userId).map { list ->
            list.map { it.toMemoryPoint() }
        }
    }
    
    /**
     * Переключить избранное
     */
    suspend fun toggleFavorite(pointId: Long, isFavorite: Boolean): Boolean {
        return try {
            memoryPointDao.updateFavoriteStatus(pointId, isFavorite)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Удалить точку памяти
     */
    suspend fun deleteMemoryPoint(pointId: Long): Boolean {
        return try {
            memoryPointDao.deleteMemoryPointById(pointId)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Обновить точку памяти
     */
    suspend fun updateMemoryPoint(memoryPoint: MemoryPoint): Boolean {
        return try {
            val entity = MemoryPointEntity(
                pointId = memoryPoint.pointId,
                userId = memoryPoint.userId,
                title = memoryPoint.title,
                description = memoryPoint.description,
                latitude = memoryPoint.latitude,
                longitude = memoryPoint.longitude,
                address = memoryPoint.address,
                createdAt = System.currentTimeMillis(),
                visitDate = memoryPoint.visitDate,
                isFavorite = memoryPoint.isFavorite
            )
            memoryPointDao.updateMemoryPoint(entity)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Получить количество воспоминаний
     */
    suspend fun getMemoryPointsCount(userId: String): Int {
        return memoryPointDao.getMemoryPointsCount(userId)
    }
    
    // ==================== MemoryEntry операции ====================
    
    /**
     * Добавить запись к точке
     */
    suspend fun addMemoryEntry(entry: MemoryEntry): Long {
        val entity = MemoryEntryEntity(
            entryId = entry.entryId,
            memoryPointId = entry.memoryPointId,
            type = entry.type,
            content = entry.content,
            createdAt = System.currentTimeMillis(),
            orderIndex = entry.orderIndex,
            filePath = entry.filePath,
            fileSize = entry.fileSize,
            thumbnailPath = entry.thumbnailPath,
            duration = entry.duration
        )
        return memoryEntryDao.insertMemoryEntry(entity)
    }
    
    /**
     * Получить записи для точки
     */
    suspend fun getMemoryEntries(memoryPointId: Long): List<MemoryEntry> {
        return memoryEntryDao.getEntriesByMemoryPointId(memoryPointId).map { it.toMemoryEntry() }
    }
    
    /**
     * Получить записи для точки (Flow)
     */
    fun getMemoryEntriesFlow(memoryPointId: Long): Flow<List<MemoryEntry>> {
        return memoryEntryDao.getEntriesByMemoryPointIdFlow(memoryPointId).map { list ->
            list.map { it.toMemoryEntry() }
        }
    }
    
    /**
     * Получить записи определенного типа
     */
    suspend fun getMemoryEntriesByType(memoryPointId: Long, type: String): List<MemoryEntry> {
        return memoryEntryDao.getEntriesByType(memoryPointId, type).map { it.toMemoryEntry() }
    }
    
    /**
     * Удалить запись
     */
    suspend fun deleteMemoryEntry(entryId: Long): Boolean {
        return try {
            memoryEntryDao.deleteMemoryEntryById(entryId)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    // ==================== Маппинг Entity <-> Model ====================
    
    private fun MemoryPointEntity.toMemoryPoint() = MemoryPoint(
        pointId = pointId,
        userId = userId,
        title = title,
        description = description,
        latitude = latitude,
        longitude = longitude,
        address = address,
        visitDate = visitDate,
        isFavorite = isFavorite
    )
    
    private fun MemoryEntryEntity.toMemoryEntry() = MemoryEntry(
        entryId = entryId,
        memoryPointId = memoryPointId,
        type = type,
        content = content,
        duration = duration,
        orderIndex = orderIndex,
        filePath = filePath,
        fileSize = fileSize,
        thumbnailPath = thumbnailPath
    )
}

