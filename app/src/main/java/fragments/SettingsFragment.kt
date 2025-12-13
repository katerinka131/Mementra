package com.example.mementra.fragments

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.mementra.R
import com.example.mementra.databinding.FragmentSettingsBinding
import com.example.mementra.utils.NotificationHelper

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var prefs: SharedPreferences
    
    // Launcher для запроса разрешения на уведомления
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(requireContext(), "Разрешение на уведомления получено", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Разрешение на уведомления отклонено", Toast.LENGTH_SHORT).show()
            binding.notificationsSwitch.isChecked = false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        
        prefs = requireContext().getSharedPreferences("mementra_settings", android.content.Context.MODE_PRIVATE)
        
        // Создать канал уведомлений
        NotificationHelper.createNotificationChannel(requireContext())
        
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupThemeSwitch()
        setupNotifications()
        setupNotificationTime()
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
        
        // Тема применяется автоматически, перезапуск не требуется
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
            if (isChecked) {
                // Проверяем разрешение на уведомления (Android 13+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        // Запрашиваем разрешение
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        return@setOnCheckedChangeListener
                    }
                }
                
                // Включаем уведомления
                prefs.edit().putBoolean("notifications_enabled", true).apply()
                
                // Запланировать уведомления с сохраненным временем
                val hour = prefs.getInt("notification_hour", 20)
                val minute = prefs.getInt("notification_minute", 0)
                NotificationHelper.scheduleDailyNotification(requireContext(), hour, minute)
                
                Toast.makeText(requireContext(), "Уведомления включены", Toast.LENGTH_SHORT).show()
                
                // Показать блок выбора времени
                binding.notificationTimeCard.visibility = View.VISIBLE
            } else {
                // Отключаем уведомления
                prefs.edit().putBoolean("notifications_enabled", false).apply()
                NotificationHelper.cancelDailyNotification(requireContext())
                
                Toast.makeText(requireContext(), "Уведомления отключены", Toast.LENGTH_SHORT).show()
                
                // Скрыть блок выбора времени
                binding.notificationTimeCard.visibility = View.GONE
            }
        }
        
        // Клик по всему блоку
        binding.notificationsLayout.setOnClickListener {
            binding.notificationsSwitch.isChecked = !binding.notificationsSwitch.isChecked
        }
        
        // Показать/скрыть блок выбора времени в зависимости от состояния
        binding.notificationTimeCard.visibility = if (notificationsEnabled) View.VISIBLE else View.GONE
    }
    
    /**
     * Настройка времени уведомлений
     */
    private fun setupNotificationTime() {
        // Загружаем сохраненное время
        val hour = prefs.getInt("notification_hour", 20)
        val minute = prefs.getInt("notification_minute", 0)
        updateNotificationTimeText(hour, minute)
        
        // Обработчик клика
        binding.notificationTimeLayout.setOnClickListener {
            showTimePickerDialog(hour, minute)
        }
    }
    
    /**
     * Показать диалог выбора времени
     */
    private fun showTimePickerDialog(currentHour: Int, currentMinute: Int) {
        TimePickerDialog(
            requireContext(),
            { _, selectedHour, selectedMinute ->
                // Сохраняем выбранное время
                prefs.edit()
                    .putInt("notification_hour", selectedHour)
                    .putInt("notification_minute", selectedMinute)
                    .apply()
                
                // Обновляем текст
                updateNotificationTimeText(selectedHour, selectedMinute)
                
                // Если уведомления включены, перепланируем их
                if (binding.notificationsSwitch.isChecked) {
                    NotificationHelper.scheduleDailyNotification(requireContext(), selectedHour, selectedMinute)
                    Toast.makeText(
                        requireContext(),
                        "Время уведомлений обновлено",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            currentHour,
            currentMinute,
            true // 24-часовой формат
        ).show()
    }
    
    /**
     * Обновить текст времени уведомлений
     */
    private fun updateNotificationTimeText(hour: Int, minute: Int) {
        binding.notificationTimeText.text = String.format("%02d:%02d", hour, minute)
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
