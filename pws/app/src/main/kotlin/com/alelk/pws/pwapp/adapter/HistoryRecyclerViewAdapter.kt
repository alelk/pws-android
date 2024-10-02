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
package com.alelk.pws.pwapp.adapter

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.alelk.pws.database.dao.HistoryItem
import com.alelk.pws.pwapp.R
import com.alelk.pws.pwapp.adapter.HistoryRecyclerViewAdapter.HistoryViewHolder

/**
 * History Recycler View Adapter
 *
 * Created by Alex Elkin on 23.05.2016.
 */
class HistoryRecyclerViewAdapter(private val onItemClickListener: (id: Long) -> Unit) :
  ListAdapter<HistoryItem, HistoryViewHolder>(HistoryItemDiffCallback()) {

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
    private val psalmName: TextView = itemView.findViewById(R.id.txt_psalm_name)
    private val psalmNumber: TextView = itemView.findViewById(R.id.txt_psalm_number)
    private val bookDisplayName: TextView = itemView.findViewById(R.id.txt_book_name)
    private val timestamp: TextView = itemView.findViewById(R.id.txt_timestamp)

    fun bind(historyItem: HistoryItem, onItemClickListener: (id: Long) -> Unit) {
      psalmNumber.text = historyItem.songNumber.toString()
      psalmName.text = historyItem.songName
      bookDisplayName.text = historyItem.bookDisplayName
      timestamp.text = DateUtils.getRelativeTimeSpanString(
        historyItem.timestamp.time, System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS
      )
      itemView.setOnClickListener { onItemClickListener(historyItem.songNumberId) }
    }
  }

  class HistoryItemDiffCallback : DiffUtil.ItemCallback<HistoryItem>() {
    override fun areItemsTheSame(oldItem: HistoryItem, newItem: HistoryItem): Boolean {
      return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: HistoryItem, newItem: HistoryItem): Boolean {
      return oldItem == newItem
    }
  }
}