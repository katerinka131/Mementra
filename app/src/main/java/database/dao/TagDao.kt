package com.example.mementra.database.dao

import androidx.room.*
import com.example.mementra.database.entities.MemoryPointTagCrossRef
import com.example.mementra.database.entities.TagEntity

/**
 * DAO для работы с тегами
 */
@Dao
interface TagDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: TagEntity): Long
    
    @Update
    suspend fun updateTag(tag: TagEntity)
    
    @Delete
    suspend fun deleteTag(tag: TagEntity)
    
    @Query("DELETE FROM tags WHERE tag_id = :tagId")
    suspend fun deleteTagById(tagId: Long)
    
    @Query("SELECT * FROM tags WHERE tag_id = :tagId")
    suspend fun getTagById(tagId: Long): TagEntity?
    
    @Query("SELECT * FROM tags WHERE user_id = :userId ORDER BY tag_name")
    suspend fun getTagsByUser(userId: String): List<TagEntity>
    
    // Работа со связями многие-ко-многим
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemoryPointTagCrossRef(crossRef: MemoryPointTagCrossRef)
    
    @Delete
    suspend fun deleteMemoryPointTagCrossRef(crossRef: MemoryPointTagCrossRef)
    
    @Query("DELETE FROM memory_point_tags WHERE memory_point_id = :memoryPointId")
    suspend fun deleteAllTagsForMemoryPoint(memoryPointId: Long)
    
    @Query("""
        SELECT tags.* FROM tags
        INNER JOIN memory_point_tags ON tags.tag_id = memory_point_tags.tag_id
        WHERE memory_point_tags.memory_point_id = :memoryPointId
        ORDER BY tags.tag_name
    """)
    suspend fun getTagsForMemoryPoint(memoryPointId: Long): List<TagEntity>
    
    @Query("""
        SELECT memory_point_id FROM memory_point_tags
        WHERE tag_id = :tagId
    """)
    suspend fun getMemoryPointIdsForTag(tagId: Long): List<Long>
}

