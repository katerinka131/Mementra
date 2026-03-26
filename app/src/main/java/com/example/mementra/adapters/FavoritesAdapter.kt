package com.example.mementra.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mementra.R
import com.example.mementra.database.models.MemoryPoint
import java.text.SimpleDateFormat
import java.util.*

class FavoritesAdapter(
    private var memories: List<MemoryPoint>,
    private val onMemoryClick: (MemoryPoint) -> Unit
) : RecyclerView.Adapter<FavoritesAdapter.FavoriteViewHolder>() {

    inner class FavoriteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tvFavoriteTitle)
        private val tvDate: TextView = itemView.findViewById(R.id.tvFavoriteDate)
        private val tvTags: TextView = itemView.findViewById(R.id.tvFavoriteTags)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvFavoriteDescription)

        fun bind(memory: MemoryPoint) {
            tvTitle.text = memory.title
            tvDate.text = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(memory.visitDate))

            if (memory.tags.isNotEmpty()) {
                tvTags.text = memory.tags.joinToString(" · ") { it.name }
                tvTags.visibility = View.VISIBLE
            } else {
                tvTags.visibility = View.GONE
            }

            memory.description?.let { description ->
                if (description.isNotBlank()) {
                    tvDescription.text = description
                    tvDescription.visibility = View.VISIBLE
                } else {
                    tvDescription.visibility = View.GONE
                }
            } ?: run {
                tvDescription.visibility = View.GONE
            }

            itemView.setOnClickListener {
                onMemoryClick(memory)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_favorite_memory, parent, false)
        return FavoriteViewHolder(view)
    }

    override fun onBindViewHolder(holder: FavoriteViewHolder, position: Int) {
        holder.bind(memories[position])
    }

    override fun getItemCount(): Int = memories.size

    fun updateMemories(newMemories: List<MemoryPoint>) {
        memories = newMemories
        notifyDataSetChanged()
    }
}