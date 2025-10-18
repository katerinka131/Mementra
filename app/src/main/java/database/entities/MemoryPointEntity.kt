package com.example.mementra.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room Entity для точек памяти
 */
@Entity(
    tableName = "memory_points",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["user_id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["user_id"]), Index(value = ["is_favorite"])]
)
data class MemoryPointEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "point_id")
    val pointId: Long = 0,
    
    @ColumnInfo(name = "user_id")
    val userId: String,
    
    @ColumnInfo(name = "title")
    val title: String,
    
    @ColumnInfo(name = "description")
    val description: String? = null,
    
    @ColumnInfo(name = "latitude")
    val latitude: Double,
    
    @ColumnInfo(name = "longitude")
    val longitude: Double,
    
    @ColumnInfo(name = "address")
    val address: String? = null,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    
    @ColumnInfo(name = "visit_date")
    val visitDate: Long,
    
    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false
)

