package com.example.countdown

import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

data class CountdownEvent(
    val title: String,
    val subtitle: String? = null,
    val date: String? = null,
    var targetTimeInMillis: Long = 0,
    val labels: List<String>? = null,
    val is_pinned: Boolean = false
) {
    fun calculateTime() {
        if (targetTimeInMillis == 0L && !date.isNullOrEmpty()) {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                targetTimeInMillis = sdf.parse(date)?.time ?: 0L
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

class CountdownAdapter(
    private val events: MutableList<CountdownEvent>,
    private val onDeleteClick: (Int) -> Unit
) :
    RecyclerView.Adapter<CountdownAdapter.CountdownViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CountdownViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_countdown_card, parent, false)
        return CountdownViewHolder(view, onDeleteClick)
    }

    override fun onBindViewHolder(holder: CountdownViewHolder, position: Int) {
        val event = events[position]
        holder.bind(event)
    }

    override fun getItemCount(): Int {
        return events.size
    }

    inner class CountdownViewHolder(itemView: View, private val onDeleteClick: (Int) -> Unit) :
        RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.cardTitleTextView)
        private val subtitleTextView: TextView = itemView.findViewById(R.id.cardSubtitleTextView)
        private val daysTextView: TextView = itemView.findViewById(R.id.daysTextView)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)

        fun bind(event: CountdownEvent) {
            titleTextView.text = event.title
            
            if (event.subtitle.isNullOrEmpty()) {
                subtitleTextView.visibility = View.GONE
            } else {
                subtitleTextView.text = event.subtitle
                subtitleTextView.visibility = View.VISIBLE
            }

            val diff = event.targetTimeInMillis - System.currentTimeMillis()
            val days = TimeUnit.MILLISECONDS.toDays(diff)

            if (days >= 0) {
                daysTextView.text = days.toString()
            } else {
                daysTextView.text = "0"
            }

            deleteButton.setOnClickListener {
                onDeleteClick(bindingAdapterPosition)
            }
        }
    }
}
