package com.example.mementra.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mementra.database.MemoryPointRepository
import com.example.mementra.database.models.MemoryEntry
import com.example.mementra.database.models.MemoryPoint
import com.example.mementra.utils.SingleLiveEvent
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import timber.log.Timber

/**
 * UI State для MapFragment
 */
sealed class MapUiState {
    object Idle : MapUiState()
    object Loading : MapUiState()
    data class Success(val message: String) : MapUiState()
    data class Error(val message: String) : MapUiState()
}

/**
 * Событие для одноразовых действий
 */
sealed class MapEvent {
    data class ShowMessage(val message: String) : MapEvent()
    data class MemoryAdded(val pointId: Long, val point: GeoPoint, val title: String) : MapEvent()
    data class MemoryUpdated(val memoryPoint: MemoryPoint) : MapEvent()
    data class MemoryDeleted(val pointId: Long) : MapEvent()
    data class FavoriteToggled(val pointId: Long, val isFavorite: Boolean) : MapEvent()
}

/**
 * ViewModel для MapFragment
 */
class MapViewModel(
    private val repository: MemoryPointRepository,
    private val userId: String
) : ViewModel() {

    // UI State
    private val _uiState = MutableLiveData<MapUiState>(MapUiState.Idle)
    val uiState: LiveData<MapUiState> = _uiState

    // Список всех воспоминаний
    private val _memoryPoints = MutableLiveData<List<MemoryPoint>>(emptyList())
    val memoryPoints: LiveData<List<MemoryPoint>> = _memoryPoints

    // События (одноразовые) - используем SingleLiveEvent для предотвращения повторной отправки
    private val _events = SingleLiveEvent<MapEvent>()
    val events: LiveData<MapEvent> = _events

    // Режим выбора точки
    private val _isSelectingPoint = MutableLiveData(false)
    val isSelectingPoint: LiveData<Boolean> = _isSelectingPoint

    init {
        loadMemoryPoints()
    }

    /**
     * Загрузить все воспоминания пользователя
     */
    fun loadMemoryPoints() {
        viewModelScope.launch {
            try {
                Timber.d("Loading memory points for user: $userId")
                _uiState.value = MapUiState.Loading
                val points = repository.getMemoryPoints(userId)
                Timber.i("Loaded ${points.size} memory points")
                _memoryPoints.value = points
                _uiState.value = MapUiState.Idle
            } catch (e: Exception) {
                Timber.e(e, "Error loading memory points")
                _uiState.value = MapUiState.Error("Ошибка загрузки воспоминаний: ${e.message}")
            }
        }
    }

    /**
     * Добавить новое воспоминание
     */
    fun addMemoryPoint(
        title: String,
        description: String,
        latitude: Double,
        longitude: Double,
        emoji: String = MemoryPoint.DEFAULT_EMOJI
    ) {
        if (title.isBlank()) {
            Timber.w("Attempted to add memory point with blank title")
            _events.value = MapEvent.ShowMessage("Введите название воспоминания")
            return
        }

        viewModelScope.launch {
            try {
                Timber.d("Adding memory point: title=$title, lat=$latitude, lon=$longitude")
                _uiState.value = MapUiState.Loading

                val memoryPoint = MemoryPoint(
                    userId = userId,
                    title = title,
                    description = description,
                    latitude = latitude,
                    longitude = longitude,
                    visitDate = System.currentTimeMillis(),
                    emoji = emoji
                )

                val pointId = repository.addMemoryPoint(memoryPoint)

                if (pointId != -1L) {
                    Timber.i("Memory point added successfully with ID: $pointId")
                    
                    // Добавляем текстовую запись если есть описание
                    if (description.isNotBlank()) {
                        val textEntry = MemoryEntry(
                            memoryPointId = pointId,
                            type = "text",
                            content = description
                        )
                        repository.addMemoryEntry(textEntry)
                        Timber.d("Text entry added for memory point $pointId")
                    }

                    // Перезагружаем список
                    loadMemoryPoints()

                    // Отправляем событие
                    _events.value = MapEvent.MemoryAdded(
                        pointId = pointId,
                        point = GeoPoint(latitude, longitude),
                        title = title
                    )
                    _events.value = MapEvent.ShowMessage("Воспоминание сохранено!")
                    _uiState.value = MapUiState.Success("Воспоминание добавлено")
                } else {
                    Timber.e("Failed to add memory point, returned ID: $pointId")
                    _uiState.value = MapUiState.Error("Ошибка сохранения")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error adding memory point")
                _uiState.value = MapUiState.Error("Ошибка: ${e.message}")
            }
        }
    }

    /**
     * Обновить воспоминание
     */
    fun updateMemoryPoint(memoryPoint: MemoryPoint) {
        if (memoryPoint.title.isBlank()) {
            _events.value = MapEvent.ShowMessage("Введите название воспоминания")
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = MapUiState.Loading

                val success = repository.updateMemoryPoint(memoryPoint)

                if (success) {
                    loadMemoryPoints()
                    _events.value = MapEvent.MemoryUpdated(memoryPoint)
                    _events.value = MapEvent.ShowMessage("Воспоминание обновлено!")
                    _uiState.value = MapUiState.Success("Воспоминание обновлено")
                } else {
                    _uiState.value = MapUiState.Error("Ошибка при обновлении")
                }
            } catch (e: Exception) {
                _uiState.value = MapUiState.Error("Ошибка: ${e.message}")
            }
        }
    }

    /**
     * Удалить воспоминание
     */
    fun deleteMemoryPoint(pointId: Long, title: String) {
        viewModelScope.launch {
            try {
                _uiState.value = MapUiState.Loading

                val success = repository.deleteMemoryPoint(pointId)

                if (success) {
                    loadMemoryPoints()
                    _events.value = MapEvent.MemoryDeleted(pointId)
                    _events.value = MapEvent.ShowMessage("Воспоминание \"$title\" удалено")
                    _uiState.value = MapUiState.Success("Воспоминание удалено")
                } else {
                    _uiState.value = MapUiState.Error("Ошибка при удалении")
                }
            } catch (e: Exception) {
                _uiState.value = MapUiState.Error("Ошибка: ${e.message}")
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
                    loadMemoryPoints()
                    _events.value = MapEvent.FavoriteToggled(pointId, newState)
                    val message = if (newState) "Добавлено в избранное" else "Убрано из избранного"
                    _events.value = MapEvent.ShowMessage(message)
                } else {
                    _events.value = MapEvent.ShowMessage("Ошибка при обновлении избранного")
                }
            } catch (e: Exception) {
                _events.value = MapEvent.ShowMessage("Ошибка: ${e.message}")
            }
        }
    }

    /**
     * Получить записи для воспоминания
     */
    fun getMemoryEntries(memoryPointId: Long, callback: (List<MemoryEntry>) -> Unit) {
        viewModelScope.launch {
            try {
                val entries = repository.getMemoryEntries(memoryPointId)
                callback(entries)
            } catch (e: Exception) {
                _events.value = MapEvent.ShowMessage("Ошибка загрузки записей: ${e.message}")
                callback(emptyList())
            }
        }
    }

    fun addMediaEntry(pointId: Long, type: String, filePath: String, fileSize: Long, duration: Long? = null) {
        viewModelScope.launch {
            try {
                val entry = MemoryEntry(
                    memoryPointId = pointId,
                    type = type,
                    content = filePath.substringAfterLast('/'),
                    filePath = filePath,
                    fileSize = fileSize,
                    duration = duration
                )
                repository.addMemoryEntry(entry)
                Timber.d("Media entry added: type=$type, pointId=$pointId")
            } catch (e: Exception) {
                Timber.e(e, "Error adding media entry")
                _events.value = MapEvent.ShowMessage("Ошибка сохранения медиа: ${e.message}")
            }
        }
    }

    /**
     * Включить режим выбора точки
     */
    fun startPointSelection() {
        _isSelectingPoint.value = true
        _events.value = MapEvent.ShowMessage("Выберите место на карте для воспоминания")
    }

    /**
     * Отменить выбор точки
     */
    fun cancelPointSelection() {
        _isSelectingPoint.value = false
    }

    /**
     * Получить воспоминание по ID
     */
    fun getMemoryPointById(pointId: Long): MemoryPoint? {
        return _memoryPoints.value?.find { it.pointId == pointId }
    }
}

