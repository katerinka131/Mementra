package com.example.mementra.utils

import android.app.AlertDialog
import android.content.Context
import android.graphics.Rect
import android.media.MediaPlayer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mementra.R
import com.example.mementra.database.models.MemoryEntry
import com.google.android.material.button.MaterialButton
import java.io.File

object MediaGridHelper {

    private var fullscreenPlayer: MediaPlayer? = null

    fun displayMedia(
        context: Context,
        container: LinearLayout,
        photos: List<MemoryEntry>,
        videos: List<MemoryEntry>,
        audios: List<MemoryEntry>
    ) {
        container.removeAllViews()

        // Фото
        if (photos.isNotEmpty()) {
            addSectionHeader(context, container, "Фото:")

            val photosContainer = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val showAllPhotos = photos.size > 6
            val photosToShow = if (showAllPhotos) photos.take(6) else photos

            addPhotoGrid(context, photosContainer, photosToShow, photos)

            if (showAllPhotos) {
                addShowAllButton(context, photosContainer, photos, "photo")
            }

            container.addView(photosContainer)
        }

        // Голосовые сообщения
        if (audios.isNotEmpty()) {
            addSectionHeader(context, container, "Голосовые сообщения:")
            val audioContainer = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            addAudioList(context, audioContainer, audios)
            container.addView(audioContainer)
        }

        // Видео
        if (videos.isNotEmpty()) {
            addSectionHeader(context, container, "Видео:")
            val videosContainer = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val showAllVideos = videos.size > 6
            val videosToShow = if (showAllVideos) videos.take(6) else videos

            addVideoGrid(context, videosContainer, videosToShow, videos)

            if (showAllVideos) {
                addShowAllButton(context, videosContainer, videos, "video")
            }

            container.addView(videosContainer)
        }
    }

    private fun addSectionHeader(context: Context, container: LinearLayout, title: String) {
        val density = context.resources.displayMetrics.density
        val header = TextView(context).apply {
            text = title
            textSize = 16f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding((16 * density).toInt(), (16 * density).toInt(), (16 * density).toInt(), (8 * density).toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        container.addView(header)
    }

    private fun addPhotoGrid(
        context: Context,
        container: LinearLayout,
        photos: List<MemoryEntry>,
        allPhotos: List<MemoryEntry>
    ) {
        val density = context.resources.displayMetrics.density
        val screenWidth = context.resources.displayMetrics.widthPixels

        // Получаем отступы контейнера (если есть)
        val containerPadding = if (container.parent is View) {
            val parent = container.parent as View
            parent.paddingLeft + parent.paddingRight
        } else {
            0
        }

        // Отступы внутри диалога (16dp с каждой стороны)
        val dialogPadding = (32 * density).toInt() // 16dp слева + 16dp справа = 32dp
        val spacing = (4 * density).toInt() // Отступ между фото

        // Доступная ширина = ширина экрана - отступы диалога
        val availableWidth = screenWidth - dialogPadding
        val itemSize = (availableWidth - (spacing * 2)) / 3

        val gridContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            // Добавляем отступы слева и справа, чтобы скомпенсировать отступы диалога
            setPadding(0, 0, 0, 0)
        }

        var row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        for ((index, photo) in photos.withIndex()) {
            val imageView = ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(itemSize, itemSize).apply {
                    if (index % 3 == 0) {
                        // Первый в ряду
                        setMargins(0, spacing, spacing / 2, spacing)
                    } else if ((index + 1) % 3 == 0) {
                        // Последний в ряду
                        setMargins(spacing / 2, spacing, 0, spacing)
                    } else {
                        // Средний
                        setMargins(spacing / 2, spacing, spacing / 2, spacing)
                    }
                }
                scaleType = ImageView.ScaleType.CENTER_CROP

                val file = File(photo.filePath!!)
                if (file.exists() && file.length() > 0) {
                    Glide.with(context)
                        .load(file)
                        .centerCrop()
                        .into(this)
                }

                setOnClickListener {
                    openFullscreenPhoto(context, allPhotos, allPhotos.indexOf(photo))
                }
            }

            row.addView(imageView)

            if ((index + 1) % 3 == 0 || index == photos.size - 1) {
                gridContainer.addView(row)
                if (index != photos.size - 1) {
                    row = LinearLayout(context).apply {
                        orientation = LinearLayout.HORIZONTAL
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                    }
                }
            }
        }

        container.addView(gridContainer)
    }

