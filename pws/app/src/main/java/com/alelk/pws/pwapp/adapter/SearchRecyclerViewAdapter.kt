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
import com.alelk.pws.pwapp.adapter.SearchRecyclerViewAdapter.SearchViewHolder
import android.view.ViewGroup
import android.view.LayoutInflater
import com.alelk.pws.pwapp.R
import androidx.cardview.widget.CardView
import android.widget.TextView
import android.os.Build
import android.text.Html
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.alelk.pws.database.provider.PwsDataProviderContract.Psalms.Search.COLUMN_BOOKDISPLAYNAME
import com.alelk.pws.database.provider.PwsDataProviderContract.Psalms.Search.COLUMN_PSALMNAME
import com.alelk.pws.database.provider.PwsDataProviderContract.Psalms.Search.COLUMN_PSALMNUMBER
import com.alelk.pws.database.provider.PwsDataProviderContract.Psalms.Search.COLUMN_PSALMNUMBER_ID
import com.alelk.pws.database.provider.PwsDataProviderContract.Psalms.Search.COLUMN_SNIPPET

/**
 * Search Recycler View Adapter
 *
 * Created by Alex Elkin on 23.05.2016.
 */
class SearchRecyclerViewAdapter(private val mClickListener: OnItemClickListener) :
  RecyclerView.Adapter<SearchViewHolder>() {
  interface OnItemClickListener {
    fun onItemClick(psalmNumberId: Long)
  }

  private var mCursor: Cursor? = null
  fun swapCursor(cursor: Cursor?) {
    if (mCursor != null) mCursor!!.close()
    mCursor = cursor
    notifyDataSetChanged()
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
    val v = LayoutInflater.from(parent.context)
      .inflate(R.layout.layout_search_list_item, parent, false)
    return SearchViewHolder(v)
  }

  override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
    if (mCursor != null && !mCursor!!.isClosed && mCursor!!.moveToPosition(position)) {
      holder.bind(mCursor!!, mClickListener)
    }
  }

  override fun getItemCount(): Int {
    return if (mCursor == null || mCursor!!.isClosed) 0 else mCursor!!.count
  }

  class SearchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    var cardView: CardView
    var psalmName: TextView
    var psalmNumber: TextView
    var bookDisplayName: TextView
    var text: TextView
    var psalmNumberId: Long = 0

    init {
      cardView = itemView.findViewById(R.id.cv_search)
      psalmName = itemView.findViewById(R.id.txt_psalm_name)
      psalmNumber = itemView.findViewById(R.id.txt_psalm_number)
      bookDisplayName = itemView.findViewById(R.id.txt_book_name)
      text = itemView.findViewById(R.id.txt_text)
    }

    fun bind(cursor: Cursor, onItemClickListener: OnItemClickListener) {
      psalmNumber.text = cursor.getString(cursor.getColumnIndex(COLUMN_PSALMNUMBER))
      psalmName.text = cursor.getString(cursor.getColumnIndex(COLUMN_PSALMNAME))
      bookDisplayName.text = cursor.getString(cursor.getColumnIndex(COLUMN_BOOKDISPLAYNAME))
      val snippet = cursor.getString(cursor.getColumnIndex(COLUMN_SNIPPET))
      if (snippet != null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
          text.text = Html.fromHtml(snippet, Html.FROM_HTML_MODE_LEGACY)
        } else {
          text.text = Html.fromHtml(snippet)
        }
      }
      psalmNumberId = cursor.getLong(cursor.getColumnIndex(COLUMN_PSALMNUMBER_ID))
      itemView.setOnClickListener { v: View? -> onItemClickListener.onItemClick(psalmNumberId) }
    }
  }
}