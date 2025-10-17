package com.example.mementra.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.mementra.databinding.FragmentDiaryBinding

class DiaryFragment : Fragment() {

    private var _binding: FragmentDiaryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDiaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDiary()
    }

    private fun setupDiary() {
        binding.diaryPlaceholderText.text = "📓 Дневник воспоминаний\n\nВсе ваши записи в хронологическом порядке\n\n• Фильтры по дате и тегам\n• Поиск по тексту\n• Сортировка"

        binding.sampleButton.setOnClickListener {
            showMessage("Открыть детали воспоминания")
        }
    }

    private fun showMessage(message: String) {
        android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}