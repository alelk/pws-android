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

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.alelk.pws.database.dao.Favorite
import com.alelk.pws.pwapp.R
import com.alelk.pws.pwapp.adapter.FavoritesRecyclerViewAdapter.FavoriteViewHolder

/**
 * Favorites Recycler View Adapter
 *
 * Created by Alex Elkin on 19.05.2016.
 */
class FavoritesRecyclerViewAdapter(
  private val onItemClickListener: (psalmNumberId: Long) -> Unit
) : ListAdapter<Favorite, FavoriteViewHolder>(FavoriteDiffCallback()) {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteViewHolder {
    val view = LayoutInflater.from(parent.context)
      .inflate(R.layout.layout_psalm_list_item, parent, false)
    return FavoriteViewHolder(view)
  }

  override fun onBindViewHolder(holder: FavoriteViewHolder, position: Int) {
    val favorite = getItem(position)
    holder.bind(favorite, onItemClickListener)
  }

  class FavoriteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val psalmName: TextView = itemView.findViewById(R.id.txt_psalm_name)
    private val psalmNumber: TextView = itemView.findViewById(R.id.txt_psalm_number)
    private val bookDisplayName: TextView = itemView.findViewById(R.id.txt_book_name)
    private var psalmNumberId: Long = 0

    fun bind(favorite: Favorite, onItemClickListener: (psalmNumberId: Long) -> Unit) {
      psalmNumber.text = favorite.songNumber.toString()
      psalmName.text = favorite.songName
      bookDisplayName.text = favorite.bookDisplayName
      psalmNumberId = favorite.songNumberId

      itemView.setOnClickListener {
        onItemClickListener(psalmNumberId)
      }
    }
  }

  class FavoriteDiffCallback : DiffUtil.ItemCallback<Favorite>() {
    override fun areItemsTheSame(oldItem: Favorite, newItem: Favorite): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Favorite, newItem: Favorite): Boolean = oldItem == newItem
  }
}