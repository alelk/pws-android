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

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.github.alelk.pws.android.app.R
import io.github.alelk.pws.android.app.adapter.FavoritesRecyclerViewAdapter.FavoriteViewHolder
import io.github.alelk.pws.database.entity.FavoriteWithSongNumberWithSongWithBook

/**
 * Favorites Recycler View Adapter
 *
 * Created by Alex Elkin on 19.05.2016.
 */
class FavoritesRecyclerViewAdapter(
  private val onItemClickListener: (songNumberId: Long) -> Unit
) : ListAdapter<FavoriteWithSongNumberWithSongWithBook, FavoriteViewHolder>(FavoriteDiffCallback()) {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteViewHolder {
    val view = LayoutInflater.from(parent.context)
      .inflate(R.layout.layout_song_list_item, parent, false)
    return FavoriteViewHolder(view)
  }

  override fun onBindViewHolder(holder: FavoriteViewHolder, position: Int) {
    val favorite = getItem(position)
    holder.bind(favorite, onItemClickListener)
  }

  class FavoriteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val songName: TextView = itemView.findViewById(R.id.txt_song_name)
    private val songNumber: TextView = itemView.findViewById(R.id.txt_song_number)
    private val bookDisplayName: TextView = itemView.findViewById(R.id.txt_book_name)
    private var songNumberId: Long = 0

    fun bind(favorite: FavoriteWithSongNumberWithSongWithBook, onItemClickListener: (songNumberId: Long) -> Unit) {
      songNumber.text = favorite.songNumber.number.toString()
      songName.text = favorite.song.name
      bookDisplayName.text = favorite.book.displayName
      songNumberId = favorite.songNumberId

      itemView.setOnClickListener {
        onItemClickListener(songNumberId)
      }
    }
  }

  class FavoriteDiffCallback : DiffUtil.ItemCallback<FavoriteWithSongNumberWithSongWithBook>() {
    override fun areItemsTheSame(oldItem: FavoriteWithSongNumberWithSongWithBook, newItem: FavoriteWithSongNumberWithSongWithBook): Boolean =
      oldItem.favorite.id == newItem.favorite.id

    override fun areContentsTheSame(oldItem: FavoriteWithSongNumberWithSongWithBook, newItem: FavoriteWithSongNumberWithSongWithBook): Boolean =
      oldItem == newItem
  }
}