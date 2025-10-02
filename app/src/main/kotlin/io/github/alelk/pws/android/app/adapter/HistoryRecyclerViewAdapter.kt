/*
 * Copyright (C) 2018 The P&W Songs Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.alelk.pws.android.app.adapter

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.github.alelk.pws.android.app.R
import io.github.alelk.pws.android.app.adapter.HistoryRecyclerViewAdapter.HistoryViewHolder
import io.github.alelk.pws.android.app.model.HistoryInfo
import io.github.alelk.pws.domain.model.SongNumberId
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.ExperimentalTime

/**
 * History Recycler View Adapter
 *
 * Created by Alex Elkin on 23.05.2016.
 */
class HistoryRecyclerViewAdapter(private val onItemClickListener: (id: SongNumberId) -> Unit) :
  ListAdapter<HistoryInfo, HistoryViewHolder>(HistoryItemDiffCallback()) {

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
        historyItem.timestamp.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds(),
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