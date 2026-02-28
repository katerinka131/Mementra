package com.example.mementra.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room Entity для записей воспоминаний
 * Поддерживает текст, фото (с миниатюрой) и аудио (с длительностью)
 */
@Entity(
    tableName = "memory_entries",
    foreignKeys = [
        ForeignKey(
            entity = MemoryPointEntity::class,
            parentColumns = ["point_id"],
            childColumns = ["memory_point_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["memory_point_id"])]
)
data class MemoryEntryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "entry_id")
    val entryId: Long = 0,
    
    @ColumnInfo(name = "memory_point_id")
    val memoryPointId: Long,
    
    @ColumnInfo(name = "type")
    val type: String, // "text", "photo", "audio"
    
    @ColumnInfo(name = "content")
    val content: String, // Текст или описание
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    
    @ColumnInfo(name = "order_index")
    val orderIndex: Int = 0,
    
    // Для фото и аудио
    @ColumnInfo(name = "file_path")
    val filePath: String? = null,
    
    @ColumnInfo(name = "file_size")
    val fileSize: Long? = null,
    
    // Для фото - путь к миниатюре
    @ColumnInfo(name = "thumbnail_path")
    val thumbnailPath: String? = null,
    
    // Для аудио - длительность в миллисекундах
    @ColumnInfo(name = "duration")
    val duration: Long? = null
)

