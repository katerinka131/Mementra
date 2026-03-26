package com.example.mementra.fragments

import android.content.Intent
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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mementra.MainActivity
import com.example.mementra.adapters.FavoritesAdapter
import com.example.mementra.database.models.MemoryEntry
import com.example.mementra.database.models.MemoryPoint
import com.example.mementra.databinding.FragmentFavoritesBinding
import com.example.mementra.utils.MemoryDialogHelper
import com.example.mementra.utils.PermissionHelper
import com.example.mementra.viewmodels.FavoritesUiState
import com.example.mementra.viewmodels.FavoritesViewModel
import com.example.mementra.viewmodels.FavoritesViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

private data class FavoritesPendingMedia(
    val filePath: String,
    val type: String,
    val fileSize: Long,
    val duration: Long? = null
)

class FavoritesFragment : Fragment() {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: FavoritesViewModel
    private lateinit var favoritesAdapter: FavoritesAdapter

    private val pendingMedia = mutableListOf<FavoritesPendingMedia>()
    private var editingPointId: Long? = null
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
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)

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

    private fun setupRecyclerView() {
        favoritesAdapter = FavoritesAdapter(emptyList()) { memory ->
            showMemoryDetails(memory.pointId)
        }

        binding.favoritesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = favoritesAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is FavoritesUiState.Idle -> {}
                is FavoritesUiState.Loading -> {
                    binding.favoritesPlaceholder.visibility = View.GONE
                    binding.favoritesRecyclerView.visibility = View.GONE
                }
                is FavoritesUiState.Empty -> {
                    binding.favoritesPlaceholder.visibility = View.VISIBLE
                    binding.favoritesRecyclerView.visibility = View.GONE
                    binding.favoritesPlaceholderText.text =
                        "Здесь будут ваши самые важные моменты\n\nПока нет избранных воспоминаний\n\nДобавьте воспоминания в избранное на карте!"
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

        viewModel.message.observe(viewLifecycleOwner) { message ->
            message?.let { showMessage(it) }
        }
    }

    private fun showMemoryDetails(pointId: Long) {
        val memoryPoint = viewModel.getMemoryById(pointId) ?: return
        val mainActivity = requireActivity() as MainActivity
        val repository = mainActivity.memoryRepo

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val entries = repository.getMemoryEntries(pointId)

                // Закрываем предыдущий диалог
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
                            onConfirm = { viewModel.deleteMemory(pointId, memoryPoint.title) }
                        )
                    },
                    onMediaDelete = { entry ->
                        viewLifecycleOwner.lifecycleScope.launch {
                            try {
                                Timber.d("=== DELETING MEDIA ===")
                                Timber.d("Entry ID: ${entry.entryId}")
                                Timber.d("Type: ${entry.type}")
                                Timber.d("Path: ${entry.filePath}")

                                val file = File(entry.filePath)
                                if (file.exists()) {
                                    file.delete()
                                }

                                viewModel.deleteMediaEntry(entry.entryId)
                                showMessage("${getMediaTypeName(entry.type)} удалено")

                                currentDialog?.dismiss()
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
            } catch (e: Exception) {
                showMessage("Ошибка загрузки записей: ${e.message}")
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
        pendingMedia.clear()

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
                savePendingMedia(memoryPoint.pointId)
                editingPointId = null
            },
            onCancel = {
                pendingMedia.clear()
                editingPointId = null
            },
            onAddPhoto = { showPhotoOptions() },
            onAddVoice = { showAudioOptions() },
            onAddVideo = { showVideoOptions() }
        )
    }

    private fun savePendingMedia(pointId: Long) {
        if (pendingMedia.isEmpty()) return
        val mainActivity = requireActivity() as MainActivity
        val repository = mainActivity.memoryRepo

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                for (media in pendingMedia) {
                    val entry = MemoryEntry(
                        memoryPointId = pointId,
                        type = media.type,
                        content = media.filePath.substringAfterLast('/'),
                        filePath = media.filePath,
                        fileSize = media.fileSize,
                        duration = media.duration
                    )
                    repository.addMemoryEntry(entry)
                }
                pendingMedia.clear()
                viewModel.loadFavorites()
            } catch (e: Exception) {
                Timber.e(e, "Error saving pending media")
                viewModel.loadFavorites()
            }
        }
    }

    // --- Photo ---
    private fun showPhotoOptions() {
        val options = arrayOf("Выбрать из галереи", "Сделать фото")
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Добавить фото")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> PermissionHelper.requestStoragePermission(this, { openImagePicker() }, { showMessage("Нужен доступ к галерее") })
                    1 -> PermissionHelper.requestCameraPermission(this, { launchCamera() }, { showMessage("Нужен доступ к камере") })
                }
            }.show()
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
            cameraPhotoUri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", file)
            takePhoto.launch(cameraPhotoUri)
        } catch (e: Exception) {
            showMessage("Ошибка камеры: ${e.message}")
        }
    }

    // --- Audio ---
    private fun showAudioOptions() {
        val options = arrayOf("Записать аудио", "Выбрать аудио файл")
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Добавить аудио")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> PermissionHelper.requestAudioPermission(this, { startAudioRecording() }, { showMessage("Нужен микрофон") })
                    1 -> openAudioPicker()
                }
            }.show()
    }

    private fun startAudioRecording() {
        AudioRecordDialog.show(requireContext()) { filePath, durationSec ->
            val file = File(filePath)
            if (file.exists() && file.length() > 1024) {
                val pointId = editingPointId
                if (pointId != null) {
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
                            viewModel.loadFavorites()
                        }
                    }
                } else {
                    pendingMedia.add(FavoritesPendingMedia(filePath, MemoryEntry.TYPE_AUDIO, file.length(), durationSec))
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
                    0 -> PermissionHelper.requestCameraPermission(this, { launchVideoCapture() }, { showMessage("Нужен доступ к камере") })
                    1 -> PermissionHelper.requestStoragePermission(this, { openVideoPicker() }, { showMessage("Нужен доступ к галерее") })
                }
            }.show()
    }

    private fun launchVideoCapture() {
        try {
            val file = createMediaFile("VID", ".mp4")
            cameraVideoFile = file
            cameraVideoUri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", file)
            captureVideo.launch(cameraVideoUri)
        } catch (e: Exception) {
            showMessage("Ошибка видео: ${e.message}")
        }
    }

    private fun openVideoPicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI).apply {
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        pickVideo.launch(intent)
    }

    // --- Handling Results ---
    private fun handleMultipleUris(data: Intent?, type: String, ext: String) {
        if (data == null) return
        val uris = mutableListOf<Uri>()
        data.clipData?.let { clip -> for (i in 0 until clip.itemCount) uris.add(clip.getItemAt(i).uri) }
        if (uris.isEmpty()) data.data?.let { uris.add(it) }
        for (uri in uris) handleMediaResult(uri, type, ext)
    }

    private fun handleCameraFile(file: File, type: String) {
        if (!file.exists() || file.length() < 1024) {
            showMessage("Файл поврежден или слишком маленький")
            if (file.exists()) file.delete()
            return
        }

        val pointId = editingPointId
        if (pointId != null) {
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
                    showMessage(getMediaTypeName(type) + " сохранено")
                    viewModel.loadFavorites()
                }
            }
        } else {
            pendingMedia.add(FavoritesPendingMedia(file.absolutePath, type, file.length()))
            showMessage(getMediaTypeName(type) + " добавлено")
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
                        showMessage(getMediaTypeName(type) + " сохранено")
                        viewModel.loadFavorites()
                    }
                }
            } else {
                pendingMedia.add(FavoritesPendingMedia(file.absolutePath, type, file.length()))
                showMessage(getMediaTypeName(type) + " добавлено")
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
            if (file.exists() && file.length() > 1024) file else null
        } catch (e: Exception) { null }
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
        viewModel.loadFavorites()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}