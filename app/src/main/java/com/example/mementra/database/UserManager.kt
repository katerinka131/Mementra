package com.example.mementra.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase

class UserManager(private val context: Context) {
    private val databaseHelper = AppDatabaseHelper(context)
    private val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    fun getUserId(): String {
        var userId = sharedPreferences.getString("user_id", null)
        if (userId == null) {
            userId = "user_${System.currentTimeMillis()}_${(1000..9999).random()}"
            sharedPreferences.edit().putString("user_id", userId).apply()
            createUser(userId)
        }
        return userId
    }

    private fun createUser(userId: String) {
        val db = databaseHelper.writableDatabase
        val values = ContentValues().apply {
            put(AppDatabaseHelper.COLUMN_USER_ID, userId)
            put(AppDatabaseHelper.COLUMN_CREATED_AT, System.currentTimeMillis())
            put(AppDatabaseHelper.COLUMN_LAST_ACTIVE, System.currentTimeMillis())
        }
        db.insert(AppDatabaseHelper.TABLE_USERS, null, values)
        db.close()
    }

    fun updateLastActive(userId: String) {
        val db = databaseHelper.writableDatabase
        val values = ContentValues().apply {
            put(AppDatabaseHelper.COLUMN_LAST_ACTIVE, System.currentTimeMillis())
        }
        db.update(
            AppDatabaseHelper.TABLE_USERS,
            values,
            "${AppDatabaseHelper.COLUMN_USER_ID} = ?",
            arrayOf(userId)
        )
        db.close()
    }
}