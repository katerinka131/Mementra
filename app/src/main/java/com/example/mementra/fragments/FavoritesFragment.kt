package com.example.mementra.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mementra.MainActivity
import com.example.mementra.adapters.FavoritesAdapter
import com.example.mementra.databinding.FragmentFavoritesBinding
import com.example.mementra.utils.MemoryDialogHelper
import com.example.mementra.viewmodels.FavoritesUiState
import com.example.mementra.viewmodels.FavoritesViewModel
import com.example.mementra.viewmodels.FavoritesViewModelFactory
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

class FavoritesFragment : Fragment() {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: FavoritesViewModel
    private lateinit var favoritesAdapter: FavoritesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)

        // Инициализация ViewModel
        val mainActivity = requireActivity() as MainActivity
        val repository = mainActivity.memoryRepo
        val userId = mainActivity.userId
        
        val factory = FavoritesViewModelFactory(repository, userId)
        viewModel = ViewModelProvider(this, factory)[FavoritesViewModel::class.java]

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
    }

    /**
     * Настройка RecyclerView
     */
    private fun setupRecyclerView() {
        favoritesAdapter = FavoritesAdapter(emptyList()) { memory ->
            showMemoryDetails(memory.pointId)
        }

        binding.favoritesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = favoritesAdapter
        }
    }

    /**
     * Подписка на изменения ViewModel
     */
    private fun observeViewModel() {
        // Наблюдаем за UI State
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is FavoritesUiState.Idle -> {
                    // Ничего не делаем
                }
                is FavoritesUiState.Loading -> {
                    // Можно показать прогресс
                    binding.favoritesPlaceholder.visibility = View.GONE
                    binding.favoritesRecyclerView.visibility = View.GONE
                }
                is FavoritesUiState.Empty -> {
                binding.favoritesPlaceholder.visibility = View.VISIBLE
                binding.favoritesRecyclerView.visibility = View.GONE
                    binding.favoritesPlaceholderText.text = 
                        "Здесь будут ваши самые важные моменты\n\n" +
                        "Пока нет избранных воспоминаний\n\n" +
                        "Добавьте воспоминания в избранное на карте!"
                }
                is FavoritesUiState.Success -> {
                binding.favoritesPlaceholder.visibility = View.GONE
                binding.favoritesRecyclerView.visibility = View.VISIBLE
                    favoritesAdapter.updateMemories(state.favorites)
                }
                is FavoritesUiState.Error -> {
                    binding.favoritesPlaceholder.visibility = View.VISIBLE
                    binding.favoritesRecyclerView.visibility = View.GONE
                    binding.favoritesPlaceholderText.text = 
                        "Ошибка загрузки избранных воспоминаний:\n${state.message}"
                }
            }
        }

        // Наблюдаем за сообщениями
        viewModel.message.observe(viewLifecycleOwner) { message ->
            message?.let {
                showMessage(it)
            }
        }
    }

    /**
     * Показать детали воспоминания
     */
    private fun showMemoryDetails(pointId: Long) {
        val memoryPoint = viewModel.getMemoryById(pointId) ?: return

        // Получаем записи через MainActivity (можно улучшить)
        val mainActivity = requireActivity() as MainActivity
        val repository = mainActivity.memoryRepo
        
        // Используем lifecycleScope для получения записей
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val entries = repository.getMemoryEntries(pointId)
                
                MemoryDialogHelper.showMemoryDetailsDialog(
                    context = requireContext(),
                    memoryPoint = memoryPoint,
                    entries = entries,
                    onFavoriteToggle = { newState ->
                        viewModel.toggleFavorite(pointId, !newState)
                    },
                    onEdit = {
                        MemoryDialogHelper.showEditMemoryDialog(
                            context = requireContext(),
                            memoryPoint = memoryPoint,
                            onSave = { title, description, emoji ->
                                val updatedPoint = memoryPoint.copy(
                                    title = title,
                                    description = description,
                                    emoji = emoji,
                                    visitDate = System.currentTimeMillis()
                                )
                                viewModel.updateMemory(updatedPoint)
                            },
                            onCancel = {}
                        )
                    },
                    onDelete = {
                        MemoryDialogHelper.showDeleteConfirmationDialog(
                            context = requireContext(),
                            memoryTitle = memoryPoint.title,
                            onConfirm = {
                                viewModel.deleteMemory(pointId, memoryPoint.title)
                            }
                        )
                    }
                )
            } catch (e: Exception) {
                showMessage("Ошибка загрузки записей: ${e.message}")
            }
        }
    }

    /**
     * Показать сообщение (только для критических ошибок)
     */
    private fun showMessage(message: String) {
        // Показываем только критические ошибки, убираем лишние уведомления
        if (message.contains("Ошибка", ignoreCase = true)) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadFavorites()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
