package com.example.mementra

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import timber.log.Timber

/**
 * Application класс для инициализации глобальных компонентов
 */
class MementraApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // Применяем сохраненную тему при запуске приложения
        applyTheme()
        
        // Инициализация Timber для логирования
        if (BuildConfig.DEBUG) {
            // В режиме отладки используем DebugTree
            Timber.plant(Timber.DebugTree())
            Timber.d("Mementra Application started in DEBUG mode")
        } else {
            // В production режиме можно использовать кастомное дерево
            // которое отправляет логи в аналитику (Firebase Crashlytics и т.д.)
            Timber.plant(ReleaseTree())
            Timber.i("Mementra Application started in RELEASE mode")
        }
    }
    
    /**
     * Применить сохраненную тему
     */
    private fun applyTheme() {
        val prefs = getSharedPreferences("mementra_settings", MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean("dark_mode", false)
        
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    /**
     * Кастомное дерево для production
     * Логирует только WARNING и ERROR
     */
    private class ReleaseTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            if (priority == android.util.Log.VERBOSE || priority == android.util.Log.DEBUG || priority == android.util.Log.INFO) {
                return
            }

            // Здесь можно добавить отправку в аналитику
            // FirebaseCrashlytics.getInstance().log(message)
            
            if (t != null) {
                // FirebaseCrashlytics.getInstance().recordException(t)
            }
        }
    }
}

