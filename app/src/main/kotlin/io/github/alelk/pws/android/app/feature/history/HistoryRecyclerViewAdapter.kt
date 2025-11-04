package io.github.alelk.pws.android.app.feature.history

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.github.alelk.pws.android.app.R
import io.github.alelk.pws.domain.core.ids.SongNumberId
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.ExperimentalTime

/**
 * History Recycler View Adapter
 *
 * Created by Alex Elkin on 23.05.2016.
 */
class HistoryRecyclerViewAdapter(private val onItemClickListener: (id: SongNumberId) -> Unit) :
  ListAdapter<HistoryInfo, HistoryRecyclerViewAdapter.HistoryViewHolder>(HistoryItemDiffCallback()) {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
    val view = LayoutInflater.from(parent.context)
      .inflate(R.layout.layout_history_list_item, parent, false)
    return HistoryViewHolder(view)
  }

  override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
    val historyItem = getItem(position)
    holder.bind(historyItem, onItemClickListener)
  }

  class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val songName: TextView = itemView.findViewById(R.id.txt_song_name)
    private val songNumber: TextView = itemView.findViewById(R.id.txt_song_number)
    private val bookDisplayName: TextView = itemView.findViewById(R.id.txt_book_name)
    private val timestamp: TextView = itemView.findViewById(R.id.txt_timestamp)

    @OptIn(ExperimentalTime::class)
    fun bind(historyItem: HistoryInfo, onItemClickListener: (id: SongNumberId) -> Unit) {
      songNumber.text = historyItem.songNumber.toString()
      songName.text = historyItem.songName
      bookDisplayName.text = historyItem.bookDisplayName
      timestamp.text = DateUtils.getRelativeTimeSpanString(
        historyItem.timestamp.toInstant(TimeZone.Companion.currentSystemDefault()).toEpochMilliseconds(),
        System.currentTimeMillis(),
        DateUtils.SECOND_IN_MILLIS
      )
      itemView.setOnClickListener { onItemClickListener(historyItem.songNumberId) }
    }
  }

  class HistoryItemDiffCallback : DiffUtil.ItemCallback<HistoryInfo>() {
    override fun areItemsTheSame(oldItem: HistoryInfo, newItem: HistoryInfo): Boolean {
      return oldItem.timestamp == newItem.timestamp
    }

    override fun areContentsTheSame(oldItem: HistoryInfo, newItem: HistoryInfo): Boolean {
      return oldItem == newItem
    }
  }
}