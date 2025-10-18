package com.example.mementra.fragments

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.example.mementra.R
import com.example.mementra.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var prefs: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        
        prefs = requireContext().getSharedPreferences("mementra_settings", android.content.Context.MODE_PRIVATE)
        
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupThemeSwitch()
        setupNotifications()
        setupSupport()
        setupShare()
        setupAbout()
    }

    /**
     * Настройка переключателя темы
     */
    private fun setupThemeSwitch() {
        // Загружаем текущую тему
        val isDarkMode = prefs.getBoolean("dark_mode", false)
        binding.themeSwitch.isChecked = isDarkMode
        updateThemeDescription(isDarkMode)
        
        // Обработчик переключения темы
        binding.themeSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("dark_mode", isChecked).apply()
            updateThemeDescription(isChecked)
            applyTheme(isChecked)
        }
        
        // Клик по всему блоку
        binding.themeSettingLayout.setOnClickListener {
            binding.themeSwitch.isChecked = !binding.themeSwitch.isChecked
        }
    }

    /**
     * Обновить описание темы
     */
    private fun updateThemeDescription(isDarkMode: Boolean) {
        binding.themeDescription.text = if (isDarkMode) "Темная" else "Светлая"
    }

    /**
     * Применить тему
     */
    private fun applyTheme(isDarkMode: Boolean) {
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
        
        Toast.makeText(
            requireContext(),
            "Тема изменена. Перезапустите приложение для полного применения.",
            Toast.LENGTH_LONG
        ).show()
    }

    /**
     * Настройка уведомлений
     */
    private fun setupNotifications() {
        // Загружаем настройку
        val notificationsEnabled = prefs.getBoolean("notifications_enabled", true)
        binding.notificationsSwitch.isChecked = notificationsEnabled
        
        // Обработчик
        binding.notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("notifications_enabled", isChecked).apply()
            
            val message = if (isChecked) {
                "Уведомления включены"
            } else {
                "Уведомления отключены"
            }
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
        
        // Клик по всему блоку
        binding.notificationsLayout.setOnClickListener {
            binding.notificationsSwitch.isChecked = !binding.notificationsSwitch.isChecked
        }
    }

    /**
     * Настройка связи с поддержкой
     */
    private fun setupSupport() {
        binding.supportLayout.setOnClickListener {
            // Открываем email клиент
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf("support@mementra.app"))
                putExtra(Intent.EXTRA_SUBJECT, "Поддержка Mementra")
                putExtra(Intent.EXTRA_TEXT, "Здравствуйте! У меня есть вопрос по приложению Mementra:\n\n")
            }
            
            try {
                startActivity(Intent.createChooser(intent, "Выберите email клиент"))
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Email: support@mementra.app\n\nСкопируйте адрес для связи",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * Настройка поделиться приложением
     */
    private fun setupShare() {
        binding.shareLayout.setOnClickListener {
            val shareText = """
                🌟 Попробуйте Mementra - приложение для сохранения воспоминаний!
                
                📍 Привязывайте воспоминания к местам на карте
                📸 Добавляйте фото и заметки
                ❤️ Сохраняйте важные моменты
                
                Скачать: https://play.google.com/store/apps/details?id=com.example.mementra
            """.trimIndent()
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
                putExtra(Intent.EXTRA_SUBJECT, "Попробуйте Mementra!")
            }
            
            startActivity(Intent.createChooser(intent, "Поделиться приложением"))
        }
    }

    /**
     * Настройка "О приложении"
     */
    private fun setupAbout() {
        binding.aboutLayout.setOnClickListener {
            val aboutText = """
                📱 Mementra
                Версия: 1.0
                
                Приложение для сохранения воспоминаний с привязкой к местам на карте.
                
                Разработчик: Mementra Team
                Email: support@mementra.app
                
                © 2025 Все права защищены
            """.trimIndent()
            
            android.app.AlertDialog.Builder(requireContext())
                .setTitle("О приложении")
                .setMessage(aboutText)
                .setPositiveButton("OK", null)
                .setNeutralButton("Сайт") { _, _ ->
                    // Открываем сайт (заглушка)
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://mementra.app"))
                    try {
                        startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Сайт: https://mementra.app", Toast.LENGTH_LONG).show()
                    }
                }
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
