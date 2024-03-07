/*
 * Copyright (C) 2024 The P&W Songs Open Source Project
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
import com.alelk.pws.database.table.PwsBookTable
import com.alelk.pws.pwapp.R
import com.alelk.pws.pwapp.adapter.BooksRecyclerViewAdapter.BooksViewHolder

/**
 * Books Recycler View Adapter
 *
 * Created by Mykhailo Dmytriakha on 05.03.2024.
 */
class BooksRecyclerViewAdapter(private val mOnItemClickListener: ((bookNumberId: Long) -> Unit)) :
  RecyclerView.Adapter<BooksViewHolder>() {

  private var mCursor: Cursor? = null
  fun swapCursor(cursor: Cursor?) {
    if (mCursor != null) mCursor!!.close()
    mCursor = cursor
    notifyDataSetChanged()
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BooksViewHolder {
    val v = LayoutInflater.from(parent.context)
      .inflate(R.layout.layout_books_list_item, parent, false)
    return BooksViewHolder(v)
  }

  override fun onBindViewHolder(holder: BooksViewHolder, position: Int) {
    if (mCursor != null && mCursor!!.moveToPosition(position)) {
      holder.bind(mCursor!!, mOnItemClickListener)
    }
  }

  override fun getItemCount(): Int {
    return if (mCursor == null || mCursor!!.isClosed) 0 else mCursor!!.count
  }

  class BooksViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    var cardView: CardView
    var bookName: TextView
    var bookShortName: TextView
    var bookNumber: TextView
    var psalmNumberId: Long = -1

    init {
      cardView = itemView.findViewById(R.id.cv_book)
      bookName = itemView.findViewById(R.id.txt_book_name)
      bookShortName = itemView.findViewById(R.id.txt_book_short_name)
      bookNumber = itemView.findViewById(R.id.txt_book_number)

    }

    fun bind(cursor: Cursor, onItemClickListener: (bookNumberId: Long) -> Unit) {
      //bookNumber.text = cursor.getString(cursor.getColumnIndex(PwsBookTable.COLUMN_ID))
      bookName.text = cursor.getString(cursor.getColumnIndex(PwsDataProviderContract.Books.COLUMN_DISPLAY_NAME))
      bookShortName.text = cursor.getString(cursor.getColumnIndex(PwsDataProviderContract.Books.COLUMN_DISPLAY_SHORT_NAME))
      psalmNumberId = cursor.getLong(cursor.getColumnIndex(PwsDataProviderContract.Books.PSALM_NUMBER_ID))
      itemView.setOnClickListener { onItemClickListener(psalmNumberId) }
    }
  }
}