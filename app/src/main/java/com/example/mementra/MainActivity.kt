package com.example.mementra

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.mementra.database.AppDatabaseHelper
import com.example.mementra.database.MemoryPointRepository
import com.example.mementra.database.UserManager
import com.example.mementra.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var userManager: UserManager
    private lateinit var memoryRepository: MemoryPointRepository

    val userId: String by lazy { userManager.getUserId() }
    val memoryRepo: MemoryPointRepository get() = memoryRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Проверяем, нужно ли показать onboarding
        if (shouldShowOnboarding()) {
            startOnboarding()
            return
        }

        // Инициализация зависимостей
        userManager = UserManager(this)
        memoryRepository = MemoryPointRepository(AppDatabaseHelper(this))

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()

        // Обновляем время последней активности
        userManager.updateLastActive(userId)
    }

    /**
     * Проверяет, нужно ли показать экран приветствия
     */
    private fun shouldShowOnboarding(): Boolean {
        val prefs = getSharedPreferences("mementra_prefs", MODE_PRIVATE)
        return !prefs.getBoolean("onboarding_completed", false)
    }

    /**
     * Запускает экран приветствия
     */
    private fun startOnboarding() {
        val intent = Intent(this, OnboardingActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun setupNavigation() {
        // Получаем NavHostFragment
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        // Получаем NavController
        val navController = navHostFragment.navController

        // Настраиваем нижнюю навигацию
        binding.bottomNavigation.setupWithNavController(navController)
    }
}