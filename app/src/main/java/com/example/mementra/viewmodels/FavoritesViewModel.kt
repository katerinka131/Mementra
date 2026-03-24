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

/**
 * UI State для FavoritesFragment
 */
sealed class FavoritesUiState {
    object Idle : FavoritesUiState()
    object Loading : FavoritesUiState()
    object Empty : FavoritesUiState()
    data class Success(val favorites: List<MemoryPoint>) : FavoritesUiState()
    data class Error(val message: String) : FavoritesUiState()
}

/**
 * ViewModel для FavoritesFragment
 */
class FavoritesViewModel(
    private val repository: MemoryPointRepository,
    private val userId: String
) : ViewModel() {

    // UI State
    private val _uiState = MutableLiveData<FavoritesUiState>(FavoritesUiState.Idle)
    val uiState: LiveData<FavoritesUiState> = _uiState

    // Список избранных воспоминаний
    private val _favoriteMemories = MutableLiveData<List<MemoryPoint>>(emptyList())
    val favoriteMemories: LiveData<List<MemoryPoint>> = _favoriteMemories

    // Сообщения (одноразовые) - используем SingleLiveEvent для предотвращения повторной отправки
    private val _message = SingleLiveEvent<String>()
    val message: LiveData<String> = _message

    init {
        loadFavorites()
    }

    /**
     * Загрузить избранные воспоминания
     */
    fun loadFavorites() {
        viewModelScope.launch {
            try {
                _uiState.value = FavoritesUiState.Loading

                val favorites = repository.getFavoriteMemoryPoints(userId)
                _favoriteMemories.value = favorites

                if (favorites.isEmpty()) {
                    _uiState.value = FavoritesUiState.Empty
                } else {
                    _uiState.value = FavoritesUiState.Success(favorites)
                }
            } catch (e: Exception) {
                _uiState.value = FavoritesUiState.Error("Ошибка загрузки: ${e.message}")
            }
        }
    }

    /**
     * Переключить избранное
     */
    fun toggleFavorite(pointId: Long, isFavorite: Boolean) {
        viewModelScope.launch {
            try {
                val success = repository.toggleFavorite(pointId, isFavorite)

                if (success) {
                    loadFavorites()
                } else {
                    _message.value = "Ошибка при обновлении избранного"
                }
            } catch (e: Exception) {
                _message.value = "Ошибка: ${e.message}"
            }
        }
    }

    /**
     * Удалить воспоминание
     */
    fun deleteMemory(pointId: Long, title: String) {
        viewModelScope.launch {
            try {
                val success = repository.deleteMemoryPoint(pointId)

                if (success) {
                    loadFavorites()
                } else {
                    _message.value = "Ошибка при удалении"
                }
            } catch (e: Exception) {
                _message.value = "Ошибка: ${e.message}"
            }
        }
    }

    /**
     * Обновить воспоминание
     */
    fun updateMemory(memoryPoint: MemoryPoint) {
        if (memoryPoint.title.isBlank()) {
            _message.value = "Введите название воспоминания"
            return
        }
        viewModelScope.launch {
            try {
                val success = repository.updateMemoryPoint(memoryPoint)
                if (success) {
                    loadFavorites()
                } else {
                    _message.value = "Ошибка при обновлении"
                }
            } catch (e: Exception) {
                _message.value = "Ошибка: ${e.message}"
            }
        }
    }

    /**
     * Получить воспоминание по ID
     */
    fun getMemoryById(pointId: Long): MemoryPoint? {
        return _favoriteMemories.value?.find { it.pointId == pointId }
    }

    /**
     * Удалить медиа запись
     */
    fun deleteMediaEntry(entryId: Long) {
        viewModelScope.launch {
            try {
                Timber.d("Deleting media entry with ID: $entryId")
                val success = repository.deleteMemoryEntry(entryId)
                if (success) {
                    Timber.d("Media entry deleted successfully: $entryId")
                } else {
                    Timber.e("Failed to delete media entry: $entryId")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error deleting media entry")
            }
        }
    }
}