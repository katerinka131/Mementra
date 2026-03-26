package com.example.mementra.fragments

import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.mementra.MainActivity
import com.example.mementra.database.models.MemoryEntry
import com.example.mementra.database.models.MemoryPoint
import com.example.mementra.databinding.FragmentMapBinding
import com.example.mementra.utils.MemoryDialogHelper
import com.example.mementra.utils.PermissionHelper
import com.example.mementra.viewmodels.MapEvent
import com.example.mementra.viewmodels.MapViewModel
import com.example.mementra.viewmodels.MapViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

private data class PendingMedia(
    val filePath: String,
    val type: String,
    val fileSize: Long,
    val duration: Long? = null
)

class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private lateinit var mapView: MapView
    private lateinit var viewModel: MapViewModel

    private var selectedPoint: GeoPoint? = null
    private var selectionMarker: Marker? = null
    private val memoryMarkers = mutableMapOf<Long, Marker>()

    // Для режима добавления: медиа собирается до сохранения точки
    private val pendingMedia = mutableListOf<PendingMedia>()

    // Для режима редактирования: pointId уже известен, медиа сохраняется сразу
    private var editingPointId: Long? = null

    // Флаг для отслеживания сохранения
    private var isSaving = false
    private var currentDialog: android.app.Dialog? = null
    private var cameraPhotoUri: Uri? = null
    private var cameraPhotoFile: File? = null
    private var cameraVideoUri: Uri? = null
    private var cameraVideoFile: File? = null

    // --- Activity Result Launchers ---

    private val pickImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            handleMultipleUris(result.data, MemoryEntry.TYPE_PHOTO, ".jpg")
        }
    }

    private val takePhoto = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            cameraPhotoFile?.let { file ->
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    if (file.exists() && file.length() > 1024) {
                        handleCameraFile(file, MemoryEntry.TYPE_PHOTO)
                    } else {
                        showMessage("Фото не сохранено или файл поврежден")
                        if (file.exists() && file.length() == 0L) file.delete()
                    }
                }, 500)
            }
        }
    }

    private val pickAudio = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            handleMultipleUris(result.data, MemoryEntry.TYPE_AUDIO, ".m4a")
        }
    }

    private val pickVideo = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            handleMultipleUris(result.data, MemoryEntry.TYPE_VIDEO, ".mp4")
        }
    }

    private val captureVideo = registerForActivityResult(ActivityResultContracts.CaptureVideo()) { success ->
        if (success) {
            cameraVideoFile?.let { file ->
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    if (file.exists() && file.length() > 1024) {
                        handleCameraFile(file, MemoryEntry.TYPE_VIDEO)
                    } else {
                        showMessage("Видео не сохранено или файл поврежден")
                        if (file.exists() && file.length() == 0L) file.delete()
                    }
                }, 500)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)

        val mainActivity = requireActivity() as MainActivity
        val repository = mainActivity.memoryRepo  // это RoomMemoryRepository
        val userId = mainActivity.userId

        val factory = MapViewModelFactory(repository, userId)
        viewModel = ViewModelProvider(this, factory)[MapViewModel::class.java]

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

    private fun setupMap() {
        mapView = binding.mapView
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.minZoomLevel = 3.0
        mapView.maxZoomLevel = 19.0

        val moscow = GeoPoint(55.7558, 37.6173)
        mapView.controller.setZoom(12.0)
        mapView.controller.setCenter(moscow)

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

    private fun setupFab() {
        binding.fabAddMemory.setOnClickListener {
            if (viewModel.isSelectingPoint.value == true) {
                viewModel.cancelPointSelection()
            } else {
                viewModel.startPointSelection()
            }
        }
    }

    private fun observeViewModel() {
        viewModel.memoryPoints.observe(viewLifecycleOwner) { points ->
            memoryMarkers.values.forEach { mapView.overlays.remove(it) }
            memoryMarkers.clear()
            points.forEach { point ->
                val geoPoint = GeoPoint(point.latitude, point.longitude)
                addMemoryMarker(geoPoint, point.title, point.pointId, point.emoji)
            }
            mapView.invalidate()
        }

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

        viewModel.events.observe(viewLifecycleOwner) { event ->
            when (event) {
                is MapEvent.ShowMessage -> showMessage(event.message)
                is MapEvent.MemoryAdded -> {
                    // Точка добавлена, теперь сохраняем медиа последовательно
                    if (!isSaving && pendingMedia.isNotEmpty()) {
                        savePendingMediaSequentially(event.pointId)
                    }
                }
                is MapEvent.MemoryUpdated -> { updateMemoryMarker(event.memoryPoint) }
                is MapEvent.MemoryDeleted -> { removeMemoryMarker(event.pointId) }
                is MapEvent.FavoriteToggled -> { }
            }
        }
    }

    private fun savePendingMediaSequentially(pointId: Long) {
        if (pendingMedia.isEmpty()) return

        isSaving = true

        // Используем корутину для последовательного сохранения
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val mainActivity = requireActivity() as MainActivity
                val repository = mainActivity.memoryRepo

                var savedCount = 0
                for (media in pendingMedia) {
                    val file = File(media.filePath)
                    if (file.exists() && file.length() > 1024) {
                        Timber.d("Saving media: ${media.type}, file: ${file.absolutePath}, size: ${file.length()}")

                        // Создаем entry и сохраняем через репозиторий напрямую
                        val entry = MemoryEntry(
                            memoryPointId = pointId,
                            type = media.type,
                            content = file.name,
                            filePath = media.filePath,
                            fileSize = file.length(),
                            duration = media.duration,
                            orderIndex = savedCount,
                            entryId = 0,
                            thumbnailPath = null
                        )

                        repository.addMemoryEntry(entry)
                        savedCount++

                        withContext(Dispatchers.Main) {
                            showMessage("${mediaTypeName(media.type)} сохранено")
                        }
                    } else {
                        Timber.e("File invalid: ${media.filePath}, exists: ${file.exists()}, size: ${file.length()}")
                        withContext(Dispatchers.Main) {
                            showMessage("Файл ${mediaTypeName(media.type)} поврежден и не сохранен")
                        }
                        if (file.exists()) file.delete()
                    }
                }

                pendingMedia.clear()

                withContext(Dispatchers.Main) {
                    if (savedCount > 0) {
                        showMessage("Сохранено $savedCount файлов")
                    }
                    Timber.d("Saved $savedCount pending media entries for pointId=$pointId")

                    // Обновляем список точек, чтобы показать новые медиа
                    viewModel.loadMemoryPoints()
                }
            } catch (e: Exception) {
                Timber.e(e, "Error saving pending media")
                withContext(Dispatchers.Main) {
                    showMessage("Ошибка сохранения: ${e.message}")
                    pendingMedia.clear()
                }
            } finally {
                isSaving = false
            }
        }
    }

    // --- Map interactions ---

    private fun onMapClick(point: GeoPoint) {
        selectedPoint = point
        updateSelectionMarker(point)
        showAddMemoryDialog()
    }

    private fun updateSelectionMarker(point: GeoPoint) {
        selectionMarker?.let { mapView.overlays.remove(it) }
        selectionMarker = Marker(mapView).apply {
            position = point
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            setIcon(requireContext().getDrawable(android.R.drawable.presence_online))
            title = "Выбранное место"
        }
        mapView.overlays.add(selectionMarker)
        mapView.invalidate()
    }

    private fun cancelPointSelection() {
        selectedPoint = null
        selectionMarker?.let {
            mapView.overlays.remove(it)
            selectionMarker = null
            mapView.invalidate()
        }
    }

    // --- Dialogs ---

    private fun showAddMemoryDialog() {
        pendingMedia.clear()
        editingPointId = null
        isSaving = false

        val mainActivity = requireActivity() as MainActivity
        val repo = mainActivity.memoryRepo
        val userId = mainActivity.userId

        viewLifecycleOwner.lifecycleScope.launch {
            val tags = withContext(Dispatchers.IO) { repo.getTagsForUser(userId) }
            MemoryDialogHelper.showAddMemoryDialog(
                context = requireContext(),
                availableTags = tags,
                initialSelectedTags = emptyList(),
                onRequestNewTag = { name, done ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        val created = try {
                            withContext(Dispatchers.IO) { repo.getOrCreateTag(userId, name) }
                        } catch (_: Exception) {
                            null
                        }
                        withContext(Dispatchers.Main) { done(created) }
                    }
                },
                onSave = { title, description, emoji, selectedTags ->
                    selectedPoint?.let { point ->
                        viewModel.addMemoryPoint(
                            title = title,
                            description = description,
                            latitude = point.latitude,
                            longitude = point.longitude,
                            emoji = emoji,
                            selectedTags = selectedTags
                        )
                    }
                    viewModel.cancelPointSelection()
                },
                onCancel = {
                    pendingMedia.clear()
                    viewModel.cancelPointSelection()
                },
                onAddPhoto = { showPhotoOptions() },
                onAddVoice = { showAudioOptions() },
                onAddVideo = { showVideoOptions() }
            )
        }
    }

    private fun showMemoryDetails(pointId: Long) {
        val memoryPoint = viewModel.getMemoryPointById(pointId) ?: return
        viewModel.getMemoryEntries(pointId) { entries ->
            // Закрываем предыдущий диалог, если он есть
            currentDialog?.dismiss()

            MemoryDialogHelper.showMemoryDetailsDialog(
                context = requireContext(),
                memoryPoint = memoryPoint,
                entries = entries,
                onFavoriteToggle = { newState ->
                    viewModel.toggleFavorite(pointId, newState)
                },
                onEdit = { showEditMemoryDialog(memoryPoint) },
                onDelete = {
                    MemoryDialogHelper.showDeleteConfirmationDialog(
                        context = requireContext(),
                        memoryTitle = memoryPoint.title,
                        onConfirm = {
                            viewModel.deleteMemoryPoint(pointId, memoryPoint.title)
                            currentDialog?.dismiss()
                        }
                    )
                },
                onMediaDelete = { entry ->
                    // Удаляем медиафайл
                    viewLifecycleOwner.lifecycleScope.launch {
                        try {
                            Timber.d("=== DELETING MEDIA ===")
                            Timber.d("Entry ID: ${entry.entryId}")
                            Timber.d("Type: ${entry.type}")
                            Timber.d("Path: ${entry.filePath}")

                            // Удаляем файл
                            val file = File(entry.filePath)
                            if (file.exists()) {
                                val deleted = file.delete()
                                Timber.d("File deleted: $deleted")
                            }

                            // Удаляем запись из базы
                            viewModel.deleteMediaEntry(entry.entryId)
                            showMessage("${getMediaTypeName(entry.type)} удалено")

                            // Закрываем текущий диалог
                            currentDialog?.dismiss()

                            // Открываем обновленный диалог
                            showMemoryDetails(pointId)

                        } catch (e: Exception) {
                            Timber.e(e, "Error deleting media")
                            showMessage("Ошибка при удалении: ${e.message}")
                        }
                    }
                }
            ).also { dialog ->
                currentDialog = dialog
            }
        }
    }
    private fun getMediaTypeName(type: String): String = when (type) {
        MemoryEntry.TYPE_PHOTO -> "Фото"
        MemoryEntry.TYPE_AUDIO -> "Аудио"
        MemoryEntry.TYPE_VIDEO -> "Видео"
        else -> "Файл"
    }

    private fun showEditMemoryDialog(memoryPoint: MemoryPoint) {
        editingPointId = memoryPoint.pointId

        val mainActivity = requireActivity() as MainActivity
        val repo = mainActivity.memoryRepo
        val userId = mainActivity.userId

        viewLifecycleOwner.lifecycleScope.launch {
            val tags = withContext(Dispatchers.IO) { repo.getTagsForUser(userId) }
            MemoryDialogHelper.showEditMemoryDialog(
                context = requireContext(),
                memoryPoint = memoryPoint,
                availableTags = tags,
                onRequestNewTag = { name, done ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        val created = try {
                            withContext(Dispatchers.IO) { repo.getOrCreateTag(userId, name) }
                        } catch (_: Exception) {
                            null
                        }
                        withContext(Dispatchers.Main) { done(created) }
                    }
                },
                onSave = { title, description, emoji, selectedTags ->
                    val updatedPoint = memoryPoint.copy(
                        title = title,
                        description = description,
                        emoji = emoji,
                        visitDate = System.currentTimeMillis(),
                        tags = selectedTags
                    )
                    viewModel.updateMemoryPoint(updatedPoint)
                    editingPointId = null
                },
                onCancel = { editingPointId = null },
                onAddPhoto = { showPhotoOptions() },
                onAddVoice = { showAudioOptions() },
                onAddVideo = { showVideoOptions() }
            )
        }
    }

    // --- Map markers ---

    private fun addMemoryMarker(point: GeoPoint, title: String, pointId: Long, emoji: String = MemoryPoint.DEFAULT_EMOJI) {
        val marker = Marker(mapView).apply {
            position = point
            this.title = title
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            val bitmap = MemoryDialogHelper.createEmojiBitmap(requireContext(), emoji)
            setIcon(BitmapDrawable(resources, bitmap))
        }
        marker.setOnMarkerClickListener { _, _ ->
            showMemoryDetails(pointId)
            true
        }
        mapView.overlays.add(marker)
        memoryMarkers[pointId] = marker
    }

    private fun updateMemoryMarker(memoryPoint: MemoryPoint) {
        memoryMarkers[memoryPoint.pointId]?.let { marker ->
            marker.title = memoryPoint.title
            val bitmap = MemoryDialogHelper.createEmojiBitmap(requireContext(), memoryPoint.emoji)
            marker.setIcon(BitmapDrawable(resources, bitmap))
            mapView.invalidate()
        }
    }

    private fun removeMemoryMarker(pointId: Long) {
        memoryMarkers[pointId]?.let {
            mapView.overlays.remove(it)
            memoryMarkers.remove(pointId)
            mapView.invalidate()
        }
    }

    // --- Photo ---

    private fun showPhotoOptions() {
        val options = arrayOf("Выбрать из галереи", "Сделать фото")
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Добавить фото")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> PermissionHelper.requestStoragePermission(
                        fragment = this,
                        onGranted = { openImagePicker() },
                        onDenied = { showMessage("Для выбора фото необходимо разрешение") }
                    )
                    1 -> PermissionHelper.requestCameraPermission(
                        fragment = this,
                        onGranted = { launchCamera() },
                        onDenied = { showMessage("Для съёмки фото необходимо разрешение камеры") }
                    )
                }
            }
            .show()
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        pickImage.launch(intent)
    }

    private fun launchCamera() {
        try {
            val file = createMediaFile("IMG", ".jpg")
            cameraPhotoFile = file
            cameraPhotoUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.provider",
                file
            )
            takePhoto.launch(cameraPhotoUri)
        } catch (e: Exception) {
            Timber.e(e, "Error launching camera")
            showMessage("Ошибка запуска камеры: ${e.message}")
        }
    }

    // --- Audio ---

    private fun showAudioOptions() {
        val options = arrayOf("Записать аудио", "Выбрать аудио файл")
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Добавить аудио")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> PermissionHelper.requestAudioPermission(
                        fragment = this,
                        onGranted = { startAudioRecording() },
                        onDenied = { showMessage("Для записи аудио необходимо разрешение микрофона") }
                    )
                    1 -> openAudioPicker()
                }
            }
            .show()
    }

    private fun startAudioRecording() {
        AudioRecordDialog.show(requireContext()) { filePath, durationSec ->
            val file = File(filePath)
            if (file.exists() && file.length() > 1024) {
                val pointId = editingPointId
                if (pointId != null) {
                    // Режим редактирования - сохраняем сразу через репозиторий
                    val mainActivity = requireActivity() as MainActivity
                    val entry = MemoryEntry(
                        memoryPointId = pointId,
                        type = MemoryEntry.TYPE_AUDIO,
                        content = file.name,
                        filePath = filePath,
                        fileSize = file.length(),
                        duration = durationSec,
                        orderIndex = 0,
                        entryId = 0,
                        thumbnailPath = null
                    )
                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                        mainActivity.memoryRepo.addMemoryEntry(entry)
                        withContext(Dispatchers.Main) {
                            showMessage("Аудио сохранено")
                        }
                    }
                } else {
                    // Режим создания - добавляем в pending
                    pendingMedia.add(PendingMedia(filePath, MemoryEntry.TYPE_AUDIO, file.length(), durationSec))
                    showMessage("Аудио добавлено")
                }
            } else {
                showMessage("Аудио не сохранено или файл поврежден")
                if (file.exists() && file.length() == 0L) file.delete()
            }
        }
    }

    private fun openAudioPicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "audio/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        pickAudio.launch(intent)
    }

    // --- Video ---

    private fun showVideoOptions() {
        val options = arrayOf("Записать видео", "Выбрать из галереи")
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Добавить видео")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> PermissionHelper.requestCameraPermission(
                        fragment = this,
                        onGranted = { launchVideoCapture() },
                        onDenied = { showMessage("Для записи видео необходимо разрешение камеры") }
                    )
                    1 -> PermissionHelper.requestStoragePermission(
                        fragment = this,
                        onGranted = { openVideoPicker() },
                        onDenied = { showMessage("Для выбора видео необходимо разрешение") }
                    )
                }
            }
            .show()
    }

    private fun launchVideoCapture() {
        try {
            val file = createMediaFile("VID", ".mp4")
            cameraVideoFile = file
            cameraVideoUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.provider",
                file
            )
            captureVideo.launch(cameraVideoUri)
        } catch (e: Exception) {
            Timber.e(e, "Error launching video capture")
            showMessage("Ошибка запуска записи видео: ${e.message}")
        }
    }

    private fun openVideoPicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI).apply {
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        pickVideo.launch(intent)
    }

    // --- Media result handling ---

    private fun handleMultipleUris(data: Intent?, type: String, ext: String) {
        if (data == null) return
        val uris = mutableListOf<Uri>()

        data.clipData?.let { clip ->
            for (i in 0 until clip.itemCount) {
                uris.add(clip.getItemAt(i).uri)
            }
        }
        if (uris.isEmpty()) {
            data.data?.let { uris.add(it) }
        }

        for (uri in uris) {
            handleMediaResult(uri, type, ext)
        }
    }

    private fun handleCameraFile(file: File, type: String) {
        if (!file.exists() || file.length() < 1024) {
            showMessage("Файл поврежден или слишком маленький")
            if (file.exists()) file.delete()
            return
        }

        val pointId = editingPointId
        if (pointId != null) {
            // Режим редактирования - сохраняем сразу через репозиторий
            val mainActivity = requireActivity() as MainActivity
            val entry = MemoryEntry(
                memoryPointId = pointId,
                type = type,
                content = file.name,
                filePath = file.absolutePath,
                fileSize = file.length(),
                duration = null,
                orderIndex = 0,
                entryId = 0,
                thumbnailPath = null
            )
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                mainActivity.memoryRepo.addMemoryEntry(entry)
                withContext(Dispatchers.Main) {
                    showMessage(mediaTypeName(type) + " сохранено")
                }
            }
        } else {
            // Режим создания - добавляем в pending
            pendingMedia.add(PendingMedia(file.absolutePath, type, file.length()))
            showMessage(mediaTypeName(type) + " добавлено")
        }
    }

    private fun handleMediaResult(uri: Uri, type: String, ext: String) {
        try {
            val file = copyUriToFile(uri, type.uppercase(), ext)
            if (file == null || !file.exists() || file.length() < 1024) {
                showMessage("Ошибка копирования файла или файл поврежден")
                return
            }

            val pointId = editingPointId
            if (pointId != null) {
                // Режим редактирования - сохраняем сразу через репозиторий
                val mainActivity = requireActivity() as MainActivity
                val entry = MemoryEntry(
                    memoryPointId = pointId,
                    type = type,
                    content = file.name,
                    filePath = file.absolutePath,
                    fileSize = file.length(),
                    duration = null,
                    orderIndex = 0,
                    entryId = 0,
                    thumbnailPath = null
                )
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    mainActivity.memoryRepo.addMemoryEntry(entry)
                    withContext(Dispatchers.Main) {
                        showMessage(mediaTypeName(type) + " сохранено")
                    }
                }
            } else {
                // Режим создания - добавляем в pending
                pendingMedia.add(PendingMedia(file.absolutePath, type, file.length()))
                showMessage(mediaTypeName(type) + " добавлено")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error handling media result")
            showMessage("Ошибка сохранения: ${e.message}")
        }
    }

    private fun copyUriToFile(uri: Uri, prefix: String, ext: String): File? {
        return try {
            val file = createMediaFile(prefix, ext)
            requireContext().contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            if (file.exists() && file.length() > 1024) file else null
        } catch (e: Exception) {
            Timber.e(e, "Error copying URI to file")
            null
        }
    }

    private fun createMediaFile(prefix: String, ext: String): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.getDefault()).format(Date())
        val dir = File(requireContext().filesDir, "media")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "${prefix}_${timestamp}${ext}")
    }

    private fun mediaTypeName(type: String): String = when (type) {
        MemoryEntry.TYPE_PHOTO -> "Фото"
        MemoryEntry.TYPE_AUDIO -> "Аудио"
        MemoryEntry.TYPE_VIDEO -> "Видео"
        else -> "Файл"
    }

    private fun showMessage(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
        viewModel.loadMemoryPoints()
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