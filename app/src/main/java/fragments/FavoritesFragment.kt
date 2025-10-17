package com.example.mementra.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mementra.adapters.FavoritesAdapter
import com.example.mementra.database.MemoryPointRepository
import com.example.mementra.database.UserManager
import com.example.mementra.databinding.FragmentFavoritesBinding

class FavoritesFragment : Fragment() {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!
    private lateinit var memoryPointRepository: MemoryPointRepository
    private lateinit var userManager: UserManager
    private var currentUserId: String = ""
    private lateinit var favoritesAdapter: FavoritesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)

        userManager = UserManager(requireContext())
        memoryPointRepository = MemoryPointRepository(com.example.mementra.database.AppDatabaseHelper(requireContext()))
        currentUserId = userManager.getUserId()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        loadFavorites()
    }

    private fun setupRecyclerView() {
        favoritesAdapter = FavoritesAdapter(emptyList()) { memory ->
            // Обработчик клика по воспоминанию - открываем детали
            showMemoryDetails(memory.pointId)
        }

        binding.favoritesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = favoritesAdapter
        }
    }

    private fun loadFavorites() {
        try {
            val favoritePoints = memoryPointRepository.getFavoriteMemoryPoints(currentUserId)

            println("FAVORITES_DEBUG: Found ${favoritePoints.size} favorites")

            if (favoritePoints.isEmpty()) {
                binding.favoritesPlaceholder.visibility = View.VISIBLE
                binding.favoritesRecyclerView.visibility = View.GONE
                binding.favoritesPlaceholderText.text = "❤️ Избранные воспоминания\n\nЗдесь будут ваши самые важные моменты\n\nПока нет избранных воспоминаний\n\nДобавьте воспоминания в избранное на карте!"
            } else {
                binding.favoritesPlaceholder.visibility = View.GONE
                binding.favoritesRecyclerView.visibility = View.VISIBLE
                favoritesAdapter.updateMemories(favoritePoints)

                android.widget.Toast.makeText(
                    requireContext(),
                    "Загружено ${favoritePoints.size} избранных воспоминаний",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            binding.favoritesPlaceholderText.text = "Ошибка загрузки избранных воспоминаний: ${e.message}"
            e.printStackTrace()
        }
    }

    private fun showMemoryDetails(memoryPointId: Long) {
        // Используем тот же метод что и в MapFragment
        // Нужно либо скопировать метод, либо вынести в общий класс

        val memoryPoints = memoryPointRepository.getMemoryPoints(currentUserId)
        val memoryPoint = memoryPoints.find { it.pointId == memoryPointId }

        memoryPoint?.let { point ->
            // Создаем диалог для просмотра воспоминания
            // Можно скопировать код из MapFragment.showMemoryDetails()
            // Или вынести в отдельную функцию/класс

            showMemoryDetailsDialog(point)
        }
    }

    private fun showMemoryDetailsDialog(memoryPoint: com.example.mementra.database.MemoryPoint) {
        val entries = memoryPointRepository.getMemoryEntries(memoryPoint.pointId)

        val dialogView = android.view.LayoutInflater.from(requireContext()).inflate(com.example.mementra.R.layout.bottom_sheet_memory_details, null)
        val dialog = android.app.Dialog(requireContext())
        dialog.setContentView(dialogView)

        val window = dialog.window
        window?.setGravity(android.view.Gravity.BOTTOM)
        window?.setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, (resources.displayMetrics.heightPixels * 0.8).toInt())
        window?.setWindowAnimations(com.example.mementra.R.style.DialogAnimation)

        // Находим все View с полными путями
        val tvTitle = dialogView.findViewById<android.widget.TextView>(com.example.mementra.R.id.tvTitle)
        val tvDescription = dialogView.findViewById<android.widget.TextView>(com.example.mementra.R.id.tvDescription)
        val tvDate = dialogView.findViewById<android.widget.TextView>(com.example.mementra.R.id.tvDate)
        val tvLocation = dialogView.findViewById<android.widget.TextView>(com.example.mementra.R.id.tvLocation)
        val tvEntries = dialogView.findViewById<android.widget.TextView>(com.example.mementra.R.id.tvEntries)
        val btnFavorite = dialogView.findViewById<android.widget.ImageButton>(com.example.mementra.R.id.btnFavorite)
        val btnEdit = dialogView.findViewById<android.widget.Button>(com.example.mementra.R.id.btnEdit)
        val btnDelete = dialogView.findViewById<android.widget.Button>(com.example.mementra.R.id.btnDelete)
        val btnClose = dialogView.findViewById<android.widget.Button>(com.example.mementra.R.id.btnClose)

        // Устанавливаем иконку избранного
        if (memoryPoint.isFavorite) {
            btnFavorite.setImageResource(com.example.mementra.R.drawable.ic_favorite_filled)
        } else {
            btnFavorite.setImageResource(com.example.mementra.R.drawable.ic_favorite_border)
        }

        // Обработчик для избранного
        btnFavorite.setOnClickListener {
            val newFavoriteState = !memoryPoint.isFavorite
            val success = memoryPointRepository.toggleFavorite(memoryPoint.pointId, newFavoriteState)

            if (success) {
                val updatedPoint = memoryPoint.copy(isFavorite = newFavoriteState)

                if (newFavoriteState) {
                    btnFavorite.setImageResource(com.example.mementra.R.drawable.ic_favorite_filled)
                    android.widget.Toast.makeText(requireContext(), "Добавлено в избранное", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    btnFavorite.setImageResource(com.example.mementra.R.drawable.ic_favorite_border)
                    android.widget.Toast.makeText(requireContext(), "Убрано из избранного", android.widget.Toast.LENGTH_SHORT).show()
                    // Обновляем список после удаления из избранного
                    loadFavorites()
                }
            }
        }

        // Устанавливаем текст
        tvTitle.text = memoryPoint.title
        tvDescription.text = memoryPoint.description ?: "Нет описания"
        tvDate.text = "Дата: ${java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(memoryPoint.visitDate))}"
        tvLocation.text = "Координаты: ${"%.6f".format(memoryPoint.latitude)}, ${"%.6f".format(memoryPoint.longitude)}"

        if (entries.isNotEmpty()) {
            val entriesText = entries.joinToString("\n\n") { entry ->
                when (entry.type) {
                    "text" -> "📝 ${entry.content}"
                    "photo" -> "📷 Фото: ${entry.content}"
                    "voice" -> "🎤 Голосовая запись: ${entry.duration} сек"
                    else -> "❓ Неизвестный тип"
                }
            }
            tvEntries.text = entriesText
        } else {
            tvEntries.text = "Нет дополнительных записей"
        }

        btnEdit.setOnClickListener {
            dialog.dismiss()
            android.widget.Toast.makeText(requireContext(), "Редактирование будет добавлено в обновлении", android.widget.Toast.LENGTH_SHORT).show()
        }

        btnDelete.setOnClickListener {
            dialog.dismiss()
            showDeleteConfirmationDialog(memoryPoint)
        }

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.setCancelable(true)
        dialog.show()
    }

    private fun showDeleteConfirmationDialog(memoryPoint: com.example.mementra.database.MemoryPoint) {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Удаление воспоминания")
            .setMessage("Вы уверены, что хотите удалить воспоминание \"${memoryPoint.title}\"?")
            .setPositiveButton("Удалить") { _, _ ->
                val success = memoryPointRepository.deleteMemoryPoint(memoryPoint.pointId)
                if (success) {
                    android.widget.Toast.makeText(requireContext(), "Воспоминание удалено", android.widget.Toast.LENGTH_SHORT).show()
                    loadFavorites() // Обновляем список
                } else {
                    android.widget.Toast.makeText(requireContext(), "Ошибка при удалении", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        loadFavorites()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}