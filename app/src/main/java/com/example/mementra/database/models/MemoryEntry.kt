package com.example.mementra.database.models

/**
 * Модель данных для записи воспоминания (используется в UI слое)
 * Поддерживает текст, фото (с миниатюрой) и аудио (с длительностью)
 */
data class MemoryEntry(
    val entryId: Long = 0,
    val memoryPointId: Long,
    val type: String, // "text", "photo", "audio"
    val content: String,
    val duration: Long? = null,
    val orderIndex: Int = 0,
    val filePath: String? = null,
    val fileSize: Long? = null,
    val thumbnailPath: String? = null
) {
    companion object {
        const val TYPE_TEXT = "text"
        const val TYPE_PHOTO = "photo"
        const val TYPE_AUDIO = "audio"
        const val TYPE_VIDEO = "video"
    }
}

