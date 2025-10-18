package com.example.mementra.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.google.gson.Gson

/**
 * @deprecated Этот класс устарел и будет удален в следующей версии.
 * Используйте AppDatabase (Room) вместо этого.
 * См. ROOM_MIGRATION_GUIDE.md для деталей миграции.
 */
@Deprecated(
    message = "Используйте AppDatabase (Room) вместо SQLiteOpenHelper",
    replaceWith = ReplaceWith("AppDatabase", "com.example.mementra.database.AppDatabase"),
    level = DeprecationLevel.WARNING
)
class AppDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "mementra.db"
        private const val DATABASE_VERSION = 2

        // Таблицы
        const val TABLE_USERS = "users"
        const val TABLE_MEMORY_POINTS = "memory_points"
        const val TABLE_MEMORY_ENTRIES = "memory_entries"
        const val TABLE_TAGS = "tags"
        const val TABLE_MEMORY_POINT_TAGS = "memory_point_tags"

        // Столбцы для users
        const val COLUMN_USER_ID = "user_id"
        const val COLUMN_CREATED_AT = "created_at"
        const val COLUMN_LAST_ACTIVE = "last_active"
        const val COLUMN_NAME = "name"
        const val COLUMN_SETTINGS = "settings"

        // Столбцы для memory_points
        const val COLUMN_POINT_ID = "point_id"
        const val COLUMN_TITLE = "title"
        const val COLUMN_DESCRIPTION = "description"
        const val COLUMN_LATITUDE = "latitude"
        const val COLUMN_LONGITUDE = "longitude"
        const val COLUMN_ADDRESS = "address"
        const val COLUMN_VISIT_DATE = "visit_date"
        const val COLUMN_IS_FAVORITE = "is_favorite"

        // Столбцы для memory_entries
        const val COLUMN_ENTRY_ID = "entry_id"
        const val COLUMN_MEMORY_POINT_ID = "memory_point_id"
        const val COLUMN_TYPE = "type"
        const val COLUMN_CONTENT = "content"
        const val COLUMN_DURATION = "duration"
        const val COLUMN_ORDER_INDEX = "order_index"
        const val COLUMN_FILE_PATH = "file_path"
        const val COLUMN_FILE_SIZE = "file_size"
        const val COLUMN_THUMBNAIL_PATH = "thumbnail_path"

        // Столбцы для tags
        const val COLUMN_TAG_ID = "tag_id"
        const val COLUMN_TAG_NAME = "tag_name"
        const val COLUMN_TAG_COLOR = "tag_color"
        const val COLUMN_TAG_ICON = "tag_icon"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Таблица пользователей
        val createUsersTable = """
            CREATE TABLE $TABLE_USERS (
                $COLUMN_USER_ID TEXT PRIMARY KEY,
                $COLUMN_CREATED_AT INTEGER NOT NULL,
                $COLUMN_LAST_ACTIVE INTEGER NOT NULL,
                $COLUMN_NAME TEXT,
                $COLUMN_SETTINGS TEXT
            )
        """.trimIndent()

        // Таблица точек памяти
        val createMemoryPointsTable = """
            CREATE TABLE $TABLE_MEMORY_POINTS (
                $COLUMN_POINT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_USER_ID TEXT NOT NULL,
                $COLUMN_TITLE TEXT NOT NULL,
                $COLUMN_DESCRIPTION TEXT,
                $COLUMN_LATITUDE REAL NOT NULL,
                $COLUMN_LONGITUDE REAL NOT NULL,
                $COLUMN_ADDRESS TEXT,
                $COLUMN_CREATED_AT INTEGER NOT NULL,
                $COLUMN_VISIT_DATE INTEGER NOT NULL,
                $COLUMN_IS_FAVORITE INTEGER DEFAULT 0,
                FOREIGN KEY ($COLUMN_USER_ID) REFERENCES $TABLE_USERS($COLUMN_USER_ID)
            )
        """.trimIndent()

        // Таблица записей к точкам
        val createMemoryEntriesTable = """
            CREATE TABLE $TABLE_MEMORY_ENTRIES (
                $COLUMN_ENTRY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_MEMORY_POINT_ID INTEGER NOT NULL,
                $COLUMN_TYPE TEXT NOT NULL,
                $COLUMN_CONTENT TEXT NOT NULL,
                $COLUMN_CREATED_AT INTEGER NOT NULL,
                $COLUMN_DURATION INTEGER,
                $COLUMN_ORDER_INDEX INTEGER DEFAULT 0,
                $COLUMN_FILE_PATH TEXT,
                $COLUMN_FILE_SIZE INTEGER,
                $COLUMN_THUMBNAIL_PATH TEXT,
                FOREIGN KEY ($COLUMN_MEMORY_POINT_ID) REFERENCES $TABLE_MEMORY_POINTS($COLUMN_POINT_ID)
            )
        """.trimIndent()

        // Таблица тегов
        val createTagsTable = """
            CREATE TABLE $TABLE_TAGS (
                $COLUMN_TAG_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_USER_ID TEXT NOT NULL,
                $COLUMN_TAG_NAME TEXT NOT NULL,
                $COLUMN_TAG_COLOR TEXT NOT NULL,
                $COLUMN_TAG_ICON TEXT,
                FOREIGN KEY ($COLUMN_USER_ID) REFERENCES $TABLE_USERS($COLUMN_USER_ID)
            )
        """.trimIndent()

        // Таблица связи точек с тегами
        val createMemoryPointTagsTable = """
            CREATE TABLE $TABLE_MEMORY_POINT_TAGS (
                $COLUMN_MEMORY_POINT_ID INTEGER NOT NULL,
                $COLUMN_TAG_ID INTEGER NOT NULL,
                PRIMARY KEY ($COLUMN_MEMORY_POINT_ID, $COLUMN_TAG_ID),
                FOREIGN KEY ($COLUMN_MEMORY_POINT_ID) REFERENCES $TABLE_MEMORY_POINTS($COLUMN_POINT_ID),
                FOREIGN KEY ($COLUMN_TAG_ID) REFERENCES $TABLE_TAGS($COLUMN_TAG_ID)
            )
        """.trimIndent()

        // Создаем таблицы
        db.execSQL(createUsersTable)
        db.execSQL(createMemoryPointsTable)
        db.execSQL(createMemoryEntriesTable)
        db.execSQL(createTagsTable)
        db.execSQL(createMemoryPointTagsTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // При обновлении версии базы данных
        db.execSQL("DROP TABLE IF EXISTS $TABLE_MEMORY_POINT_TAGS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TAGS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_MEMORY_ENTRIES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_MEMORY_POINTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
    }
}