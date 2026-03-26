package com.example.mementra.database

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.example.mementra.database.models.MemoryEntry
import com.example.mementra.database.models.MemoryPoint
import com.example.mementra.database.models.MemoryTag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * @deprecated Этот класс устарел и будет удален в следующей версии.
 * Используйте RoomMemoryRepository вместо этого.
 */
@Deprecated(
    message = "Используйте RoomMemoryRepository вместо MemoryPointRepository",
    replaceWith = ReplaceWith("RoomMemoryRepository", "com.example.mementra.database.RoomMemoryRepository"),
    level = DeprecationLevel.WARNING
)
class MemoryPointRepository(private val databaseHelper: AppDatabaseHelper) {

    companion object {
        private val NEW_TAG_COLORS = listOf(
            "#6750A4", "#00695C", "#1565C0", "#6A1B9A", "#C62828", "#2E7D32", "#BF360C", "#455A64"
        )
    }

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
    }

    // Получить все точки пользователя (асинхронно)
    suspend fun getMemoryPoints(userId: String): List<MemoryPoint> = withContext(Dispatchers.IO) {
        val db = databaseHelper.readableDatabase
        val points = mutableListOf<MemoryPoint>()
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
        attachTagsToPoints(db, points)
    }

    private fun loadTagsForPoints(db: SQLiteDatabase, pointIds: List<Long>): Map<Long, List<MemoryTag>> {
        if (pointIds.isEmpty()) return emptyMap()
        val map = pointIds.associateWith { mutableListOf<MemoryTag>() }.toMutableMap()
        val placeholders = pointIds.joinToString(",") { "?" }
        val sql = """
            SELECT mpt.${AppDatabaseHelper.COLUMN_MEMORY_POINT_ID},
                   t.${AppDatabaseHelper.COLUMN_TAG_ID},
                   t.${AppDatabaseHelper.COLUMN_USER_ID},
                   t.${AppDatabaseHelper.COLUMN_TAG_NAME},
                   t.${AppDatabaseHelper.COLUMN_TAG_COLOR},
                   t.${AppDatabaseHelper.COLUMN_TAG_ICON}
            FROM ${AppDatabaseHelper.TABLE_MEMORY_POINT_TAGS} mpt
            INNER JOIN ${AppDatabaseHelper.TABLE_TAGS} t
                ON mpt.${AppDatabaseHelper.COLUMN_TAG_ID} = t.${AppDatabaseHelper.COLUMN_TAG_ID}
            WHERE mpt.${AppDatabaseHelper.COLUMN_MEMORY_POINT_ID} IN ($placeholders)
        """.trimIndent().replace("\n", " ")
        val args = pointIds.map { it.toString() }.toTypedArray()
        db.rawQuery(sql, args).use { c ->
            val idxPt = c.getColumnIndex(AppDatabaseHelper.COLUMN_MEMORY_POINT_ID)
            val idxTagId = c.getColumnIndex(AppDatabaseHelper.COLUMN_TAG_ID)
            val idxUid = c.getColumnIndex(AppDatabaseHelper.COLUMN_USER_ID)
            val idxName = c.getColumnIndex(AppDatabaseHelper.COLUMN_TAG_NAME)
            val idxColor = c.getColumnIndex(AppDatabaseHelper.COLUMN_TAG_COLOR)
            val idxIcon = c.getColumnIndex(AppDatabaseHelper.COLUMN_TAG_ICON)
            while (c.moveToNext()) {
                val pid = c.getLong(idxPt)
                val tag = MemoryTag(
                    tagId = c.getLong(idxTagId),
                    userId = c.getString(idxUid),
                    name = c.getString(idxName),
                    colorHex = c.getString(idxColor),
                    icon = if (idxIcon >= 0 && !c.isNull(idxIcon)) c.getString(idxIcon) else null
                )
                map[pid]?.add(tag)
            }
        }
        return map.mapValues { (_, v) -> v.toList() }
    }

    private fun attachTagsToPoints(db: SQLiteDatabase, points: List<MemoryPoint>): List<MemoryPoint> {
        val tagMap = loadTagsForPoints(db, points.map { it.pointId })
        return points.map { p -> p.copy(tags = tagMap[p.pointId] ?: emptyList()) }
    }

    suspend fun getTagsForUser(userId: String): List<MemoryTag> = withContext(Dispatchers.IO) {
        val db = databaseHelper.readableDatabase
        val out = mutableListOf<MemoryTag>()
        db.query(
            AppDatabaseHelper.TABLE_TAGS,
            null,
            "${AppDatabaseHelper.COLUMN_USER_ID} = ?",
            arrayOf(userId),
            null, null,
            "${AppDatabaseHelper.COLUMN_TAG_NAME} COLLATE NOCASE ASC"
        ).use { c ->
            while (c.moveToNext()) {
                out.add(cursorRowToMemoryTag(c))
            }
        }
        out
    }

    suspend fun getOrCreateTag(userId: String, name: String): MemoryTag = withContext(Dispatchers.IO) {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "Пустое имя тега" }
        val db = databaseHelper.writableDatabase
        db.query(
            AppDatabaseHelper.TABLE_TAGS,
            null,
            "${AppDatabaseHelper.COLUMN_USER_ID} = ? AND LOWER(${AppDatabaseHelper.COLUMN_TAG_NAME}) = LOWER(?)",
            arrayOf(userId, trimmed),
            null, null, null,
            "1"
        ).use { c ->
            if (c.moveToFirst()) return@withContext cursorRowToMemoryTag(c)
        }
        val color = NEW_TAG_COLORS[kotlin.math.abs(trimmed.hashCode()) % NEW_TAG_COLORS.size]
        val values = ContentValues().apply {
            put(AppDatabaseHelper.COLUMN_USER_ID, userId)
            put(AppDatabaseHelper.COLUMN_TAG_NAME, trimmed)
            put(AppDatabaseHelper.COLUMN_TAG_COLOR, color)
        }
        val id = db.insert(AppDatabaseHelper.TABLE_TAGS, null, values)
        MemoryTag(tagId = id, userId = userId, name = trimmed, colorHex = color, icon = null)
    }

    private fun cursorRowToMemoryTag(c: Cursor): MemoryTag {
        val iconIdx = c.getColumnIndex(AppDatabaseHelper.COLUMN_TAG_ICON)
        return MemoryTag(
            tagId = c.getLong(c.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_TAG_ID)),
            userId = c.getString(c.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_USER_ID)),
            name = c.getString(c.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_TAG_NAME)),
            colorHex = c.getString(c.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_TAG_COLOR)),
            icon = if (iconIdx >= 0 && !c.isNull(iconIdx)) c.getString(iconIdx) else null
        )
    }

    private suspend fun replacePointTags(pointId: Long, tagIds: List<Long>) = withContext(Dispatchers.IO) {
        val db = databaseHelper.writableDatabase
        db.delete(
            AppDatabaseHelper.TABLE_MEMORY_POINT_TAGS,
            "${AppDatabaseHelper.COLUMN_MEMORY_POINT_ID} = ?",
            arrayOf(pointId.toString())
        )
        for (tagId in tagIds.distinct()) {
            val cv = ContentValues().apply {
                put(AppDatabaseHelper.COLUMN_MEMORY_POINT_ID, pointId)
                put(AppDatabaseHelper.COLUMN_TAG_ID, tagId)
            }
            db.insert(AppDatabaseHelper.TABLE_MEMORY_POINT_TAGS, null, cv)
        }
    }

    suspend fun replaceMemoryPointTags(pointId: Long, tagIds: List<Long>) {
        replacePointTags(pointId, tagIds)
    }

    // Добавить запись к точке (асинхронно)
    suspend fun addMemoryEntry(entry: MemoryEntry): Long = withContext(Dispatchers.IO) {
        val db = databaseHelper.writableDatabase
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
    }

    // Удалить запись (асинхронно)
    suspend fun deleteMemoryEntry(entryId: Long): Boolean = withContext(Dispatchers.IO) {
        val db = databaseHelper.writableDatabase
        val rowsDeleted = db.delete(
            AppDatabaseHelper.TABLE_MEMORY_ENTRIES,
            "${AppDatabaseHelper.COLUMN_ENTRY_ID} = ?",
            arrayOf(entryId.toString())
        )
        Timber.d("Rows deleted: $rowsDeleted")
        rowsDeleted > 0
    }

    // Удалить точку памяти (асинхронно)
    suspend fun deleteMemoryPoint(pointId: Long): Boolean = withContext(Dispatchers.IO) {
        val db = databaseHelper.writableDatabase
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
    }

    // Обновить точку памяти (асинхронно)
    suspend fun updateMemoryPoint(memoryPoint: MemoryPoint): Boolean = withContext(Dispatchers.IO) {
        val db = databaseHelper.writableDatabase
        val values = ContentValues().apply {
            put(AppDatabaseHelper.COLUMN_TITLE, memoryPoint.title)
            put(AppDatabaseHelper.COLUMN_DESCRIPTION, memoryPoint.description)
            put(AppDatabaseHelper.COLUMN_VISIT_DATE, memoryPoint.visitDate)
            put(AppDatabaseHelper.COLUMN_IS_FAVORITE, if (memoryPoint.isFavorite) 1 else 0)
            put(AppDatabaseHelper.COLUMN_EMOJI, memoryPoint.emoji)
        }
        val ok = db.update(
            AppDatabaseHelper.TABLE_MEMORY_POINTS,
            values,
            "${AppDatabaseHelper.COLUMN_POINT_ID} = ?",
            arrayOf(memoryPoint.pointId.toString())
        ) > 0
        if (ok) {
            replacePointTags(memoryPoint.pointId, memoryPoint.tags.map { it.tagId })
        }
        ok
    }

    // Получить избранные воспоминания (асинхронно)
    suspend fun getFavoriteMemoryPoints(userId: String): List<MemoryPoint> = withContext(Dispatchers.IO) {
        val db = databaseHelper.readableDatabase
        val points = mutableListOf<MemoryPoint>()
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
        attachTagsToPoints(db, points)
    }

    // Переключить избранное (асинхронно)
    suspend fun toggleFavorite(pointId: Long, isFavorite: Boolean): Boolean = withContext(Dispatchers.IO) {
        val db = databaseHelper.writableDatabase
        val values = ContentValues().apply {
            put(AppDatabaseHelper.COLUMN_IS_FAVORITE, if (isFavorite) 1 else 0)
        }
        db.update(
            AppDatabaseHelper.TABLE_MEMORY_POINTS,
            values,
            "${AppDatabaseHelper.COLUMN_POINT_ID} = ?",
            arrayOf(pointId.toString())
        ) > 0
    }

    // Получить записи для точки (асинхронно)
    suspend fun getMemoryEntries(memoryPointId: Long): List<MemoryEntry> = withContext(Dispatchers.IO) {
        val db = databaseHelper.readableDatabase
        val entries = mutableListOf<MemoryEntry>()
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
        entries
    }
}