    private fun addVideoGrid(
        context: Context,
        container: LinearLayout,
        videos: List<MemoryEntry>,
        allVideos: List<MemoryEntry>
    ) {
        val density = context.resources.displayMetrics.density
        val screenWidth = context.resources.displayMetrics.widthPixels
        val spacing = (4 * density).toInt()
        val itemSize = (screenWidth - (spacing * 2)) / 3

        val gridContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        var row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        for ((index, video) in videos.withIndex()) {
            val videoContainer = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(itemSize, itemSize).apply {
                    if (index % 3 == 0) {
                        setMargins(0, spacing, spacing / 2, spacing)
                    } else if ((index + 1) % 3 == 0) {
                        setMargins(spacing / 2, spacing, 0, spacing)
                    } else {
                        setMargins(spacing / 2, spacing, spacing / 2, spacing)
                    }
                }
                gravity = android.view.Gravity.CENTER
            }

            val thumbnail = ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(itemSize, itemSize).apply {
                    height = itemSize - (40 * density).toInt()
                }
                scaleType = ImageView.ScaleType.CENTER_CROP

                val file = File(video.filePath!!)
                if (file.exists() && file.length() > 0) {
                    Glide.with(context)
                        .load(file)
                        .centerCrop()
                        .into(this)
                }
            }

            val playIcon = TextView(context).apply {
                text = "▶"
                textSize = 24f
                gravity = android.view.Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            videoContainer.addView(thumbnail)
            videoContainer.addView(playIcon)

            videoContainer.setOnClickListener {
                openVideoWithPlayer(context, video.filePath!!)
            }

            row.addView(videoContainer)

            if ((index + 1) % 3 == 0 || index == videos.size - 1) {
                gridContainer.addView(row)
                if (index != videos.size - 1) {
                    row = LinearLayout(context).apply {
                        orientation = LinearLayout.HORIZONTAL
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                    }
                }
            }
        }

