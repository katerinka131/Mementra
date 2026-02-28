package com.example.mementra.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mementra.MainActivity
import com.example.mementra.adapters.FavoritesAdapter
import com.example.mementra.databinding.FragmentDiaryBinding
import com.example.mementra.utils.MemoryDialogHelper
import com.example.mementra.viewmodels.DiaryUiState
import com.example.mementra.viewmodels.DiaryViewModel
import com.example.mementra.viewmodels.DiaryViewModelFactory
import com.example.mementra.viewmodels.SortType
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import timber.log.Timber

class DiaryFragment : Fragment() {

    private var _binding: FragmentDiaryBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: DiaryViewModel
    private lateinit var diaryAdapter: FavoritesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDiaryBinding.inflate(inflater, container, false)

        // Инициализация ViewModel
        val mainActivity = requireActivity() as MainActivity
        val repository = mainActivity.memoryRepo
        val userId = mainActivity.userId
        
        val factory = DiaryViewModelFactory(repository, userId)
        viewModel = ViewModelProvider(this, factory)[DiaryViewModel::class.java]

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSearchView()
        setupSortButton()
        observeViewModel()
    }

    /**
     * Настройка RecyclerView
     */
    private fun setupRecyclerView() {
        diaryAdapter = FavoritesAdapter(emptyList()) { memory ->
            showMemoryDetails(memory.pointId)
        }

        binding.diaryRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = diaryAdapter
        }
    }

    /**
     * Настройка поиска
     */
    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { viewModel.search(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let { viewModel.search(it) }
                return true
            }
        })
    }

    /**
     * Настройка кнопки сортировки
     */
    private fun setupSortButton() {
        binding.sampleButton.setOnClickListener {
            showSortDialog()
        }
    }

    /**
     * Показать диалог сортировки
     */
    private fun showSortDialog() {
        val sortOptions = arrayOf(
            "Сначала новые",
            "Сначала старые",
            "По алфавиту (A-Z)",
            "По алфавиту (Z-A)"
        )

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Сортировка")
            .setItems(sortOptions) { _, which ->
                val sortType = when (which) {
                    0 -> SortType.DATE_DESC
                    1 -> SortType.DATE_ASC
                    2 -> SortType.TITLE_ASC
                    3 -> SortType.TITLE_DESC
                    else -> SortType.DATE_DESC
                }
                viewModel.changeSorting(sortType)
            }
            .show()
    }

    /**
     * Подписка на изменения ViewModel
     */
    private fun observeViewModel() {
        // Наблюдаем за UI State
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is DiaryUiState.Idle -> {
                    // Ничего не делаем
                }
                is DiaryUiState.Loading -> {
                    binding.diaryPlaceholder.visibility = View.VISIBLE
                    binding.diaryRecyclerView.visibility = View.GONE
                    binding.diaryPlaceholderText.text = "⏳ Загрузка..."
                }
                is DiaryUiState.Empty -> {
                    binding.diaryPlaceholder.visibility = View.VISIBLE
                    binding.diaryRecyclerView.visibility = View.GONE
                    binding.diaryPlaceholderText.text = 
                        "Здесь пока нет записей\n\n" +
                        "Добавьте воспоминания на карте!"
                }
                is DiaryUiState.Success -> {
                    binding.diaryPlaceholder.visibility = View.GONE
                    binding.diaryRecyclerView.visibility = View.VISIBLE
                    diaryAdapter.updateMemories(state.memories)
                }
                is DiaryUiState.Error -> {
                    binding.diaryPlaceholder.visibility = View.VISIBLE
                    binding.diaryRecyclerView.visibility = View.GONE
                    binding.diaryPlaceholderText.text = 
                        "❌ Ошибка загрузки дневника:\n${state.message}"
                }

                else -> {}
            }
        }

        // Наблюдаем за отфильтрованными воспоминаниями
        viewModel.filteredMemories.observe(viewLifecycleOwner) { memories ->
            Timber.d("Filtered memories updated: ${memories.size} items")
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

        val mainActivity = requireActivity() as MainActivity
        val repository = mainActivity.memoryRepo
        
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
     * Показать сообщение
     */
    private fun showMessage(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadMemories()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
