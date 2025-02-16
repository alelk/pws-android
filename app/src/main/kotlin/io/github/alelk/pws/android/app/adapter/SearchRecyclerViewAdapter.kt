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

import android.os.Build
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.github.alelk.pws.android.app.R
import io.github.alelk.pws.database.entity.SongSearchResultEntity
import io.github.alelk.pws.domain.model.SongNumberId

/**
 * Search Recycler View Adapter
 *
 * Created by Alex Elkin on 23.05.2016.
 */

class SearchRecyclerViewAdapter(
  private val clickListener: (songNumberId: SongNumberId) -> Unit
) : ListAdapter<SongSearchResultEntity, SearchRecyclerViewAdapter.SearchViewHolder>(DiffCallback()) {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
    val view = LayoutInflater.from(parent.context)
      .inflate(R.layout.layout_search_list_item, parent, false)
    return SearchViewHolder(view)
  }

  override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
    val songSearchResult = getItem(position)
    holder.bind(songSearchResult, clickListener)
  }

  class SearchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val songName: TextView = itemView.findViewById(R.id.txt_song_name)
    private val songNumber: TextView = itemView.findViewById(R.id.txt_song_number)
    private val bookDisplayName: TextView = itemView.findViewById(R.id.txt_book_name)
    private val text: TextView = itemView.findViewById(R.id.txt_text)

    fun bind(
      songSearchResult: SongSearchResultEntity,
      onItemClickListener: (songNumberId: SongNumberId) -> Unit
    ) {
      songNumber.text = songSearchResult.songNumber.toString()
      songName.text = songSearchResult.songName
      bookDisplayName.text = songSearchResult.bookDisplayName
      val snippet = songSearchResult.snippet
      if (snippet.isNotEmpty()) {
        text.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
          Html.fromHtml(snippet, Html.FROM_HTML_MODE_LEGACY)
        } else {
          Html.fromHtml(snippet)
        }
      }
      itemView.setOnClickListener {
        onItemClickListener(songSearchResult.songNumberId)
      }
    }
  }

  class DiffCallback : DiffUtil.ItemCallback<SongSearchResultEntity>() {
    override fun areItemsTheSame(oldItem: SongSearchResultEntity, newItem: SongSearchResultEntity): Boolean {
      return oldItem.songNumberId == newItem.songNumberId
    }

    override fun areContentsTheSame(oldItem: SongSearchResultEntity, newItem: SongSearchResultEntity): Boolean {
      return oldItem == newItem
    }
  }
}