package com.example.mementra.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Room Entity для связи многие-ко-многим между точками памяти и тегами
 */
@Entity(
    tableName = "memory_point_tags",
    primaryKeys = ["memory_point_id", "tag_id"],
    foreignKeys = [
        ForeignKey(
            entity = MemoryPointEntity::class,
            parentColumns = ["point_id"],
            childColumns = ["memory_point_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["tag_id"],
            childColumns = ["tag_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["memory_point_id"]),
        Index(value = ["tag_id"])
    ]
)
data class MemoryPointTagCrossRef(
    @ColumnInfo(name = "memory_point_id")
    val memoryPointId: Long,
    
    @ColumnInfo(name = "tag_id")
    val tagId: Long
)

