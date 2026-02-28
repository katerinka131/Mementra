package com.example.mementra.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mementra.database.MemoryPointRepository
import com.example.mementra.database.models.MemoryPoint
import com.example.mementra.utils.SingleLiveEvent
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*

/**
 * UI State для DiaryFragment
 */
sealed class DiaryUiState {
    object Idle : DiaryUiState()
    object Loading : DiaryUiState()
    object Empty : DiaryUiState()
    data class Success(val memories: List<MemoryPoint>) : DiaryUiState()
    data class Error(val message: String) : DiaryUiState()
}

/**
 * Фильтр для дневника
 */
data class DiaryFilter(
    val searchQuery: String = "",
    val sortBy: SortType = SortType.DATE_DESC,
    val dateFrom: Long? = null,
    val dateTo: Long? = null
)

enum class SortType {
    DATE_DESC,      // Сначала новые
    DATE_ASC,       // Сначала старые
    TITLE_ASC,      // По алфавиту A-Z
    TITLE_DESC      // По алфавиту Z-A
}

/**
 * ViewModel для DiaryFragment
 */
class DiaryViewModel(
    private val repository: MemoryPointRepository,
    private val userId: String
) : ViewModel() {

    // UI State
    private val _uiState = MutableLiveData<DiaryUiState>(DiaryUiState.Idle)
    val uiState: LiveData<DiaryUiState> = _uiState

    // Все воспоминания
    private val _allMemories = MutableLiveData<List<MemoryPoint>>(emptyList())
    
    // Отфильтрованные воспоминания
    private val _filteredMemories = MutableLiveData<List<MemoryPoint>>(emptyList())
    val filteredMemories: LiveData<List<MemoryPoint>> = _filteredMemories

    // Текущий фильтр
    private val _currentFilter = MutableLiveData(DiaryFilter())
    val currentFilter: LiveData<DiaryFilter> = _currentFilter

    // Сообщения (одноразовые) - используем SingleLiveEvent для предотвращения повторной отправки
    private val _message = SingleLiveEvent<String>()
    val message: LiveData<String> = _message

    init {
        loadMemories()
    }

    /**
     * Загрузить все воспоминания
     */
    fun loadMemories() {
        viewModelScope.launch {
            try {
                Timber.d("Loading memories for diary, userId: $userId")
                _uiState.value = DiaryUiState.Loading
                
                val memories = repository.getMemoryPoints(userId)
                _allMemories.value = memories
                
                Timber.i("Loaded ${memories.size} memories for diary")
                
                // Применяем текущий фильтр
                applyFilter(_currentFilter.value ?: DiaryFilter())
                
            } catch (e: Exception) {
                Timber.e(e, "Error loading memories for diary")
                _uiState.value = DiaryUiState.Error("Ошибка загрузки: ${e.message}")
            }
        }
    }

    /**
     * Применить фильтр
     */
    fun applyFilter(filter: DiaryFilter) {
        viewModelScope.launch {
            try {
                Timber.d("Applying filter: $filter")
                _currentFilter.value = filter
                
                val allMemories = _allMemories.value ?: emptyList()
                var filtered = allMemories.toList()
                
                // Фильтр по поисковому запросу
                if (filter.searchQuery.isNotBlank()) {
                    filtered = filtered.filter { memory ->
                        memory.title.contains(filter.searchQuery, ignoreCase = true) ||
                        memory.description?.contains(filter.searchQuery, ignoreCase = true) == true
                    }
                }
                
                // Фильтр по дате (от)
                filter.dateFrom?.let { dateFrom ->
                    filtered = filtered.filter { it.visitDate >= dateFrom }
                }
                
                // Фильтр по дате (до)
                filter.dateTo?.let { dateTo ->
                    filtered = filtered.filter { it.visitDate <= dateTo }
                }
                
                // Сортировка
                filtered = when (filter.sortBy) {
                    SortType.DATE_DESC -> filtered.sortedByDescending { it.visitDate }
                    SortType.DATE_ASC -> filtered.sortedBy { it.visitDate }
                    SortType.TITLE_ASC -> filtered.sortedBy { it.title.lowercase() }
                    SortType.TITLE_DESC -> filtered.sortedByDescending { it.title.lowercase() }
                }
                
                _filteredMemories.value = filtered
                
                if (filtered.isEmpty()) {
                    _uiState.value = DiaryUiState.Empty
                } else {
                    _uiState.value = DiaryUiState.Success(filtered)
                }
                
                Timber.i("Filter applied, ${filtered.size} memories match")
                
            } catch (e: Exception) {
                Timber.e(e, "Error applying filter")
                _message.value = "Ошибка фильтрации: ${e.message}"
            }
        }
    }

    /**
     * Поиск по тексту
     */
    fun search(query: String) {
        val newFilter = _currentFilter.value?.copy(searchQuery = query) ?: DiaryFilter(searchQuery = query)
        applyFilter(newFilter)
    }

    /**
     * Изменить сортировку
     */
    fun changeSorting(sortType: SortType) {
        val newFilter = _currentFilter.value?.copy(sortBy = sortType) ?: DiaryFilter(sortBy = sortType)
        applyFilter(newFilter)
    }

    /**
     * Установить фильтр по дате
     */
    fun setDateFilter(dateFrom: Long?, dateTo: Long?) {
        val newFilter = _currentFilter.value?.copy(
            dateFrom = dateFrom,
            dateTo = dateTo
        ) ?: DiaryFilter(dateFrom = dateFrom, dateTo = dateTo)
        applyFilter(newFilter)
    }

    /**
     * Сбросить все фильтры
     */
    fun resetFilters() {
        applyFilter(DiaryFilter())
        _message.value = "Фильтры сброшены"
    }

    /**
     * Удалить воспоминание
     */
    fun deleteMemory(pointId: Long, title: String) {
        viewModelScope.launch {
            try {
                Timber.d("Deleting memory: $pointId")
                val success = repository.deleteMemoryPoint(pointId)
                
                if (success) {
                    Timber.i("Memory deleted successfully: $pointId")
                    loadMemories() // Перезагружаем список
                    _message.value = "Воспоминание \"$title\" удалено"
                } else {
                    Timber.w("Failed to delete memory: $pointId")
                    _message.value = "Ошибка при удалении"
                }
            } catch (e: Exception) {
                Timber.e(e, "Error deleting memory")
                _message.value = "Ошибка: ${e.message}"
            }
        }
    }

    /**
     * Переключить избранное
     */
    fun toggleFavorite(pointId: Long, currentState: Boolean) {
        viewModelScope.launch {
            try {
                val newState = !currentState
                val success = repository.toggleFavorite(pointId, newState)
                
                if (success) {
                    loadMemories()
                    val msg = if (newState) "Добавлено в избранное" else "Убрано из избранного"
                    _message.value = msg
                } else {
                    _message.value = "Ошибка при обновлении избранного"
                }
            } catch (e: Exception) {
                Timber.e(e, "Error toggling favorite")
                _message.value = "Ошибка: ${e.message}"
            }
        }
    }

    fun updateMemory(memoryPoint: MemoryPoint) {
        if (memoryPoint.title.isBlank()) {
            _message.value = "Введите название воспоминания"
            return
        }
        viewModelScope.launch {
            try {
                val success = repository.updateMemoryPoint(memoryPoint)
                if (success) {
                    loadMemories()
                    _message.value = "Воспоминание обновлено"
                } else {
                    _message.value = "Ошибка при обновлении"
                }
            } catch (e: Exception) {
                Timber.e(e, "Error updating memory")
                _message.value = "Ошибка: ${e.message}"
            }
        }
    }

    /**
     * Получить воспоминание по ID
     */
    fun getMemoryById(pointId: Long): MemoryPoint? {
        return _filteredMemories.value?.find { it.pointId == pointId }
    }

    /**
     * Получить статистику
     */
    fun getStatistics(): DiaryStatistics {
        val memories = _allMemories.value ?: emptyList()
        val favorites = memories.count { it.isFavorite }
        
        return DiaryStatistics(
            totalMemories = memories.size,
            favoriteMemories = favorites,
            oldestMemory = memories.minByOrNull { it.visitDate }?.visitDate,
            newestMemory = memories.maxByOrNull { it.visitDate }?.visitDate
        )
    }
}

/**
 * Статистика дневника
 */
data class DiaryStatistics(
    val totalMemories: Int,
    val favoriteMemories: Int,
    val oldestMemory: Long?,
    val newestMemory: Long?
)

