package com.example.mementra.database.models

/**
 * Модель данных для точки памяти (используется в UI слое)
 */
data class MemoryPoint(
    val pointId: Long = 0,
    val userId: String,
    val title: String,
    val description: String? = null,
    val latitude: Double,
    val longitude: Double,
    val address: String? = null,
    val visitDate: Long,
    val isFavorite: Boolean = false
)

