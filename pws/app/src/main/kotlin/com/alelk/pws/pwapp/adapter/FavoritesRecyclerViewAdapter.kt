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

import android.database.Cursor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.alelk.pws.database.provider.PwsDataProviderContract
import com.alelk.pws.pwapp.R
import com.alelk.pws.pwapp.adapter.FavoritesRecyclerViewAdapter.FavoriteViewHolder

/**
 * Favorites Recycler View Adapter
 *
 * Created by Alex Elkin on 19.05.2016.
 */
class FavoritesRecyclerViewAdapter(private val mOnItemClickListener: ((psalmNumberId: Long) -> Unit)) :
  RecyclerView.Adapter<FavoriteViewHolder>() {

  private var mCursor: Cursor? = null
  fun swapCursor(cursor: Cursor?) {
    if (mCursor != null) mCursor!!.close()
    mCursor = cursor
    notifyDataSetChanged()
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteViewHolder {
    val v = LayoutInflater.from(parent.context)
      .inflate(R.layout.layout_psalm_list_item, parent, false)
    return FavoriteViewHolder(v)
  }

  override fun onBindViewHolder(holder: FavoriteViewHolder, position: Int) {
    if (mCursor != null && mCursor!!.moveToPosition(position)) {
      holder.bind(mCursor!!, mOnItemClickListener)
    }
  }

  override fun getItemCount(): Int {
    return if (mCursor == null || mCursor!!.isClosed) 0 else mCursor!!.count
  }

  fun updateView() {
    notifyDataSetChanged()
  }

  class FavoriteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    var cardView: CardView
    var psalmName: TextView
    var psalmNumber: TextView
    var bookDisplayName: TextView
    var psalmNumberId: Long = 0

    init {
      cardView = itemView.findViewById(R.id.cv_psalm_item)
      psalmName = itemView.findViewById(R.id.txt_psalm_name)
      psalmNumber = itemView.findViewById(R.id.txt_psalm_number)
      bookDisplayName = itemView.findViewById(R.id.txt_book_name)
    }

    fun bind(cursor: Cursor, onItemClickListener: (psalmNumberId: Long) -> Unit) {
      psalmNumber.text =
        cursor.getString(cursor.getColumnIndex(PwsDataProviderContract.Favorites.COLUMN_PSALMNUMBER))
      psalmName.text =
        cursor.getString(cursor.getColumnIndex(PwsDataProviderContract.Favorites.COLUMN_PSALMNAME))
      bookDisplayName.text =
        cursor.getString(cursor.getColumnIndex(PwsDataProviderContract.Favorites.COLUMN_BOOKDISPLAYNAME))
      psalmNumberId =
        cursor.getLong(cursor.getColumnIndex(PwsDataProviderContract.Favorites.COLUMN_PSALMNUMBER_ID))
      itemView.setOnClickListener { onItemClickListener(psalmNumberId) }
    }
  }
}