package com.example.mementra.fragments

import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.mementra.MainActivity
import com.example.mementra.database.models.MemoryEntry
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
                if (file.exists() && file.length() > 0) {
                    handleCameraFile(file, MemoryEntry.TYPE_PHOTO)
                }
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
                if (file.exists() && file.length() > 0) {
                    handleCameraFile(file, MemoryEntry.TYPE_VIDEO)
                } else {
                    showMessage("Видео не сохранено")
                }
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
        val repository = mainActivity.memoryRepo
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
                    savePendingMedia(event.pointId)
                }
                is MapEvent.MemoryUpdated -> { updateMemoryMarker(event.memoryPoint) }
                is MapEvent.MemoryDeleted -> { removeMemoryMarker(event.pointId) }
                is MapEvent.FavoriteToggled -> { }
            }
        }
    }

    private fun savePendingMedia(pointId: Long) {
        if (pendingMedia.isEmpty()) return
        for (media in pendingMedia) {
            viewModel.addMediaEntry(pointId, media.type, media.filePath, media.fileSize, media.duration)
        }
        val count = pendingMedia.size
        pendingMedia.clear()
        Timber.d("Saved $count pending media entries for pointId=$pointId")
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

        MemoryDialogHelper.showAddMemoryDialog(
            context = requireContext(),
            onSave = { title, description, emoji ->
                selectedPoint?.let { point ->
                    viewModel.addMemoryPoint(
                        title = title,
                        description = description,
                        latitude = point.latitude,
                        longitude = point.longitude,
                        emoji = emoji
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
                onEdit = { showEditMemoryDialog(memoryPoint) },
                onDelete = {
                    MemoryDialogHelper.showDeleteConfirmationDialog(
                        context = requireContext(),
                        memoryTitle = memoryPoint.title,
                        onConfirm = { viewModel.deleteMemoryPoint(pointId, memoryPoint.title) }
                    )
                }
            )
        }
    }

    private fun showEditMemoryDialog(memoryPoint: MemoryPoint) {
        editingPointId = memoryPoint.pointId

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
                viewModel.updateMemoryPoint(updatedPoint)
                editingPointId = null
            },
            onCancel = { editingPointId = null },
            onAddPhoto = { showPhotoOptions() },
            onAddVoice = { showAudioOptions() },
            onAddVideo = { showVideoOptions() }
        )
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
            if (file.exists()) {
                val pointId = editingPointId
                if (pointId != null) {
                    viewModel.addMediaEntry(pointId, MemoryEntry.TYPE_AUDIO, filePath, file.length(), durationSec)
                    showMessage("Аудио сохранено")
                } else {
                    pendingMedia.add(PendingMedia(filePath, MemoryEntry.TYPE_AUDIO, file.length(), durationSec))
                    showMessage("Аудио добавлено")
                }
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
        val pointId = editingPointId
        if (pointId != null) {
            viewModel.addMediaEntry(pointId, type, file.absolutePath, file.length())
            showMessage(mediaTypeName(type) + " сохранено")
        } else {
            pendingMedia.add(PendingMedia(file.absolutePath, type, file.length()))
            showMessage(mediaTypeName(type) + " добавлено")
        }
    }

    private fun handleMediaResult(uri: Uri, type: String, ext: String) {
        try {
            val file = copyUriToFile(uri, type.uppercase(), ext)
            if (file == null || !file.exists()) {
                showMessage("Ошибка копирования файла")
                return
            }

            val pointId = editingPointId
            if (pointId != null) {
                viewModel.addMediaEntry(pointId, type, file.absolutePath, file.length())
                showMessage(mediaTypeName(type) + " сохранено")
            } else {
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
                FileOutputStream(file).use { output -> input.copyTo(output) }
            }
            if (file.length() > 0) file else null
        } catch (e: Exception) {
            Timber.e(e, "Error copying URI to file")
            null
        }
    }

    private fun createMediaFile(prefix: String, ext: String): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.getDefault()).format(Date())
        val dir = File(requireContext().filesDir, "media")
        dir.mkdirs()
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