        container.addView(gridContainer)
    }

    private fun addAudioList(context: Context, container: LinearLayout, audios: List<MemoryEntry>) {
        val density = context.resources.displayMetrics.density

        for ((index, audio) in audios.withIndex()) {
            val audioItem = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = (8 * density).toInt()
                    leftMargin = (16 * density).toInt()
                    rightMargin = (16 * density).toInt()
                }
                setPadding((16 * density).toInt(), (12 * density).toInt(), (16 * density).toInt(), (12 * density).toInt())
                setBackgroundResource(R.drawable.audio_item_background)
            }

            val playButton = TextView(context).apply {
                text = "▶"
                textSize = 18f
                setPadding((8 * density).toInt(), 0, (16 * density).toInt(), 0)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val audioName = TextView(context).apply {
                text = "Аудио ${index + 1}"
                textSize = 14f
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }

            var currentPlayer: MediaPlayer? = null

            playButton.setOnClickListener {
                try {
                    if (currentPlayer?.isPlaying == true) {
                        currentPlayer?.stop()
                        currentPlayer?.release()
                        currentPlayer = null
                        playButton.text = "▶"
                    } else {
                        fullscreenPlayer?.let {
                            if (it.isPlaying) {
                                it.stop()
                                it.release()
                                fullscreenPlayer = null
                            }
                        }

                        currentPlayer = MediaPlayer().apply {
                            setDataSource(audio.filePath!!)
                            prepare()
                            start()
                            playButton.text = "⏸"
                            setOnCompletionListener {
                                playButton.text = "▶"
                                release()
                                currentPlayer = null
                            }
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Ошибка воспроизведения", Toast.LENGTH_SHORT).show()
                }
            }

            audioItem.addView(playButton)
            audioItem.addView(audioName)
            container.addView(audioItem)
        }
    }

    private fun addShowAllButton(context: Context, container: LinearLayout, allItems: List<MemoryEntry>, type: String) {
        val density = context.resources.displayMetrics.density
        val button = MaterialButton(context).apply {
            text = if (type == "photo") "Просмотреть все фото" else "Просмотреть все видео"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (8 * density).toInt()
                bottomMargin = (16 * density).toInt()
                leftMargin = (16 * density).toInt()
                rightMargin = (16 * density).toInt()
            }
        }

        button.setOnClickListener {
            showAllMediaDialog(context, allItems, type)
        }

        container.addView(button)
    }

    private fun showAllMediaDialog(context: Context, items: List<MemoryEntry>, type: String) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_all_media, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recyclerView)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvTitle)

        tvTitle.text = if (type == "photo") "Все фото" else "Все видео"

        val density = context.resources.displayMetrics.density
        val screenWidth = context.resources.displayMetrics.widthPixels

        // Отступы внутри диалога (как в основном диалоге)
        val dialogPadding = (32 * density).toInt() // 16dp слева + 16dp справа
        val spacing = (4 * density).toInt() // Отступ между фото

        // Доступная ширина = ширина экрана - отступы диалога
        val availableWidth = screenWidth - dialogPadding
        val itemSize = (availableWidth - (spacing * 2)) / 3

        // Убираем внутренние отступы у RecyclerView
        recyclerView.setPadding(0, 0, 0, 0)
        recyclerView.clipToPadding = false

        recyclerView.layoutManager = GridLayoutManager(context, 3)
        recyclerView.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                val position = parent.getChildAdapterPosition(view)
                val column = position % 3

                outRect.top = spacing
                outRect.bottom = spacing

                when (column) {
                    0 -> {
                        // Первый в ряду - нет левого отступа
                        outRect.left = 0
                        outRect.right = spacing / 2
                    }
                    2 -> {
                        // Последний в ряду - нет правого отступа
                        outRect.left = spacing / 2
                        outRect.right = 0
                    }
                    else -> {
                        // Средний - отступы с обеих сторон
                        outRect.left = spacing / 2
                        outRect.right = spacing / 2
                    }
                }
            }
        })

        recyclerView.adapter = AllMediaAdapter(context, items, type, itemSize)

        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setPositiveButton("Закрыть", null)
            .create()

        dialog.show()

        // Устанавливаем размер диалога
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            (context.resources.displayMetrics.heightPixels * 0.8).toInt()
        )
    }

    private fun openFullscreenPhoto(context: Context, photos: List<MemoryEntry>, startIndex: Int) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_fullscreen_photo, null)
        val imageView = dialogView.findViewById<ImageView>(R.id.photoView)
        val prevButton = dialogView.findViewById<View>(R.id.btnPrev)
        val nextButton = dialogView.findViewById<View>(R.id.btnNext)
        val counterText = dialogView.findViewById<TextView>(R.id.tvCounter)

        var currentIndex = startIndex

        fun updatePhoto() {
            val file = File(photos[currentIndex].filePath!!)
            if (file.exists() && file.length() > 0) {
                Glide.with(context)
                    .load(file)
                    .into(imageView)
            }
            counterText.text = "${currentIndex + 1} / ${photos.size}"

            prevButton.isEnabled = currentIndex > 0
            nextButton.isEnabled = currentIndex < photos.size - 1
        }

        prevButton.setOnClickListener {
            if (currentIndex > 0) {
                currentIndex--
                updatePhoto()
            }
        }

        nextButton.setOnClickListener {
            if (currentIndex < photos.size - 1) {
                currentIndex++
                updatePhoto()
            }
        }

        updatePhoto()

        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setPositiveButton("Закрыть", null)
            .create()

        dialog.show()
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    private fun openVideoWithPlayer(context: Context, filePath: String) {
        try {
            val file = File(filePath)
            if (!file.exists() || file.length() == 0L) {
                Toast.makeText(context, "Видеофайл не найден или поврежден", Toast.LENGTH_SHORT).show()
                return
            }

            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "video/mp4")
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(android.content.Intent.createChooser(intent, "Открыть видео"))
        } catch (e: Exception) {
            Toast.makeText(context, "Не удалось открыть видео: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private class AllMediaAdapter(
        private val context: Context,
        private val items: List<MemoryEntry>,
        private val type: String,
        private val itemSize: Int
    ) : RecyclerView.Adapter<AllMediaAdapter.ViewHolder>() {

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val imageView: ImageView = itemView.findViewById(R.id.imageView)
            val playIcon: TextView? = itemView.findViewById(R.id.playIcon)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val layout = if (type == "photo") R.layout.item_media_grid else R.layout.item_video_grid
            val view = LayoutInflater.from(context).inflate(layout, parent, false)
            view.layoutParams = ViewGroup.LayoutParams(itemSize, itemSize)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            val file = File(item.filePath!!)

            if (file.exists() && file.length() > 0) {
                Glide.with(context)
                    .load(file)
                    .centerCrop()
                    .into(holder.imageView)
            }

            holder.itemView.setOnClickListener {
                if (type == "photo") {
                    openFullscreenPhoto(context, items, position)
                } else {
                    openVideoWithPlayer(context, item.filePath!!)
                }
            }
        }

        override fun getItemCount() = items.size
    }
}