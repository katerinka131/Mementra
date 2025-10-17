package com.example.mementra

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class OnboardingAdapter(
    private val items: List<MotivationCard>
) : RecyclerView.Adapter<OnboardingAdapter.MotivationViewHolder>() {

    inner class MotivationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val icon: ImageView = itemView.findViewById(R.id.motivationIcon)
        private val title: TextView = itemView.findViewById(R.id.motivationTitle)
        private val description: TextView = itemView.findViewById(R.id.motivationDescription)

        fun bind(item: MotivationCard) {
            icon.setImageResource(item.iconRes)
            title.text = item.title
            description.text = item.description
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MotivationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_motivation_card, parent, false)
        return MotivationViewHolder(view)
    }

    override fun onBindViewHolder(holder: MotivationViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}