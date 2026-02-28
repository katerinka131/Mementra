package com.example.mementra.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.mementra.database.dao.*
import com.example.mementra.database.entities.*

/**
 * Room Database для приложения Mementra
 * 
 * Версия 3: Миграция с SQLiteOpenHelper на Room
 * - Добавлена поддержка фото (file_path, file_size, thumbnail_path)
 * - Добавлена поддержка аудио (file_path, duration)
 */
@Database(
    entities = [
        UserEntity::class,
        MemoryPointEntity::class,
        MemoryEntryEntity::class,
        TagEntity::class,
        MemoryPointTagCrossRef::class
    ],
    version = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun userDao(): UserDao
    abstract fun memoryPointDao(): MemoryPointDao
    abstract fun memoryEntryDao(): MemoryEntryDao
    abstract fun tagDao(): TagDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        private const val DATABASE_NAME = "mementra.db"
        
        /**
         * Миграция с версии 2 (SQLiteOpenHelper) на версию 3 (Room)
         * Данные уже существуют, просто обновляем схему
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Таблицы уже существуют из SQLiteOpenHelper
                // Проверяем и добавляем недостающие столбцы, если их нет
                
                // Проверяем наличие столбцов в memory_entries
                try {
                    database.execSQL("""
                        CREATE TABLE IF NOT EXISTS memory_entries_new (
                            entry_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            memory_point_id INTEGER NOT NULL,
                            type TEXT NOT NULL,
                            content TEXT NOT NULL,
                            created_at INTEGER NOT NULL,
                            order_index INTEGER NOT NULL DEFAULT 0,
                            file_path TEXT,
                            file_size INTEGER,
                            thumbnail_path TEXT,
                            duration INTEGER,
                            FOREIGN KEY(memory_point_id) REFERENCES memory_points(point_id) ON DELETE CASCADE
                        )
                    """.trimIndent())
                    
                    // Копируем данные из старой таблицы
                    database.execSQL("""
                        INSERT INTO memory_entries_new 
                        (entry_id, memory_point_id, type, content, created_at, order_index, file_path, file_size, thumbnail_path, duration)
                        SELECT entry_id, memory_point_id, type, content, created_at, 
                               COALESCE(order_index, 0), file_path, file_size, thumbnail_path, duration
                        FROM memory_entries
                    """.trimIndent())
                    
                    // Удаляем старую таблицу
                    database.execSQL("DROP TABLE memory_entries")
                    
                    // Переименовываем новую таблицу
                    database.execSQL("ALTER TABLE memory_entries_new RENAME TO memory_entries")
                    
                    // Создаем индексы
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_memory_entries_memory_point_id ON memory_entries(memory_point_id)")
                } catch (e: Exception) {
                    // Если таблица уже в правильном формате, игнорируем ошибку
                }
                
                // Создаем индексы для оптимизации
                database.execSQL("CREATE INDEX IF NOT EXISTS index_memory_points_user_id ON memory_points(user_id)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_memory_points_is_favorite ON memory_points(is_favorite)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_tags_user_id ON tags(user_id)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_memory_point_tags_memory_point_id ON memory_point_tags(memory_point_id)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_memory_point_tags_tag_id ON memory_point_tags(tag_id)")
            }
        }
        
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_2_3)
                    .fallbackToDestructiveMigration() // В случае проблем с миграцией
                    .build()
                
                INSTANCE = instance
                instance
            }
        }
        
        /**
         * Для тестирования - создание in-memory базы данных
         */
        fun getInMemoryDatabase(context: Context): AppDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                AppDatabase::class.java
            ).build()
        }
    }
}

