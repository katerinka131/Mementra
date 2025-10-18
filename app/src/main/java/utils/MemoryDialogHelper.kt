package com.example.mementra.utils

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import com.example.mementra.R
import com.example.mementra.database.models.MemoryEntry
import com.example.mementra.database.models.MemoryPoint
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.*

/**
 * Вспомогательный класс для работы с диалогами воспоминаний
 * Убирает дублирование кода между фрагментами
 */
object MemoryDialogHelper {

    /**
     * Показать диалог добавления воспоминания
     */
    fun showAddMemoryDialog(
        context: Context,
        onSave: (title: String, description: String) -> Unit,
        onCancel: () -> Unit,
        onAddPhoto: () -> Unit = {},
        onAddVoice: () -> Unit = {}
    ) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_add_memory, null)
        val dialog = createBottomSheetDialog(context, dialogView)

        val etTitle = dialogView.findViewById<TextInputEditText>(R.id.etTitle)
        val etDescription = dialogView.findViewById<TextInputEditText>(R.id.etDescription)
        val btnAddPhoto = dialogView.findViewById<MaterialButton>(R.id.btnAddPhoto)
        val btnAddVoice = dialogView.findViewById<MaterialButton>(R.id.btnAddVoice)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancel)
        val btnSave = dialogView.findViewById<MaterialButton>(R.id.btnSave)

        // Настройка полей ввода
        configureEditText(etTitle)
        configureEditText(etDescription)

        btnAddPhoto.setOnClickListener {
            onAddPhoto()
        }

        btnAddVoice.setOnClickListener {
            onAddVoice()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
            onCancel()
        }

        btnSave.setOnClickListener {
            val title = etTitle.text.toString()
            val description = etDescription.text.toString()
            onSave(title, description)
            dialog.dismiss()
        }

        dialog.setOnCancelListener {
            onCancel()
        }

        dialog.show()
        setupKeyboard(context, dialog, etTitle)
    }

    /**
     * Показать диалог редактирования воспоминания
     */
    fun showEditMemoryDialog(
        context: Context,
        memoryPoint: MemoryPoint,
        onSave: (title: String, description: String) -> Unit,
        onCancel: () -> Unit
    ) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_add_memory, null)
        val dialog = createBottomSheetDialog(context, dialogView)

        val tvDialogTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val etTitle = dialogView.findViewById<TextInputEditText>(R.id.etTitle)
        val etDescription = dialogView.findViewById<TextInputEditText>(R.id.etDescription)
        val btnAddPhoto = dialogView.findViewById<MaterialButton>(R.id.btnAddPhoto)
        val btnAddVoice = dialogView.findViewById<MaterialButton>(R.id.btnAddVoice)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancel)
        val btnSave = dialogView.findViewById<MaterialButton>(R.id.btnSave)

        // Настройка для режима редактирования
        tvDialogTitle.text = "Редактировать воспоминание"
        btnSave.text = "Обновить"
        btnAddPhoto.visibility = android.view.View.GONE
        btnAddVoice.visibility = android.view.View.GONE

        // Заполняем существующими данными
        etTitle.setText(memoryPoint.title)
        etDescription.setText(memoryPoint.description ?: "")

        // Настройка полей ввода
        configureEditText(etTitle)
        configureEditText(etDescription)

        btnCancel.setOnClickListener {
            dialog.dismiss()
            onCancel()
        }

        btnSave.setOnClickListener {
            val title = etTitle.text.toString()
            val description = etDescription.text.toString()
            onSave(title, description)
            dialog.dismiss()
        }

        dialog.show()
        setupKeyboard(context, dialog, etTitle)
    }

    /**
     * Показать диалог с деталями воспоминания
     */
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
        val btnFavorite = dialogView.findViewById<android.widget.ImageButton>(R.id.btnFavorite)
        val btnEdit = dialogView.findViewById<android.widget.Button>(R.id.btnEdit)
        val btnDelete = dialogView.findViewById<android.widget.Button>(R.id.btnDelete)
        val btnClose = dialogView.findViewById<android.widget.Button>(R.id.btnClose)

        // Устанавливаем данные
        tvTitle.text = memoryPoint.title
        tvDescription.text = memoryPoint.description ?: "Нет описания"
        tvDate.text = "Дата: ${SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(memoryPoint.visitDate))}"
        tvLocation.text = "Координаты: ${"%.6f".format(memoryPoint.latitude)}, ${"%.6f".format(memoryPoint.longitude)}"

        // Иконка избранного
        updateFavoriteIcon(btnFavorite, memoryPoint.isFavorite)

        // Записи
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

        // Обработчики
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

    /**
     * Показать диалог подтверждения удаления
     */
    fun showDeleteConfirmationDialog(
        context: Context,
        memoryTitle: String,
        onConfirm: () -> Unit
    ) {
        AlertDialog.Builder(context)
            .setTitle("Удаление воспоминания")
            .setMessage("Вы уверены, что хотите удалить воспоминание \"$memoryTitle\"?")
            .setPositiveButton("Удалить") { _, _ ->
                onConfirm()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    /**
     * Создать Bottom Sheet диалог
     */
    private fun createBottomSheetDialog(context: Context, dialogView: android.view.View): Dialog {
        val dialog = Dialog(context, android.R.style.Theme_Material_Light_NoActionBar)
        dialog.setContentView(dialogView)

        val window = dialog.window
        window?.setGravity(Gravity.BOTTOM)
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        window?.decorView?.setPadding(0, 0, 0, 0)
        window?.setBackgroundDrawableResource(android.R.color.white)

        val displayMetrics = context.resources.displayMetrics
        val height = (displayMetrics.heightPixels * 0.8).toInt()
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, height)

        window?.setWindowAnimations(R.style.DialogAnimation)

        dialog.setCanceledOnTouchOutside(true)

        return dialog
    }

    /**
     * Настроить EditText (отключить панель редактирования, подсказки и т.д.)
     */
    private fun configureEditText(editText: TextInputEditText) {
        editText.setRawInputType(
            android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        )
        editText.imeOptions = EditorInfo.IME_ACTION_DONE or
                EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING or
                EditorInfo.IME_FLAG_NO_EXTRACT_UI or
                EditorInfo.IME_FLAG_NO_FULLSCREEN

        // Убираем подсказки и автозамену
        editText.inputType = editText.inputType or android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS

        // Отключаем контекстное меню
        editText.customSelectionActionModeCallback = object : android.view.ActionMode.Callback {
            override fun onCreateActionMode(mode: android.view.ActionMode?, menu: android.view.Menu?) = false
            override fun onPrepareActionMode(mode: android.view.ActionMode?, menu: android.view.Menu?) = false
            override fun onActionItemClicked(mode: android.view.ActionMode?, item: android.view.MenuItem?) = false
            override fun onDestroyActionMode(mode: android.view.ActionMode?) {}
        }

        // Убираем полосы прокрутки
        editText.isVerticalScrollBarEnabled = false
        editText.isHorizontalScrollBarEnabled = false
    }

    /**
     * Настроить клавиатуру
     */
    private fun setupKeyboard(context: Context, dialog: Dialog, editText: TextInputEditText) {
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        editText.requestFocus()

        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
    }

    /**
     * Обновить иконку избранного
     */
    private fun updateFavoriteIcon(button: android.widget.ImageButton, isFavorite: Boolean) {
        if (isFavorite) {
            button.setImageResource(R.drawable.ic_favorite_filled)
        } else {
            button.setImageResource(R.drawable.ic_favorite_border)
        }
    }
}

