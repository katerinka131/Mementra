package com.example.mementra.utils

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.mementra.R
import com.example.mementra.database.models.MemoryEntry
import com.example.mementra.database.models.MemoryPoint
import com.example.mementra.database.models.MemoryTag
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object MemoryDialogHelper {

    fun showAddMemoryDialog(
        context: Context,
        availableTags: List<MemoryTag> = emptyList(),
        initialSelectedTags: List<MemoryTag> = emptyList(),
        onRequestNewTag: (String, (MemoryTag?) -> Unit) -> Unit = { _, done -> done(null) },
        onSave: (title: String, description: String, emoji: String, selectedTags: List<MemoryTag>) -> Unit,
        onCancel: () -> Unit,
        onAddPhoto: () -> Unit = {},
        onAddVoice: () -> Unit = {},
        onAddVideo: () -> Unit = {}
    ) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_add_memory, null)
        val dialog = createBottomSheetDialog(context, dialogView)

        val etTitle = dialogView.findViewById<TextInputEditText>(R.id.etTitle)
        val etDescription = dialogView.findViewById<TextInputEditText>(R.id.etDescription)
        val btnAddPhoto = dialogView.findViewById<MaterialButton>(R.id.btnAddPhoto)
        val btnAddVoice = dialogView.findViewById<MaterialButton>(R.id.btnAddVoice)
        val btnAddVideo = dialogView.findViewById<MaterialButton>(R.id.btnAddVideo)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancel)
        val btnSave = dialogView.findViewById<MaterialButton>(R.id.btnSave)
        val emojiContainer = dialogView.findViewById<LinearLayout>(R.id.emojiContainer)
        val tagChipGroup = dialogView.findViewById<ChipGroup>(R.id.tagChipGroup)
        val btnAddTag = dialogView.findViewById<MaterialButton>(R.id.btnAddTag)

        configureEditText(etTitle)
        configureEditText(etDescription)

        val tagList = availableTags.toMutableList().apply {
            sortBy { it.name.lowercase(Locale.getDefault()) }
        }
        val getSelectedTags = setupMemoryTagChips(
            context, tagChipGroup, btnAddTag, tagList,
            initialSelectedTags.map { it.tagId }.toSet(),
            onRequestNewTag
        )

        var selectedEmoji = MemoryPoint.DEFAULT_EMOJI
        setupEmojiPicker(context, emojiContainer, selectedEmoji) { emoji ->
            selectedEmoji = emoji
        }

        btnAddPhoto.setOnClickListener { onAddPhoto() }
        btnAddVoice.setOnClickListener { onAddVoice() }
        btnAddVideo.setOnClickListener { onAddVideo() }

        btnCancel.setOnClickListener {
            dialog.dismiss()
            onCancel()
        }

        btnSave.setOnClickListener {
            val title = etTitle.text.toString()
            val description = etDescription.text.toString()
            onSave(title, description, selectedEmoji, getSelectedTags())
            dialog.dismiss()
        }

        dialog.setOnCancelListener { onCancel() }

        dialog.show()
        setupKeyboard(context, dialog, etTitle)
    }

    fun showEditMemoryDialog(
        context: Context,
        memoryPoint: MemoryPoint,
        availableTags: List<MemoryTag>,
        onRequestNewTag: (String, (MemoryTag?) -> Unit) -> Unit,
        onSave: (title: String, description: String, emoji: String, selectedTags: List<MemoryTag>) -> Unit,
        onCancel: () -> Unit,
        onAddPhoto: () -> Unit = {},
        onAddVoice: () -> Unit = {},
        onAddVideo: () -> Unit = {}
    ) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_add_memory, null)
        val dialog = createBottomSheetDialog(context, dialogView)

        val tvDialogTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val etTitle = dialogView.findViewById<TextInputEditText>(R.id.etTitle)
        val etDescription = dialogView.findViewById<TextInputEditText>(R.id.etDescription)
        val btnAddPhoto = dialogView.findViewById<MaterialButton>(R.id.btnAddPhoto)
        val btnAddVoice = dialogView.findViewById<MaterialButton>(R.id.btnAddVoice)
        val btnAddVideo = dialogView.findViewById<MaterialButton>(R.id.btnAddVideo)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancel)
        val btnSave = dialogView.findViewById<MaterialButton>(R.id.btnSave)
        val emojiContainer = dialogView.findViewById<LinearLayout>(R.id.emojiContainer)
        val tagChipGroup = dialogView.findViewById<ChipGroup>(R.id.tagChipGroup)
        val btnAddTag = dialogView.findViewById<MaterialButton>(R.id.btnAddTag)

        tvDialogTitle.text = "Редактировать воспоминание"
        btnSave.text = "Обновить"

        etTitle.setText(memoryPoint.title)
        etDescription.setText(memoryPoint.description ?: "")

        configureEditText(etTitle)
        configureEditText(etDescription)

        val merged = (availableTags + memoryPoint.tags).distinctBy { it.tagId }.toMutableList()
        merged.sortBy { it.name.lowercase(Locale.getDefault()) }
        val getSelectedTags = setupMemoryTagChips(
            context, tagChipGroup, btnAddTag, merged,
            memoryPoint.tags.map { it.tagId }.toSet(), onRequestNewTag
        )

        var selectedEmoji = memoryPoint.emoji
        setupEmojiPicker(context, emojiContainer, selectedEmoji) { emoji ->
            selectedEmoji = emoji
        }

        btnAddPhoto.setOnClickListener { onAddPhoto() }
        btnAddVoice.setOnClickListener { onAddVoice() }
        btnAddVideo.setOnClickListener { onAddVideo() }

        btnCancel.setOnClickListener {
            dialog.dismiss()
            onCancel()
        }

        btnSave.setOnClickListener {
            val title = etTitle.text.toString()
            val description = etDescription.text.toString()
            onSave(title, description, selectedEmoji, getSelectedTags())
            dialog.dismiss()
        }

        dialog.show()
        setupKeyboard(context, dialog, etTitle)
    }

    private fun setupMemoryTagChips(
        context: Context,
        chipGroup: ChipGroup,
        btnAddTag: MaterialButton,
        tagList: MutableList<MemoryTag>,
        initialSelectedIds: Set<Long>,
        onRequestNewTag: (String, (MemoryTag?) -> Unit) -> Unit
    ): () -> List<MemoryTag> {
        val selectedIds = initialSelectedIds.toMutableSet()

        fun rebuildChips() {
            chipGroup.removeAllViews()
            for (tag in tagList) {
                val chip = Chip(context).apply {
                    text = tag.name
                    isCheckable = true
                    isChecked = tag.tagId in selectedIds
                    try {
                        val c = Color.parseColor(tag.colorHex)
                        chipStrokeWidth = resources.displayMetrics.density * 2f
                        chipStrokeColor = ColorStateList.valueOf(c)
                        setTextColor(c)
                    } catch (_: Exception) { }
                }
                chip.setOnCheckedChangeListener { _, checked ->
                    if (checked) selectedIds.add(tag.tagId) else selectedIds.remove(tag.tagId)
                }
                chipGroup.addView(chip)
            }
        }

        rebuildChips()

        btnAddTag.setOnClickListener {
            val input = EditText(context).apply {
                hint = "Название тега"
                val pad = (24 * resources.displayMetrics.density).toInt()
                setPadding(pad, pad, pad, pad)
            }
            AlertDialog.Builder(context)
                .setTitle("Новый тег")
                .setView(input)
                .setPositiveButton("Создать") { _, _ ->
                    val name = input.text?.toString()?.trim().orEmpty()
                    if (name.isEmpty()) return@setPositiveButton
                    onRequestNewTag(name) { newTag ->
                        if (newTag != null) {
                            if (tagList.none { it.tagId == newTag.tagId }) {
                                tagList.add(newTag)
                                tagList.sortBy { it.name.lowercase(Locale.getDefault()) }
                            }
                            selectedIds.add(newTag.tagId)
                            rebuildChips()
                        }
                    }
                }
                .setNegativeButton("Отмена", null)
                .show()
        }

        return { tagList.filter { it.tagId in selectedIds } }
    }

    fun showMemoryDetailsDialog(
        context: Context,
        memoryPoint: MemoryPoint,
        entries: List<MemoryEntry>,
        onFavoriteToggle: (isFavorite: Boolean) -> Unit,
        onEdit: () -> Unit,
        onDelete: () -> Unit,
        onMediaDelete: ((MemoryEntry) -> Unit)? = null
    ): android.app.Dialog {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_memory_details, null)
        val dialog = createBottomSheetDialog(context, dialogView)

        val tvTitle = dialogView.findViewById<TextView>(R.id.tvTitle)
        val tvDescription = dialogView.findViewById<TextView>(R.id.tvDescription)
        val tvDate = dialogView.findViewById<TextView>(R.id.tvDate)
        val tvLocation = dialogView.findViewById<TextView>(R.id.tvLocation)
        val tvMemoryTags = dialogView.findViewById<TextView>(R.id.tvMemoryTags)
        val mediaContainer = dialogView.findViewById<LinearLayout>(R.id.mediaContainer)
        val btnFavorite = dialogView.findViewById<android.widget.ImageButton>(R.id.btnFavorite)
        val btnEdit = dialogView.findViewById<android.widget.Button>(R.id.btnEdit)
        val btnDelete = dialogView.findViewById<android.widget.Button>(R.id.btnDelete)
        val btnClose = dialogView.findViewById<android.widget.Button>(R.id.btnClose)

        tvTitle.text = "${memoryPoint.emoji} ${memoryPoint.title}"
        tvDescription.text = memoryPoint.description ?: "Нет описания"
        tvDate.text = "Дата: ${SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(memoryPoint.visitDate))}"
        tvLocation.text = "Координаты: ${"%.6f".format(memoryPoint.latitude)}, ${"%.6f".format(memoryPoint.longitude)}"

        if (memoryPoint.tags.isEmpty()) {
            tvMemoryTags.visibility = View.GONE
        } else {
            tvMemoryTags.visibility = View.VISIBLE
            tvMemoryTags.text = "Теги: ${memoryPoint.tags.joinToString(", ") { it.name }}"
        }

        updateFavoriteIcon(btnFavorite, memoryPoint.isFavorite)

        // Фильтруем медиа
        val photos = entries.filter {
            it.type == MemoryEntry.TYPE_PHOTO &&
                    !it.filePath.isNullOrBlank() &&
                    File(it.filePath).exists() &&
                    File(it.filePath).length() > 0
        }
        val audios = entries.filter {
            it.type == MemoryEntry.TYPE_AUDIO &&
                    !it.filePath.isNullOrBlank() &&
                    File(it.filePath).exists() &&
                    File(it.filePath).length() > 0
        }
        val videos = entries.filter {
            it.type == MemoryEntry.TYPE_VIDEO &&
                    !it.filePath.isNullOrBlank() &&
                    File(it.filePath).exists() &&
                    File(it.filePath).length() > 0
        }

        // Устанавливаем слушатель удаления для MediaGridHelper
        MediaGridHelper.setOnMediaDeleteListener(onMediaDelete)

        // Используем MediaGridHelper для красивого отображения
        MediaGridHelper.displayMedia(context, mediaContainer, photos, videos, audios)

        btnFavorite.setOnClickListener {
            val newState = !memoryPoint.isFavorite
            updateFavoriteIcon(btnFavorite, newState)
            onFavoriteToggle(newState)
        }

        btnEdit.setOnClickListener {
            dialog.dismiss()
            onEdit()
        }

        btnDelete.setOnClickListener {
            dialog.dismiss()
            onDelete()
        }

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.setOnDismissListener {
            // Очищаем слушатель при закрытии диалога
            MediaGridHelper.setOnMediaDeleteListener(null)
        }

        dialog.show()

        return dialog
    }

    fun showDeleteConfirmationDialog(
        context: Context,
        memoryTitle: String,
        onConfirm: () -> Unit
    ) {
        AlertDialog.Builder(context)
            .setTitle("Удаление воспоминания")
            .setMessage("Вы уверены, что хотите удалить воспоминание \"$memoryTitle\"?")
            .setPositiveButton("Удалить") { _, _ -> onConfirm() }
            .setNegativeButton("Отмена", null)
            .show()
    }

    fun createEmojiBitmap(context: Context, emoji: String, sizeDp: Int = 40): Bitmap {
        val density = context.resources.displayMetrics.density
        val sizePx = (sizeDp * density).toInt()
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = sizePx * 0.7f
            textAlign = Paint.Align.CENTER
        }
        val x = sizePx / 2f
        val y = sizePx / 2f - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText(emoji, x, y, paint)
        return bitmap
    }

    private fun setupEmojiPicker(
        context: Context,
        container: LinearLayout,
        currentEmoji: String,
        onEmojiSelected: (String) -> Unit
    ) {
        val density = context.resources.displayMetrics.density
        val buttons = mutableListOf<TextView>()

        for (emoji in MemoryPoint.EMOJI_OPTIONS) {
            val tv = TextView(context).apply {
                text = emoji
                textSize = 24f
                gravity = android.view.Gravity.CENTER
                val size = (44 * density).toInt()
                val margin = (4 * density).toInt()
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    setMargins(margin, 0, margin, 0)
                }
                setBackgroundResource(
                    if (emoji == currentEmoji) R.drawable.emoji_selected_bg
                    else android.R.color.transparent
                )
                setOnClickListener {
                    onEmojiSelected(emoji)
                    buttons.forEach { btn ->
                        btn.setBackgroundResource(android.R.color.transparent)
                    }
                    setBackgroundResource(R.drawable.emoji_selected_bg)
                }
            }
            buttons.add(tv)
            container.addView(tv)
        }
    }

    private fun createBottomSheetDialog(context: Context, dialogView: View): Dialog {
        val dialog = Dialog(context, R.style.BottomSheetDialogTheme)
        dialog.setContentView(dialogView)

        val window = dialog.window
        window?.setGravity(Gravity.BOTTOM)
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        window?.decorView?.setPadding(0, 0, 0, 0)

        val displayMetrics = context.resources.displayMetrics
        val height = (displayMetrics.heightPixels * 0.85).toInt()
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, height)

        window?.setWindowAnimations(R.style.DialogAnimation)

        dialog.setCanceledOnTouchOutside(true)

        return dialog
    }

    private fun configureEditText(editText: TextInputEditText) {
        editText.setRawInputType(
            android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        )
        editText.imeOptions = EditorInfo.IME_ACTION_DONE or
                EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING or
                EditorInfo.IME_FLAG_NO_EXTRACT_UI or
                EditorInfo.IME_FLAG_NO_FULLSCREEN

        editText.inputType = editText.inputType or android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS

        editText.customSelectionActionModeCallback = object : android.view.ActionMode.Callback {
            override fun onCreateActionMode(mode: android.view.ActionMode?, menu: android.view.Menu?) = false
            override fun onPrepareActionMode(mode: android.view.ActionMode?, menu: android.view.Menu?) = false
            override fun onActionItemClicked(mode: android.view.ActionMode?, item: android.view.MenuItem?) = false
            override fun onDestroyActionMode(mode: android.view.ActionMode?) {}
        }

        editText.isVerticalScrollBarEnabled = false
        editText.isHorizontalScrollBarEnabled = false
    }

    private fun setupKeyboard(context: Context, dialog: Dialog, editText: TextInputEditText) {
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        editText.requestFocus()

        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun updateFavoriteIcon(button: android.widget.ImageButton, isFavorite: Boolean) {
        if (isFavorite) {
            button.setImageResource(R.drawable.ic_favorite_filled)
        } else {
            button.setImageResource(R.drawable.ic_favorite_border)
        }
    }
}