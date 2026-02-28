package com.example.mementra.utils

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.media.MediaPlayer
import android.net.Uri
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.mementra.R
import com.example.mementra.database.models.MemoryEntry
import com.example.mementra.database.models.MemoryPoint
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object MemoryDialogHelper {

    fun showAddMemoryDialog(
        context: Context,
        onSave: (title: String, description: String, emoji: String) -> Unit,
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

        configureEditText(etTitle)
        configureEditText(etDescription)

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
            onSave(title, description, selectedEmoji)
            dialog.dismiss()
        }

        dialog.setOnCancelListener { onCancel() }

        dialog.show()
        setupKeyboard(context, dialog, etTitle)
    }

    fun showEditMemoryDialog(
        context: Context,
        memoryPoint: MemoryPoint,
        onSave: (title: String, description: String, emoji: String) -> Unit,
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

        tvDialogTitle.text = "Редактировать воспоминание"
        btnSave.text = "Обновить"

        etTitle.setText(memoryPoint.title)
        etDescription.setText(memoryPoint.description ?: "")

        configureEditText(etTitle)
        configureEditText(etDescription)

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
            onSave(title, description, selectedEmoji)
            dialog.dismiss()
        }

        dialog.show()
        setupKeyboard(context, dialog, etTitle)
    }

    fun showMemoryDetailsDialog(
        context: Context,
        memoryPoint: MemoryPoint,
        entries: List<MemoryEntry>,
        onFavoriteToggle: (isFavorite: Boolean) -> Unit,
        onEdit: () -> Unit,
        onDelete: () -> Unit
    ) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_memory_details, null)
        val dialog = createBottomSheetDialog(context, dialogView)

        val tvTitle = dialogView.findViewById<TextView>(R.id.tvTitle)
        val tvDescription = dialogView.findViewById<TextView>(R.id.tvDescription)
        val tvDate = dialogView.findViewById<TextView>(R.id.tvDate)
        val tvLocation = dialogView.findViewById<TextView>(R.id.tvLocation)
        val tvEntries = dialogView.findViewById<TextView>(R.id.tvEntries)
        val mediaContainer = dialogView.findViewById<LinearLayout>(R.id.mediaContainer)
        val btnFavorite = dialogView.findViewById<android.widget.ImageButton>(R.id.btnFavorite)
        val btnEdit = dialogView.findViewById<android.widget.Button>(R.id.btnEdit)
        val btnDelete = dialogView.findViewById<android.widget.Button>(R.id.btnDelete)
        val btnClose = dialogView.findViewById<android.widget.Button>(R.id.btnClose)

        tvTitle.text = "${memoryPoint.emoji} ${memoryPoint.title}"
        tvDescription.text = memoryPoint.description ?: "Нет описания"
        tvDate.text = "Дата: ${SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(memoryPoint.visitDate))}"
        tvLocation.text = "Координаты: ${"%.6f".format(memoryPoint.latitude)}, ${"%.6f".format(memoryPoint.longitude)}"

        updateFavoriteIcon(btnFavorite, memoryPoint.isFavorite)

        val photos = entries.filter { it.type == MemoryEntry.TYPE_PHOTO && !it.filePath.isNullOrBlank() && File(it.filePath).exists() }
        val audios = entries.filter { it.type == MemoryEntry.TYPE_AUDIO && !it.filePath.isNullOrBlank() && File(it.filePath).exists() }
        val videos = entries.filter { it.type == MemoryEntry.TYPE_VIDEO && !it.filePath.isNullOrBlank() && File(it.filePath).exists() }
        val hasMedia = photos.isNotEmpty() || audios.isNotEmpty() || videos.isNotEmpty()

        if (photos.isNotEmpty()) {
            addMediaGroupButton(context, mediaContainer, photos, MemoryEntry.TYPE_PHOTO)
        }
        if (audios.isNotEmpty()) {
            addAudioGroupToContainer(context, mediaContainer, audios)
        }
        if (videos.isNotEmpty()) {
            addMediaGroupButton(context, mediaContainer, videos, MemoryEntry.TYPE_VIDEO)
        }

        tvEntries.visibility = View.GONE
        dialogView.findViewById<TextView>(R.id.tvEntriesLabel)?.visibility =
            if (hasMedia) View.VISIBLE else View.GONE

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

        dialog.show()
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

    private fun addMediaGroupButton(
        context: Context,
        container: LinearLayout?,
        entries: List<MemoryEntry>,
        type: String
    ) {
        container ?: return
        if (entries.isEmpty()) return

        val density = context.resources.displayMetrics.density
        val count = entries.size
        val (label, icon, mime) = when (type) {
            MemoryEntry.TYPE_PHOTO -> Triple(
                if (count == 1) "Просмотреть фото" else "Просмотреть фото ($count)",
                android.R.drawable.ic_menu_gallery,
                "image/*"
            )
            MemoryEntry.TYPE_VIDEO -> Triple(
                if (count == 1) "Воспроизвести видео" else "Воспроизвести видео ($count)",
                android.R.drawable.ic_media_play,
                "video/mp4"
            )
            else -> return
        }

        if (count == 1) {
            val file = File(entries.first().filePath!!)
            val btn = MaterialButton(context).apply {
                text = label
                setIconResource(icon)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = (8 * density).toInt() }
            }
            btn.setOnClickListener { openFileWithProvider(context, file, mime) }
            container.addView(btn)
        } else {
            val btn = MaterialButton(context).apply {
                text = label
                setIconResource(icon)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = (8 * density).toInt() }
            }
            btn.setOnClickListener {
                showMediaListDialog(context, entries, type, mime)
            }
            container.addView(btn)
        }
    }

    private fun showMediaListDialog(context: Context, entries: List<MemoryEntry>, type: String, mime: String) {
        val names = entries.mapIndexed { i, _ ->
            when (type) {
                MemoryEntry.TYPE_PHOTO -> "Фото ${i + 1}"
                MemoryEntry.TYPE_VIDEO -> "Видео ${i + 1}"
                else -> "Файл ${i + 1}"
            }
        }.toTypedArray()

        AlertDialog.Builder(context)
            .setTitle(if (type == MemoryEntry.TYPE_PHOTO) "Фото" else "Видео")
            .setItems(names) { _, which ->
                val file = File(entries[which].filePath!!)
                openFileWithProvider(context, file, mime)
            }
            .setNegativeButton("Закрыть", null)
            .show()
    }

    private fun addAudioGroupToContainer(context: Context, container: LinearLayout?, entries: List<MemoryEntry>) {
        container ?: return
        if (entries.isEmpty()) return

        val density = context.resources.displayMetrics.density
        val count = entries.size
        val label = if (count == 1) "Воспроизвести аудио" else "Воспроизвести аудио ($count)"

        val btn = MaterialButton(context).apply {
            text = label
            setIconResource(android.R.drawable.ic_media_play)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (8 * density).toInt() }
        }

        if (count == 1) {
            var mediaPlayer: MediaPlayer? = null
            val filePath = entries.first().filePath!!
            btn.setOnClickListener {
                if (mediaPlayer?.isPlaying == true) {
                    mediaPlayer?.stop()
                    mediaPlayer?.release()
                    mediaPlayer = null
                    btn.text = label
                } else {
                    try {
                        mediaPlayer = MediaPlayer().apply {
                            setDataSource(filePath)
                            prepare()
                            start()
                        }
                        btn.text = "Остановить"
                        mediaPlayer?.setOnCompletionListener {
                            btn.text = label
                            mediaPlayer?.release()
                            mediaPlayer = null
                        }
                    } catch (_: Exception) { }
                }
            }
        } else {
            btn.setOnClickListener {
                showAudioListDialog(context, entries)
            }
        }

        container.addView(btn)
    }

    private fun showAudioListDialog(context: Context, entries: List<MemoryEntry>) {
        val names = entries.mapIndexed { i, _ -> "Аудио ${i + 1}" }.toTypedArray()
        var currentPlayer: MediaPlayer? = null

        AlertDialog.Builder(context)
            .setTitle("Аудио")
            .setItems(names) { _, which ->
                currentPlayer?.let { if (it.isPlaying) it.stop(); it.release() }
                try {
                    currentPlayer = MediaPlayer().apply {
                        setDataSource(entries[which].filePath!!)
                        prepare()
                        start()
                    }
                } catch (_: Exception) { }
            }
            .setNegativeButton("Закрыть") { _, _ ->
                currentPlayer?.let { if (it.isPlaying) it.stop(); it.release() }
            }
            .show()
    }

    private fun openFileWithProvider(context: Context, file: File, mimeType: String) {
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = android.content.Intent.createChooser(intent, "Открыть с помощью...")
            context.startActivity(chooser)
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "Не удалось открыть файл", android.widget.Toast.LENGTH_SHORT).show()
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
