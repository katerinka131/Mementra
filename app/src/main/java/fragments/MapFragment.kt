package com.example.mementra.fragments

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.mementra.R
import android.view.inputmethod.EditorInfo
import android.app.Dialog
import android.view.Gravity
import com.example.mementra.database.MemoryEntry
import com.example.mementra.database.MemoryPoint
import com.example.mementra.database.MemoryPointRepository
import com.example.mementra.database.UserManager
import com.example.mementra.databinding.DialogAddMemoryBinding
import com.example.mementra.databinding.DialogMemoryDetailsBinding
import com.example.mementra.databinding.FragmentMapBinding
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.text.SimpleDateFormat
import android.view.WindowManager
import android.content.Context
import com.google.android.material.bottomsheet.BottomSheetBehavior
import android.view.inputmethod.InputMethodManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import android.view.MotionEvent
import java.util.*
import android.util.DisplayMetrics
class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private lateinit var mapView: MapView
    private lateinit var memoryPointRepository: MemoryPointRepository
    private lateinit var userManager: UserManager
    private var currentUserId: String = ""

    private var selectedPoint: GeoPoint? = null
    private var selectionMarker: Marker? = null
    private val memoryMarkers = mutableListOf<Marker>()

    // Для выбора фото
    private val pickImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { imageUri ->
                // Здесь можно сохранить URI изображения
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

        // Инициализация базы данных
        userManager = UserManager(requireContext())
        memoryPointRepository = MemoryPointRepository(com.example.mementra.database.AppDatabaseHelper(requireContext()))
        currentUserId = userManager.getUserId()

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
        loadExistingMemories()
    }

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


        mapView.setOnTouchListener { _, event ->
            if (event.action == android.view.MotionEvent.ACTION_UP && binding.fabAddMemory.tag == "selecting") {
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

    private fun onMapClick(point: GeoPoint) {
        // Если режим выбора точки активен
        if (binding.fabAddMemory.tag == "selecting") {
            selectedPoint = point
            updateSelectionMarker(point)
            showAddMemoryDialog()
        }
    }

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

    private fun setupFab() {
        binding.fabAddMemory.setOnClickListener {
            if (binding.fabAddMemory.tag != "selecting") {
                // Включаем режим выбора точки
                binding.fabAddMemory.tag = "selecting"
                binding.fabAddMemory.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                binding.tvSelectPointHint.visibility = View.VISIBLE
                showMessage("Выберите место на карте для воспоминания")
            } else {
                // Отключаем режим выбора точки
                cancelPointSelection()
            }
        }
    }

    private fun cancelPointSelection() {
        binding.fabAddMemory.tag = null
        binding.fabAddMemory.setImageResource(android.R.drawable.ic_menu_add)
        binding.tvSelectPointHint.visibility = View.GONE
        selectedPoint = null

        selectionMarker?.let {
            mapView.overlays.remove(it)
            selectionMarker = null
            mapView.invalidate()
        }
    }

    private fun showAddMemoryDialog() {
        val dialogView = layoutInflater.inflate(R.layout.bottom_sheet_add_memory, null)

        val dialog = Dialog(requireContext(), android.R.style.Theme_Material_Light_NoActionBar)
        dialog.setContentView(dialogView)

        val window = dialog.window
        window?.setGravity(android.view.Gravity.BOTTOM)
        window?.setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        window?.decorView?.setPadding(0, 0, 0, 0)
        window?.setBackgroundDrawableResource(android.R.color.white)

        val displayMetrics = resources.displayMetrics
        val height = (displayMetrics.heightPixels * 0.8).toInt()
        window?.setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, height)

        window?.setWindowAnimations(R.style.DialogAnimation)

        val etTitle = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etTitle)
        val etDescription = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etDescription)
        val btnAddPhoto = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnAddPhoto)
        val btnAddVoice = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnAddVoice)
        val btnCancel = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancel)
        val btnSave = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSave)

        // ПОЛНОЕ ОТКЛЮЧЕНИЕ ПАНЕЛИ РЕДАКТИРОВАНИЯ ТЕКСТА
        etTitle.setRawInputType(android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)
        etTitle.imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_DONE or
                android.view.inputmethod.EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING or
                android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI or
                android.view.inputmethod.EditorInfo.IME_FLAG_NO_FULLSCREEN

        etDescription.setRawInputType(android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE or android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)
        etDescription.imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_DONE or
                android.view.inputmethod.EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING or
                android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI or
                android.view.inputmethod.EditorInfo.IME_FLAG_NO_FULLSCREEN

        // Убираем подсказки и автозамену (исправленная версия)
        etTitle.inputType = etTitle.inputType or android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        etDescription.inputType = etDescription.inputType or android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS

        // Отключаем контекстное меню
        etTitle.customSelectionActionModeCallback = object : android.view.ActionMode.Callback {
            override fun onCreateActionMode(mode: android.view.ActionMode?, menu: android.view.Menu?) = false
            override fun onPrepareActionMode(mode: android.view.ActionMode?, menu: android.view.Menu?) = false
            override fun onActionItemClicked(mode: android.view.ActionMode?, item: android.view.MenuItem?) = false
            override fun onDestroyActionMode(mode: android.view.ActionMode?) {}
        }

        etDescription.customSelectionActionModeCallback = object : android.view.ActionMode.Callback {
            override fun onCreateActionMode(mode: android.view.ActionMode?, menu: android.view.Menu?) = false
            override fun onPrepareActionMode(mode: android.view.ActionMode?, menu: android.view.Menu?) = false
            override fun onActionItemClicked(mode: android.view.ActionMode?, item: android.view.MenuItem?) = false
            override fun onDestroyActionMode(mode: android.view.ActionMode?) {}
        }

        // Убираем вертикальную полосу прокрутки
        etDescription.isVerticalScrollBarEnabled = false
        etDescription.isHorizontalScrollBarEnabled = false

        btnAddPhoto.setOnClickListener {
            openImagePicker()
        }

        btnAddVoice.setOnClickListener {
            showMessage("Запись голоса будет добавлена в будущем обновлении")
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
            cancelPointSelection()
        }

        btnSave.setOnClickListener {
            val title = etTitle.text.toString()
            val description = etDescription.text.toString()

            if (title.isBlank()) {
                showMessage("Введите название воспоминания")
                return@setOnClickListener
            }

            saveMemoryPoint(title, description)
            dialog.dismiss()
        }

        dialog.setCanceledOnTouchOutside(true)
        dialog.setOnCancelListener {
            cancelPointSelection()
        }

        dialog.show()
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        etTitle.requestFocus()

        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(etTitle, InputMethodManager.SHOW_IMPLICIT)
    }
    private fun saveMemoryPoint(title: String, description: String) {
        if (title.isBlank()) {
            showMessage("Введите название воспоминания")
            return
        }

        selectedPoint?.let { point ->
            val memoryPoint = MemoryPoint(
                userId = currentUserId,
                title = title,
                description = description,
                latitude = point.latitude,
                longitude = point.longitude,
                visitDate = System.currentTimeMillis()
            )

            val pointId = memoryPointRepository.addMemoryPoint(memoryPoint)

            if (pointId != -1L) {
                // Добавляем текстовую запись если есть описание
                if (description.isNotBlank()) {
                    val textEntry = MemoryEntry(
                        memoryPointId = pointId,
                        type = "text",
                        content = description
                    )
                    memoryPointRepository.addMemoryEntry(textEntry)
                }

                addMemoryMarker(point, title, pointId)
                showMessage("Воспоминание сохранено!")
            } else {
                showMessage("Ошибка сохранения")
            }
        }

        cancelPointSelection()
    }

    private fun addMemoryMarker(point: GeoPoint, title: String, pointId: Long) {
        val marker = Marker(mapView).apply {
            position = point
            this.title = title
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            setIcon(requireContext().getDrawable(android.R.drawable.ic_menu_myplaces))
            this.setRelatedObject(pointId) // Сохраняем ID точки в маркере
        }

        // Обработчик клика по маркеру воспоминания
        marker.setOnMarkerClickListener { marker, _ ->
            val memoryPointId = marker.getRelatedObject() as? Long
            memoryPointId?.let { showMemoryDetails(it) }
            true
        }

        mapView.overlays.add(marker)
        memoryMarkers.add(marker)
        mapView.invalidate()
    }

    private fun showMemoryDetails(memoryPointId: Long) {
        val memoryPoints = memoryPointRepository.getMemoryPoints(currentUserId)
        val memoryPoint = memoryPoints.find { it.pointId == memoryPointId }

        memoryPoint?.let { point ->
            val entries = memoryPointRepository.getMemoryEntries(memoryPointId)

            val dialogView = layoutInflater.inflate(R.layout.bottom_sheet_memory_details, null)
            val dialog = Dialog(requireContext())
            dialog.setContentView(dialogView)

            val window = dialog.window
            window?.setGravity(Gravity.BOTTOM)
            window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, (resources.displayMetrics.heightPixels * 0.8).toInt())
            window?.setWindowAnimations(R.style.DialogAnimation)

            val tvTitle = dialogView.findViewById<android.widget.TextView>(R.id.tvTitle)
            val tvDescription = dialogView.findViewById<android.widget.TextView>(R.id.tvDescription)
            val tvDate = dialogView.findViewById<android.widget.TextView>(R.id.tvDate)
            val tvLocation = dialogView.findViewById<android.widget.TextView>(R.id.tvLocation)
            val tvEntries = dialogView.findViewById<android.widget.TextView>(R.id.tvEntries)
            val btnFavorite = dialogView.findViewById<android.widget.ImageButton>(R.id.btnFavorite)
            val btnEdit = dialogView.findViewById<android.widget.Button>(R.id.btnEdit)
            val btnDelete = dialogView.findViewById<android.widget.Button>(R.id.btnDelete)
            val btnClose = dialogView.findViewById<android.widget.Button>(R.id.btnClose)

            // Устанавливаем иконку избранного
            if (point.isFavorite) {
                btnFavorite.setImageResource(R.drawable.ic_favorite_filled)
            } else {
                btnFavorite.setImageResource(R.drawable.ic_favorite_border)
            }

            // Обработчик для избранного
            btnFavorite.setOnClickListener {
                val newFavoriteState = !point.isFavorite
                val success = memoryPointRepository.toggleFavorite(point.pointId, newFavoriteState)

                if (success) {
                    // СОЗДАЕМ НОВЫЙ ОБЪЕКТ с обновленным состоянием избранного
                    val updatedPoint = point.copy(isFavorite = newFavoriteState)

                    // Меняем иконку
                    if (newFavoriteState) {
                        btnFavorite.setImageResource(R.drawable.ic_favorite_filled)
                        showMessage("Добавлено в избранное")
                    } else {
                        btnFavorite.setImageResource(R.drawable.ic_favorite_border)
                        showMessage("Убрано из избранного")
                    }

                    // Обновляем данные в списке (если нужно)
                    // Здесь можно обновить локальный список если он используется
                } else {
                    showMessage("Ошибка при обновлении избранного")
                }
            }

            tvTitle.text = point.title
            tvDescription.text = point.description ?: "Нет описания"
            tvDate.text = "Дата: ${SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(point.visitDate))}"
            tvLocation.text = "Координаты: ${"%.6f".format(point.latitude)}, ${"%.6f".format(point.longitude)}"

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
                showEditMemoryDialog(point)
            }

            btnDelete.setOnClickListener {
                dialog.dismiss()
                showDeleteConfirmationDialog(point)
            }

            btnClose.setOnClickListener {
                dialog.dismiss()
            }

            dialog.setCancelable(true)
            dialog.show()
        }
    }

    private fun showEditMemoryDialog(memoryPoint: MemoryPoint) {
        val dialogView = layoutInflater.inflate(R.layout.bottom_sheet_add_memory, null)
        val dialog = Dialog(requireContext(), android.R.style.Theme_Material_Light_NoActionBar)
        dialog.setContentView(dialogView)

        val window = dialog.window
        window?.setGravity(android.view.Gravity.BOTTOM)
        window?.setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        window?.decorView?.setPadding(0, 0, 0, 0)
        window?.setBackgroundDrawableResource(android.R.color.white)

        val displayMetrics = resources.displayMetrics
        val height = (displayMetrics.heightPixels * 0.8).toInt()
        window?.setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, height)

        window?.setWindowAnimations(R.style.DialogAnimation)

        val tvDialogTitle = dialogView.findViewById<android.widget.TextView>(R.id.tvDialogTitle)
        val etTitle = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etTitle)
        val etDescription = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etDescription)
        val btnAddPhoto = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnAddPhoto)
        val btnAddVoice = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnAddVoice)
        val btnCancel = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancel)
        val btnSave = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSave)

        // ТАКОЕ ЖЕ ОТКЛЮЧЕНИЕ ПАНЕЛИ РЕДАКТИРОВАНИЯ
        etTitle.setRawInputType(android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)
        etTitle.imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_DONE or
                android.view.inputmethod.EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING or
                android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI or
                android.view.inputmethod.EditorInfo.IME_FLAG_NO_FULLSCREEN

        etDescription.setRawInputType(android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE or android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)
        etDescription.imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_DONE or
                android.view.inputmethod.EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING or
                android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI or
                android.view.inputmethod.EditorInfo.IME_FLAG_NO_FULLSCREEN

        // Убираем подсказки (исправленная версия)
        etTitle.inputType = etTitle.inputType or android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        etDescription.inputType = etDescription.inputType or android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS

        // Отключаем контекстное меню
        etTitle.customSelectionActionModeCallback = object : android.view.ActionMode.Callback {
            override fun onCreateActionMode(mode: android.view.ActionMode?, menu: android.view.Menu?) = false
            override fun onPrepareActionMode(mode: android.view.ActionMode?, menu: android.view.Menu?) = false
            override fun onActionItemClicked(mode: android.view.ActionMode?, item: android.view.MenuItem?) = false
            override fun onDestroyActionMode(mode: android.view.ActionMode?) {}
        }

        etDescription.customSelectionActionModeCallback = object : android.view.ActionMode.Callback {
            override fun onCreateActionMode(mode: android.view.ActionMode?, menu: android.view.Menu?) = false
            override fun onPrepareActionMode(mode: android.view.ActionMode?, menu: android.view.Menu?) = false
            override fun onActionItemClicked(mode: android.view.ActionMode?, item: android.view.MenuItem?) = false
            override fun onDestroyActionMode(mode: android.view.ActionMode?) {}
        }

        etDescription.isVerticalScrollBarEnabled = false
        etDescription.isHorizontalScrollBarEnabled = false

        tvDialogTitle.text = "Редактировать воспоминание"
        btnSave.text = "Обновить"

        btnAddPhoto.visibility = View.GONE
        btnAddVoice.visibility = View.GONE

        etTitle.setText(memoryPoint.title)
        etDescription.setText(memoryPoint.description ?: "")

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSave.setOnClickListener {
            val newTitle = etTitle.text.toString()
            val newDescription = etDescription.text.toString()

            if (newTitle.isBlank()) {
                showMessage("Введите название воспоминания")
                return@setOnClickListener
            }

            // ОБНОВЛЯЕМ ДАННЫЕ ТОЧКИ
            val updatedMemoryPoint = memoryPoint.copy(
                title = newTitle,
                description = newDescription,
                visitDate = System.currentTimeMillis() // Обновляем дату редактирования
            )

            // СОХРАНЯЕМ В БАЗУ ДАННЫХ
            val success = memoryPointRepository.updateMemoryPoint(updatedMemoryPoint)

            if (success) {
                // ОБНОВЛЯЕМ МАРКЕР НА КАРТЕ
                updateMemoryMarker(updatedMemoryPoint)
                showMessage("Воспоминание обновлено!")
                dialog.dismiss()
            } else {
                showMessage("Ошибка при обновлении воспоминания")
            }
        }

        dialog.setCanceledOnTouchOutside(true)
        dialog.show()
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        etTitle.requestFocus()
    }
    private fun updateMemoryMarker(updatedMemoryPoint: MemoryPoint) {
        // Находим старый маркер
        val oldMarker = memoryMarkers.find {
            it.getRelatedObject() as? Long == updatedMemoryPoint.pointId
        }

        oldMarker?.let { marker ->
            // Удаляем старый маркер
            mapView.overlays.remove(marker)
            memoryMarkers.remove(marker)

            // Создаем новый маркер с обновленными данными
            val newMarker = Marker(mapView).apply {
                position = GeoPoint(updatedMemoryPoint.latitude, updatedMemoryPoint.longitude)
                title = updatedMemoryPoint.title
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                setIcon(requireContext().getDrawable(android.R.drawable.ic_menu_myplaces))
                setRelatedObject(updatedMemoryPoint.pointId)
            }

            // Обработчик клика по обновленному маркеру
            newMarker.setOnMarkerClickListener { marker, _ ->
                val memoryPointId = marker.getRelatedObject() as? Long
                memoryPointId?.let { showMemoryDetails(it) }
                true
            }

            // Добавляем новый маркер
            mapView.overlays.add(newMarker)
            memoryMarkers.add(newMarker)
            mapView.invalidate()
        }
    }
    private fun showDeleteConfirmationDialog(memoryPoint: MemoryPoint) {
        AlertDialog.Builder(requireContext())
            .setTitle("Удаление воспоминания")
            .setMessage("Вы уверены, что хотите удалить воспоминание \"${memoryPoint.title}\"?")
            .setPositiveButton("Удалить") { _, _ ->
                deleteMemoryPoint(memoryPoint)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun deleteMemoryPoint(memoryPoint: MemoryPoint) {
        val success = memoryPointRepository.deleteMemoryPoint(memoryPoint.pointId)

        if (success) {
            // Удаляем маркер с карты
            removeMemoryMarker(memoryPoint.pointId)
            showMessage("Воспоминание \"${memoryPoint.title}\" удалено")
        } else {
            showMessage("Ошибка при удалении воспоминания")
        }
    }

    private fun removeMemoryMarker(pointId: Long) {
        val markerToRemove = memoryMarkers.find {
            it.getRelatedObject() as? Long == pointId
        }

        markerToRemove?.let { marker ->
            mapView.overlays.remove(marker)
            memoryMarkers.remove(marker)
            mapView.invalidate()
        }
    }

    private fun loadExistingMemories() {
        val memoryPoints = memoryPointRepository.getMemoryPoints(currentUserId)
        memoryPoints.forEach { point ->
            val geoPoint = GeoPoint(point.latitude, point.longitude)
            addMemoryMarker(geoPoint, point.title, point.pointId)
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImage.launch(intent)
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    private fun showMessage(message: String) {
        android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}