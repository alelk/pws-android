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
import io.github.alelk.pws.database.dao.Favorite
import io.github.alelk.pws.android.app.adapter.FavoritesRecyclerViewAdapter.FavoriteViewHolder

/**
 * Favorites Recycler View Adapter
 *
 * Created by Alex Elkin on 19.05.2016.
 */
class FavoritesRecyclerViewAdapter(
  private val onItemClickListener: (songNumberId: Long) -> Unit
) : ListAdapter<Favorite, FavoriteViewHolder>(FavoriteDiffCallback()) {

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

    fun bind(favorite: Favorite, onItemClickListener: (songNumberId: Long) -> Unit) {
      songNumber.text = favorite.songNumber.toString()
      songName.text = favorite.songName
      bookDisplayName.text = favorite.bookDisplayName
      songNumberId = favorite.songNumberId

      itemView.setOnClickListener {
        onItemClickListener(songNumberId)
      }
    }
  }

  class FavoriteDiffCallback : DiffUtil.ItemCallback<Favorite>() {
    override fun areItemsTheSame(oldItem: Favorite, newItem: Favorite): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Favorite, newItem: Favorite): Boolean = oldItem == newItem
  }
}