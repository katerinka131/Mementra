package com.example.mementra.database

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase

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

data class MemoryEntry(
    val entryId: Long = 0,
    val memoryPointId: Long,
    val type: String, // "text", "voice", "photo"
    val content: String,
    val duration: Long? = null,
    val orderIndex: Int = 0
)

class MemoryPointRepository(private val databaseHelper: AppDatabaseHelper) {

    // Добавить точку памяти
    fun addMemoryPoint(memoryPoint: MemoryPoint): Long {
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
        }
        val id = db.insert(AppDatabaseHelper.TABLE_MEMORY_POINTS, null, values)
        db.close()
        return id
    }

    // Получить все точки пользователя
    fun getMemoryPoints(userId: String): List<MemoryPoint> {
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
                val point = MemoryPoint(
                    pointId = it.getLong(it.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_POINT_ID)),
                    userId = it.getString(it.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_USER_ID)),
                    title = it.getString(it.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_TITLE)),
                    description = it.getString(it.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_DESCRIPTION)),
                    latitude = it.getDouble(it.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_LATITUDE)),
                    longitude = it.getDouble(it.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_LONGITUDE)),
                    address = it.getString(it.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_ADDRESS)),
                    visitDate = it.getLong(it.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_VISIT_DATE)),
                    isFavorite = it.getInt(it.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_IS_FAVORITE)) == 1
                )
                points.add(point)
            }
        }
        db.close()
        return points
    }

    // Добавить запись к точке
    fun addMemoryEntry(entry: MemoryEntry): Long {
        val db = databaseHelper.writableDatabase
        val values = ContentValues().apply {
            put(AppDatabaseHelper.COLUMN_MEMORY_POINT_ID, entry.memoryPointId)
            put(AppDatabaseHelper.COLUMN_TYPE, entry.type)
            put(AppDatabaseHelper.COLUMN_CONTENT, entry.content)
            put(AppDatabaseHelper.COLUMN_CREATED_AT, System.currentTimeMillis())
            put(AppDatabaseHelper.COLUMN_DURATION, entry.duration)
            put(AppDatabaseHelper.COLUMN_ORDER_INDEX, entry.orderIndex)
        }
        val id = db.insert(AppDatabaseHelper.TABLE_MEMORY_ENTRIES, null, values)
        db.close()
        return id
    }
    // Добавьте этот метод в MemoryPointRepository.kt
    fun deleteMemoryPoint(pointId: Long): Boolean {
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
        val deleted = db.delete(
            AppDatabaseHelper.TABLE_MEMORY_POINTS,
            "${AppDatabaseHelper.COLUMN_POINT_ID} = ?",
            arrayOf(pointId.toString())
        ) > 0

        db.close()
        return deleted
    }
    // Добавьте этот метод в MemoryPointRepository.kt
    fun updateMemoryPoint(memoryPoint: MemoryPoint): Boolean {
        val db = databaseHelper.writableDatabase
        val values = ContentValues().apply {
            put(AppDatabaseHelper.COLUMN_TITLE, memoryPoint.title)
            put(AppDatabaseHelper.COLUMN_DESCRIPTION, memoryPoint.description)
            put(AppDatabaseHelper.COLUMN_VISIT_DATE, memoryPoint.visitDate)
            put(AppDatabaseHelper.COLUMN_IS_FAVORITE, if (memoryPoint.isFavorite) 1 else 0)
        }

        val updated = db.update(
            AppDatabaseHelper.TABLE_MEMORY_POINTS,
            values,
            "${AppDatabaseHelper.COLUMN_POINT_ID} = ?",
            arrayOf(memoryPoint.pointId.toString())
        ) > 0

        db.close()
        return updated
    }
    // Добавьте эти методы в MemoryPointRepository.kt

    // Получить избранные воспоминания
    fun getFavoriteMemoryPoints(userId: String): List<MemoryPoint> {
        val db = databaseHelper.readableDatabase
        val points = mutableListOf<MemoryPoint>()

        val cursor = db.query(
            AppDatabaseHelper.TABLE_MEMORY_POINTS,
            null,
            "${AppDatabaseHelper.COLUMN_USER_ID} = ? AND ${AppDatabaseHelper.COLUMN_IS_FAVORITE} = ?",
            arrayOf(userId, "1"), // ВАЖНО: "1" для true
            null, null,
            "${AppDatabaseHelper.COLUMN_VISIT_DATE} DESC"
        )

        cursor.use {
            while (it.moveToNext()) {
                val point = MemoryPoint(
                    pointId = it.getLong(it.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_POINT_ID)),
                    userId = it.getString(it.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_USER_ID)),
                    title = it.getString(it.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_TITLE)),
                    description = it.getString(it.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_DESCRIPTION)),
                    latitude = it.getDouble(it.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_LATITUDE)),
                    longitude = it.getDouble(it.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_LONGITUDE)),
                    address = it.getString(it.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_ADDRESS)),
                    visitDate = it.getLong(it.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_VISIT_DATE)),
                    isFavorite = it.getInt(it.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_IS_FAVORITE)) == 1
                )
                points.add(point)
            }
        }
        db.close()
        return points
    }

    // Переключить избранное
    fun toggleFavorite(pointId: Long, isFavorite: Boolean): Boolean {
        val db = databaseHelper.writableDatabase
        val values = ContentValues().apply {
            put(AppDatabaseHelper.COLUMN_IS_FAVORITE, if (isFavorite) 1 else 0)
        }

        val updated = db.update(
            AppDatabaseHelper.TABLE_MEMORY_POINTS,
            values,
            "${AppDatabaseHelper.COLUMN_POINT_ID} = ?",
            arrayOf(pointId.toString())
        ) > 0

        db.close()
        return updated
    }
    // Получить записи для точки
    fun getMemoryEntries(memoryPointId: Long): List<MemoryEntry> {
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
                val entry = MemoryEntry(
                    entryId = it.getLong(it.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_ENTRY_ID)),
                    memoryPointId = it.getLong(it.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_MEMORY_POINT_ID)),
                    type = it.getString(it.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_TYPE)),
                    content = it.getString(it.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_CONTENT)),
                    duration = it.getLong(it.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_DURATION)),
                    orderIndex = it.getInt(it.getColumnIndexOrThrow(AppDatabaseHelper.COLUMN_ORDER_INDEX))
                )
                entries.add(entry)
            }
        }
        db.close()
        return entries
    }
}