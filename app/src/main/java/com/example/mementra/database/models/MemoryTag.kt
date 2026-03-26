package com.example.mementra.database.models

/**
 * Тег воспоминания (хранится в таблице tags, связь — memory_point_tags).
 */
data class MemoryTag(
    val tagId: Long,
    val userId: String,
    val name: String,
    val colorHex: String,
    val icon: String? = null
)
