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

import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.github.alelk.pws.android.app.R
import io.github.alelk.pws.database.entity.SongRefReason
import io.github.alelk.pws.database.entity.SongReferenceDetailsEntity
import io.github.alelk.pws.domain.model.SongNumberId

/**
 * Song References Recycler View Adapter
 *
 * Created by Alex Elkin on 06.01.2017.
 */
class SongReferencesRecyclerViewAdapter(
  private val headerTextSize: Float,
  private val onItemClickListener: (songNumberId: SongNumberId) -> Unit
) : ListAdapter<SongReferenceDetailsEntity, RecyclerView.ViewHolder>(DiffCallback()) {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
    return if (viewType == TYPE_HEADER) {
      val view = LayoutInflater.from(parent.context)
        .inflate(R.layout.layout_referred_song_list_header, parent, false)
      HeaderViewHolder(view)
    } else {
      val view = LayoutInflater.from(parent.context)
        .inflate(R.layout.layout_referred_song_list_item, parent, false)
      ReferredViewHolder(view, parent.context)
    }
  }

  override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
    if (position == 0) {
      val headerViewHolder = holder as HeaderViewHolder
      if (headerTextSize >= MIN_HEADER_TEXT_SIZE) {
        headerViewHolder.bind(headerTextSize)
      }
    } else {
      val referredViewHolder = holder as ReferredViewHolder
      val item = getItem(position - 1)
      referredViewHolder.bind(item, onItemClickListener)
    }
  }

  override fun getItemViewType(position: Int): Int = if (position == 0) TYPE_HEADER else TYPE_ITEM
  override fun getItemCount(): Int = super.getItemCount() + 1

  class ReferredViewHolder(itemView: View, val context: Context) : RecyclerView.ViewHolder(itemView) {
    private val songName: TextView = itemView.findViewById(R.id.txt_song_name)
    private val songNumber: TextView = itemView.findViewById(R.id.txt_song_number)
    private val bookDisplayName: TextView = itemView.findViewById(R.id.txt_book_name)
    private val reason: TextView = itemView.findViewById(R.id.txt_reason)

    fun bind(item: SongReferenceDetailsEntity, onItemClickListener: (songNumberId: SongNumberId) -> Unit) {
      songName.text = item.song.name
      songNumber.text = item.songNumber.number.toString()
      bookDisplayName.text = item.book.displayName

      val reasonText = if (item.songRef.reason == SongRefReason.Variation) context.getString(R.string.lbl_another_variant, item.songRef.volume) else ""
      reason.text = reasonText

      itemView.setOnClickListener {
        onItemClickListener(item.songNumber.id)
      }
    }
  }

  class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val txtReferences: TextView = itemView.findViewById(R.id.lbl_song_references)
    fun bind(textSize: Float) {
      txtReferences.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
    }
  }

  companion object {
    private const val TYPE_HEADER = 0
    private const val TYPE_ITEM = 1
    private const val MIN_HEADER_TEXT_SIZE = 10f
  }

  class DiffCallback : DiffUtil.ItemCallback<SongReferenceDetailsEntity>() {
    override fun areItemsTheSame(oldItem: SongReferenceDetailsEntity, newItem: SongReferenceDetailsEntity): Boolean =
      oldItem.songRef.songId == newItem.songRef.songId && oldItem.songRef.refSongId == newItem.songRef.refSongId

    override fun areContentsTheSame(oldItem: SongReferenceDetailsEntity, newItem: SongReferenceDetailsEntity): Boolean =
      oldItem == newItem
  }
}