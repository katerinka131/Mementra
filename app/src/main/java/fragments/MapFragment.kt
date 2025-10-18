package com.example.mementra.fragments

import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.mementra.MainActivity
import com.example.mementra.database.AppDatabaseHelper
import com.example.mementra.database.MemoryPointRepository
import com.example.mementra.database.UserManager
import com.example.mementra.database.models.MemoryPoint
import com.example.mementra.databinding.FragmentMapBinding
import com.example.mementra.utils.MemoryDialogHelper
import com.example.mementra.utils.PermissionHelper
import com.example.mementra.viewmodels.MapEvent
import com.example.mementra.viewmodels.MapViewModel
import com.example.mementra.viewmodels.MapViewModelFactory
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import android.widget.Toast

class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var mapView: MapView
    private lateinit var viewModel: MapViewModel

    private var selectedPoint: GeoPoint? = null
    private var selectionMarker: Marker? = null
    private val memoryMarkers = mutableMapOf<Long, Marker>() // pointId -> Marker

    // Для выбора фото
    private val pickImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { imageUri ->
                showMessage("Фото выбрано: $imageUri")
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(layoutInflater, container, false)

        // Инициализация ViewModel
        val mainActivity = requireActivity() as MainActivity
        val repository = mainActivity.memoryRepo
        val userId = mainActivity.userId
        
        val factory = MapViewModelFactory(repository, userId)
        viewModel = ViewModelProvider(this, factory)[MapViewModel::class.java]

        // Конфигурация OSMDroid
        Configuration.getInstance().load(
            requireContext(),
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext())
        )
        Configuration.getInstance().userAgentValue = requireContext().packageName

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMap()
        setupFab()
        observeViewModel()
    }

    /**
     * Настройка карты
     */
    private fun setupMap() {
        mapView = binding.mapView

        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.minZoomLevel = 3.0
        mapView.maxZoomLevel = 19.0

        // Центр карты - Москва
        val moscow = GeoPoint(55.7558, 37.6173)
        mapView.controller.setZoom(12.0)
        mapView.controller.setCenter(moscow)

        // Обработчик кликов по карте
        mapView.setOnTouchListener { _, event ->
            if (event.action == android.view.MotionEvent.ACTION_UP && 
                viewModel.isSelectingPoint.value == true) {
                val projection = mapView.projection
                val geoPoint = projection.fromPixels(event.x.toInt(), event.y.toInt()) as GeoPoint
                onMapClick(geoPoint)
                true
            } else {
                false
            }
        }

        binding.mapPlaceholder.visibility = View.GONE
        mapView.invalidate()
    }

    /**
     * Настройка FAB кнопки
     */
    private fun setupFab() {
        binding.fabAddMemory.setOnClickListener {
            if (viewModel.isSelectingPoint.value == true) {
                // Отключаем режим выбора
                viewModel.cancelPointSelection()
            } else {
                // Включаем режим выбора
                viewModel.startPointSelection()
            }
        }
    }

    /**
     * Подписка на изменения ViewModel
     */
    private fun observeViewModel() {
        // Наблюдаем за списком воспоминаний
        viewModel.memoryPoints.observe(viewLifecycleOwner) { points ->
            // Очищаем старые маркеры
            memoryMarkers.values.forEach { mapView.overlays.remove(it) }
            memoryMarkers.clear()

            // Добавляем новые маркеры
            points.forEach { point ->
                val geoPoint = GeoPoint(point.latitude, point.longitude)
                addMemoryMarker(geoPoint, point.title, point.pointId)
            }
            mapView.invalidate()
        }

        // Наблюдаем за режимом выбора точки
        viewModel.isSelectingPoint.observe(viewLifecycleOwner) { isSelecting ->
            if (isSelecting) {
                binding.fabAddMemory.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                binding.tvSelectPointHint.visibility = View.VISIBLE
            } else {
                binding.fabAddMemory.setImageResource(android.R.drawable.ic_menu_add)
                binding.tvSelectPointHint.visibility = View.GONE
                cancelPointSelection()
            }
        }

        // Наблюдаем за событиями
        viewModel.events.observe(viewLifecycleOwner) { event ->
            when (event) {
                is MapEvent.ShowMessage -> showMessage(event.message)
                is MapEvent.MemoryAdded -> {
                    // Маркер уже добавлен через наблюдение за memoryPoints
                }
                is MapEvent.MemoryUpdated -> {
                    updateMemoryMarker(event.memoryPoint)
                }
                is MapEvent.MemoryDeleted -> {
                    removeMemoryMarker(event.pointId)
                }
                is MapEvent.FavoriteToggled -> {
                    // Можно добавить визуальную индикацию на маркере
                }
            }
        }
    }

    /**
     * Обработчик клика по карте
     */
    private fun onMapClick(point: GeoPoint) {
            selectedPoint = point
            updateSelectionMarker(point)
            showAddMemoryDialog()
    }

    /**
     * Обновить маркер выбора
     */
    private fun updateSelectionMarker(point: GeoPoint) {
        // Удаляем старый маркер выбора
        selectionMarker?.let {
            mapView.overlays.remove(it)
        }

        // Создаем новый маркер выбора
        selectionMarker = Marker(mapView).apply {
            position = point
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            setIcon(requireContext().getDrawable(android.R.drawable.presence_online))
            title = "Выбранное место"
        }

        mapView.overlays.add(selectionMarker)
        mapView.invalidate()
    }

    /**
     * Отменить выбор точки
     */
    private fun cancelPointSelection() {
        selectedPoint = null
        selectionMarker?.let {
            mapView.overlays.remove(it)
            selectionMarker = null
            mapView.invalidate()
        }
    }

    /**
     * Показать диалог добавления воспоминания
     */
    private fun showAddMemoryDialog() {
        MemoryDialogHelper.showAddMemoryDialog(
            context = requireContext(),
            onSave = { title, description ->
        selectedPoint?.let { point ->
                    viewModel.addMemoryPoint(
                title = title,
                description = description,
                latitude = point.latitude,
                        longitude = point.longitude
                    )
                }
                viewModel.cancelPointSelection()
            },
            onCancel = {
                viewModel.cancelPointSelection()
            },
            onAddPhoto = {
                requestPhotoPermission()
            },
            onAddVoice = {
                requestAudioPermission()
            }
        )
    }

    /**
     * Добавить маркер воспоминания на карту
     */
    private fun addMemoryMarker(point: GeoPoint, title: String, pointId: Long) {
        val marker = Marker(mapView).apply {
            position = point
            this.title = title
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            setIcon(requireContext().getDrawable(android.R.drawable.ic_menu_myplaces))
        }

        // Обработчик клика по маркеру
        marker.setOnMarkerClickListener { _, _ ->
            showMemoryDetails(pointId)
            true
        }

        mapView.overlays.add(marker)
        memoryMarkers[pointId] = marker
    }

    /**
     * Показать детали воспоминания
     */
    private fun showMemoryDetails(pointId: Long) {
        val memoryPoint = viewModel.getMemoryPointById(pointId) ?: return

        viewModel.getMemoryEntries(pointId) { entries ->
            MemoryDialogHelper.showMemoryDetailsDialog(
                context = requireContext(),
                memoryPoint = memoryPoint,
                entries = entries,
                onFavoriteToggle = { newState ->
                    viewModel.toggleFavorite(pointId, !newState)
                },
                onEdit = {
                    showEditMemoryDialog(memoryPoint)
                },
                onDelete = {
                    MemoryDialogHelper.showDeleteConfirmationDialog(
                        context = requireContext(),
                        memoryTitle = memoryPoint.title,
                        onConfirm = {
                            viewModel.deleteMemoryPoint(pointId, memoryPoint.title)
                        }
                    )
                }
            )
        }
    }

    /**
     * Показать диалог редактирования воспоминания
     */
    private fun showEditMemoryDialog(memoryPoint: MemoryPoint) {
        MemoryDialogHelper.showEditMemoryDialog(
            context = requireContext(),
            memoryPoint = memoryPoint,
            onSave = { title, description ->
                val updatedPoint = memoryPoint.copy(
                    title = title,
                    description = description,
                    visitDate = System.currentTimeMillis()
                )
                viewModel.updateMemoryPoint(updatedPoint)
            },
            onCancel = {}
        )
    }

    /**
     * Обновить маркер на карте
     */
    private fun updateMemoryMarker(memoryPoint: MemoryPoint) {
        val oldMarker = memoryMarkers[memoryPoint.pointId]
        oldMarker?.let { marker ->
            // Обновляем заголовок маркера
            marker.title = memoryPoint.title
            mapView.invalidate()
        }
    }

    /**
     * Удалить маркер с карты
     */
    private fun removeMemoryMarker(pointId: Long) {
        val marker = memoryMarkers[pointId]
        marker?.let {
            mapView.overlays.remove(it)
            memoryMarkers.remove(pointId)
            mapView.invalidate()
        }
    }

    /**
     * Запросить разрешение на фото
     */
    private fun requestPhotoPermission() {
        PermissionHelper.requestStoragePermission(
            fragment = this,
            onGranted = {
                openImagePicker()
            },
            onDenied = {
                showMessage("Для выбора фото необходимо разрешение на доступ к хранилищу")
            }
        )
    }

    /**
     * Запросить разрешение на аудио
     */
    private fun requestAudioPermission() {
        PermissionHelper.requestAudioPermission(
            fragment = this,
            onGranted = {
                showMessage("Запись голоса будет добавлена в будущем обновлении")
            },
            onDenied = {
                showMessage("Для записи голоса необходимо разрешение на доступ к микрофону")
            }
        )
    }

    /**
     * Открыть выбор изображения
     */
    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImage.launch(intent)
    }

    /**
     * Показать сообщение
     */
    private fun showMessage(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
