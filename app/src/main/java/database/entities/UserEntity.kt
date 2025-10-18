package com.example.mementra.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity для пользователей
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    @ColumnInfo(name = "user_id")
    val userId: String,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    
    @ColumnInfo(name = "last_active")
    val lastActive: Long,
    
    @ColumnInfo(name = "name")
    val name: String? = null,
    
    @ColumnInfo(name = "settings")
    val settings: String? = null
)

