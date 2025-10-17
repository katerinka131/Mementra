package com.example.mementra.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.mementra.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSettings()
    }

    private fun setupSettings() {
        binding.settingsPlaceholderText.text = "⚙️ Настройки Mementra\n\n• Профиль пользователя\n• Управление тегами\n• Экспорт данных\n• Резервное копирование\n• О приложении"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}