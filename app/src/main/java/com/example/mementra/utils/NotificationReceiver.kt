package com.example.mementra.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * BroadcastReceiver для получения сигнала AlarmManager и отправки уведомления
 */
class NotificationReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        // Проверяем, включены ли уведомления
        val prefs = context.getSharedPreferences("mementra_settings", Context.MODE_PRIVATE)
        val notificationsEnabled = prefs.getBoolean("notifications_enabled", true)
        
        if (notificationsEnabled) {
            NotificationHelper.showNotification(context)
        }
    }
}

