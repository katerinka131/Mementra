package com.example.mementra.database

import android.content.ContentValues
import android.database.Cursor
import com.example.mementra.database.models.MemoryEntry
import com.example.mementra.database.models.MemoryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * @deprecated Этот класс устарел и будет удален в следующей версии.
 * Используйте RoomMemoryRepository вместо этого.
 * См. ROOM_MIGRATION_GUIDE.md для деталей миграции.
 */
@Deprecated(
    message = "Используйте RoomMemoryRepository вместо MemoryPointRepository",
    replaceWith = ReplaceWith("RoomMemoryRepository", "com.example.mementra.database.RoomMemoryRepository"),
    level = DeprecationLevel.WARNING
)
class MemoryPointRepository(private val databaseHelper: AppDatabaseHelper) {

    private fun cursorToMemoryPoint(cursor: Cursor): MemoryPoint {
        val emojiIndex = cursor.getColumnIndex(AppDatabaseHelper.COLUMN_EMOJI)
        return MemoryPoint(
            pointId = cursor.getLong(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_POINT_ID)),
            userId = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_USER_ID)),
            title = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_TITLE)),
            description = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_DESCRIPTION)),
            latitude = cursor.getDouble(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_LATITUDE)),
            longitude = cursor.getDouble(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_LONGITUDE)),
            address = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_ADDRESS)),
            visitDate = cursor.getLong(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_VISIT_DATE)),
            isFavorite = cursor.getInt(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_IS_FAVORITE)) == 1,
            emoji = if (emojiIndex >= 0 && !cursor.isNull(emojiIndex)) cursor.getString(emojiIndex) else MemoryPoint.DEFAULT_EMOJI
        )
    }

    // Добавить точку памяти (асинхронно)
    suspend fun addMemoryPoint(memoryPoint: MemoryPoint): Long = withContext(Dispatchers.IO) {
        val db = databaseHelper.writableDatabase
        try {
            val values = ContentValues().apply {
                put(AppDatabaseHelper.COLUMN_USER_ID, memoryPoint.userId)
                put(AppDatabaseHelper.COLUMN_TITLE, memoryPoint.title)
                put(AppDatabaseHelper.COLUMN_DESCRIPTION, memoryPoint.description)
                put(AppDatabaseHelper.COLUMN_LATITUDE, memoryPoint.latitude)
                put(AppDatabaseHelper.COLUMN_LONGITUDE, memoryPoint.longitude)
                put(AppDatabaseHelper.COLUMN_ADDRESS, memoryPoint.address)
                put(AppDatabaseHelper.COLUMN_CREATED_AT, System.currentTimeMillis())
                put(AppDatabaseHelper.COLUMN_VISIT_DATE, memoryPoint.visitDate)
                put(AppDatabaseHelper.COLUMN_IS_FAVORITE, if (memoryPoint.isFavorite) 1 else 0)
                put(AppDatabaseHelper.COLUMN_EMOJI, memoryPoint.emoji)
            }
            db.insert(AppDatabaseHelper.TABLE_MEMORY_POINTS, null, values)
        } finally {
            db.close()
        }
    }

    // Получить все точки пользователя (асинхронно)
    suspend fun getMemoryPoints(userId: String): List<MemoryPoint> = withContext(Dispatchers.IO) {
        val db = databaseHelper.readableDatabase
        val points = mutableListOf<MemoryPoint>()

        try {
            val cursor = db.query(
                AppDatabaseHelper.TABLE_MEMORY_POINTS,
                null,
                "${AppDatabaseHelper.COLUMN_USER_ID} = ?",
                arrayOf(userId),
                null, null,
                "${AppDatabaseHelper.COLUMN_VISIT_DATE} DESC"
            )

            cursor.use {
                while (it.moveToNext()) {
                    points.add(cursorToMemoryPoint(it))
                }
            }
        } finally {
            db.close()
        }
        points
    }

    // Добавить запись к точке (асинхронно)
    suspend fun addMemoryEntry(entry: MemoryEntry): Long = withContext(Dispatchers.IO) {
        val db = databaseHelper.writableDatabase
        try {
            val values = ContentValues().apply {
                put(AppDatabaseHelper.COLUMN_MEMORY_POINT_ID, entry.memoryPointId)
                put(AppDatabaseHelper.COLUMN_TYPE, entry.type)
                put(AppDatabaseHelper.COLUMN_CONTENT, entry.content)
                put(AppDatabaseHelper.COLUMN_CREATED_AT, System.currentTimeMillis())
                put(AppDatabaseHelper.COLUMN_DURATION, entry.duration)
                put(AppDatabaseHelper.COLUMN_ORDER_INDEX, entry.orderIndex)
                put(AppDatabaseHelper.COLUMN_FILE_PATH, entry.filePath)
                put(AppDatabaseHelper.COLUMN_FILE_SIZE, entry.fileSize)
                put(AppDatabaseHelper.COLUMN_THUMBNAIL_PATH, entry.thumbnailPath)
            }
            db.insert(AppDatabaseHelper.TABLE_MEMORY_ENTRIES, null, values)
        } finally {
            db.close()
        }
    }

    // Удалить точку памяти (асинхронно)
    suspend fun deleteMemoryPoint(pointId: Long): Boolean = withContext(Dispatchers.IO) {
        val db = databaseHelper.writableDatabase
        try {
            // Удаляем связанные записи сначала
            db.delete(
                AppDatabaseHelper.TABLE_MEMORY_ENTRIES,
                "${AppDatabaseHelper.COLUMN_MEMORY_POINT_ID} = ?",
                arrayOf(pointId.toString())
            )

            // Удаляем связи с тегами (если есть)
            db.delete(
                AppDatabaseHelper.TABLE_MEMORY_POINT_TAGS,
                "${AppDatabaseHelper.COLUMN_MEMORY_POINT_ID} = ?",
                arrayOf(pointId.toString())
            )

            // Удаляем саму точку
            db.delete(
                AppDatabaseHelper.TABLE_MEMORY_POINTS,
                "${AppDatabaseHelper.COLUMN_POINT_ID} = ?",
                arrayOf(pointId.toString())
            ) > 0
        } finally {
            db.close()
        }
    }

    // Обновить точку памяти (асинхронно)
    suspend fun updateMemoryPoint(memoryPoint: MemoryPoint): Boolean = withContext(Dispatchers.IO) {
        val db = databaseHelper.writableDatabase
        try {
            val values = ContentValues().apply {
                put(AppDatabaseHelper.COLUMN_TITLE, memoryPoint.title)
                put(AppDatabaseHelper.COLUMN_DESCRIPTION, memoryPoint.description)
                put(AppDatabaseHelper.COLUMN_VISIT_DATE, memoryPoint.visitDate)
                put(AppDatabaseHelper.COLUMN_IS_FAVORITE, if (memoryPoint.isFavorite) 1 else 0)
                put(AppDatabaseHelper.COLUMN_EMOJI, memoryPoint.emoji)
            }

            db.update(
                AppDatabaseHelper.TABLE_MEMORY_POINTS,
                values,
                "${AppDatabaseHelper.COLUMN_POINT_ID} = ?",
                arrayOf(memoryPoint.pointId.toString())
            ) > 0
        } finally {
            db.close()
        }
    }

    // Получить избранные воспоминания (асинхронно)
    suspend fun getFavoriteMemoryPoints(userId: String): List<MemoryPoint> = withContext(Dispatchers.IO) {
        val db = databaseHelper.readableDatabase
        val points = mutableListOf<MemoryPoint>()

        try {
            val cursor = db.query(
                AppDatabaseHelper.TABLE_MEMORY_POINTS,
                null,
                "${AppDatabaseHelper.COLUMN_USER_ID} = ? AND ${AppDatabaseHelper.COLUMN_IS_FAVORITE} = ?",
                arrayOf(userId, "1"),
                null, null,
                "${AppDatabaseHelper.COLUMN_VISIT_DATE} DESC"
            )

            cursor.use {
                while (it.moveToNext()) {
                    points.add(cursorToMemoryPoint(it))
                }
            }
        } finally {
            db.close()
        }
        points
    }

    // Переключить избранное (асинхронно)
    suspend fun toggleFavorite(pointId: Long, isFavorite: Boolean): Boolean = withContext(Dispatchers.IO) {
        val db = databaseHelper.writableDatabase
        try {
            val values = ContentValues().apply {
                put(AppDatabaseHelper.COLUMN_IS_FAVORITE, if (isFavorite) 1 else 0)
            }

            db.update(
                AppDatabaseHelper.TABLE_MEMORY_POINTS,
                values,
                "${AppDatabaseHelper.COLUMN_POINT_ID} = ?",
                arrayOf(pointId.toString())
            ) > 0
        } finally {
            db.close()
        }
    }

    // Получить записи для точки (асинхронно)
    suspend fun getMemoryEntries(memoryPointId: Long): List<MemoryEntry> = withContext(Dispatchers.IO) {
        val db = databaseHelper.readableDatabase
        val entries = mutableListOf<MemoryEntry>()

        try {
            val cursor = db.query(
                AppDatabaseHelper.TABLE_MEMORY_ENTRIES,
                null,
                "${AppDatabaseHelper.COLUMN_MEMORY_POINT_ID} = ?",
                arrayOf(memoryPointId.toString()),
                null, null,
                "${AppDatabaseHelper.COLUMN_ORDER_INDEX}, ${AppDatabaseHelper.COLUMN_CREATED_AT}"
            )

            cursor.use {
                while (it.moveToNext()) {
                    val durationIdx = it.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_DURATION)
                    val filePathIdx = it.getColumnIndex(AppDatabaseHelper.COLUMN_FILE_PATH)
                    val fileSizeIdx = it.getColumnIndex(AppDatabaseHelper.COLUMN_FILE_SIZE)
                    val thumbIdx = it.getColumnIndex(AppDatabaseHelper.COLUMN_THUMBNAIL_PATH)
                    val entry = MemoryEntry(
                        entryId = it.getLong(it.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_ENTRY_ID)),
                        memoryPointId = it.getLong(it.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_MEMORY_POINT_ID)),
                        type = it.getString(it.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_TYPE)),
                        content = it.getString(it.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_CONTENT)),
                        duration = if (it.isNull(durationIdx)) null else it.getLong(durationIdx),
                        orderIndex = it.getInt(it.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_ORDER_INDEX)),
                        filePath = if (filePathIdx >= 0 && !it.isNull(filePathIdx)) it.getString(filePathIdx) else null,
                        fileSize = if (fileSizeIdx >= 0 && !it.isNull(fileSizeIdx)) it.getLong(fileSizeIdx) else null,
                        thumbnailPath = if (thumbIdx >= 0 && !it.isNull(thumbIdx)) it.getString(thumbIdx) else null
                    )
                    entries.add(entry)
                }
            }
        } finally {
            db.close()
        }
        entries
    }
}
