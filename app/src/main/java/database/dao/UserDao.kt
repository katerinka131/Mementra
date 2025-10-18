package com.example.mementra.database.dao

import androidx.room.*
import com.example.mementra.database.entities.UserEntity

/**
 * DAO для работы с пользователями
 */
@Dao
interface UserDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long
    
    @Update
    suspend fun updateUser(user: UserEntity)
    
    @Delete
    suspend fun deleteUser(user: UserEntity)
    
    @Query("SELECT * FROM users WHERE user_id = :userId")
    suspend fun getUserById(userId: String): UserEntity?
    
    @Query("SELECT * FROM users")
    suspend fun getAllUsers(): List<UserEntity>
    
    @Query("UPDATE users SET last_active = :timestamp WHERE user_id = :userId")
    suspend fun updateLastActive(userId: String, timestamp: Long)
}

