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
import com.alelk.pws.pwapp.adapter.HistoryRecyclerViewAdapter.HistoryViewHolder
import android.view.ViewGroup
import android.view.LayoutInflater
import com.alelk.pws.pwapp.R
import androidx.cardview.widget.CardView
import android.widget.TextView
import com.alelk.pws.database.provider.PwsDataProviderContract
import android.text.format.DateUtils
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.alelk.pws.database.provider.PwsDataProviderContract.Books.COLUMN_BOOKDISPLAYNAME
import com.alelk.pws.database.provider.PwsDataProviderContract.History.COLUMN_HISTORYTIMESTAMP
import com.alelk.pws.database.provider.PwsDataProviderContract.History.COLUMN_PSALMNUMBER_ID
import com.alelk.pws.database.provider.PwsDataProviderContract.Psalms.COLUMN_PSALMNAME
import com.alelk.pws.database.provider.PwsDataProviderContract.Psalms.COLUMN_PSALMNUMBER
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

/**
 * History Recycler View Adapter
 *
 * Created by Alex Elkin on 23.05.2016.
 */
class HistoryRecyclerViewAdapter(private val mOnItemClickListener: (id: Long) -> Unit) :
  RecyclerView.Adapter<HistoryViewHolder>() {

  private var mCursor: Cursor? = null
  fun swapCursor(cursor: Cursor?) {
    if (mCursor != null) mCursor!!.close()
    mCursor = cursor
    notifyDataSetChanged()
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
    val v = LayoutInflater.from(parent.context)
      .inflate(R.layout.layout_history_list_item, parent, false)
    return HistoryViewHolder(v)
  }

  override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
    if (mCursor != null && mCursor!!.moveToPosition(position)) {
      holder.bind(mCursor!!, mOnItemClickListener)
    }
  }

  override fun getItemCount(): Int {
    return if (mCursor == null || mCursor!!.isClosed) 0 else mCursor!!.count
  }

  class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    var cardView: CardView
    var psalmName: TextView
    var psalmNumber: TextView
    var bookDisplayName: TextView
    var timestamp: TextView
    var psalmNumberId: Long = 0

    init {
      cardView = itemView.findViewById(R.id.cv_history)
      psalmName = itemView.findViewById(R.id.txt_psalm_name)
      psalmNumber = itemView.findViewById(R.id.txt_psalm_number)
      bookDisplayName = itemView.findViewById(R.id.txt_book_name)
      timestamp = itemView.findViewById(R.id.txt_timestamp)
    }

    fun bind(cursor: Cursor, onItemClickListener: (id: Long) -> Unit) {
      psalmNumber.text = cursor.getString(cursor.getColumnIndex(COLUMN_PSALMNUMBER))
      psalmName.text = cursor.getString(cursor.getColumnIndex(COLUMN_PSALMNAME))
      bookDisplayName.text = cursor.getString(cursor.getColumnIndex(COLUMN_BOOKDISPLAYNAME))
      psalmNumberId = cursor.getLong(cursor.getColumnIndex(COLUMN_PSALMNUMBER_ID))
      var accessTime = cursor.getString(cursor.getColumnIndex(COLUMN_HISTORYTIMESTAMP))
      val df = SimpleDateFormat(PwsDataProviderContract.HISTORY_TIMESTAMP_FORMAT, Locale.US)
      try {
        accessTime = DateUtils.getRelativeTimeSpanString(
          Objects.requireNonNull(df.parse(accessTime)).time,
          System.currentTimeMillis(),
          DateUtils.SECOND_IN_MILLIS
        ) as String
        timestamp.text = accessTime
      } catch (e: ParseException) {
        e.printStackTrace()
      }
      itemView.setOnClickListener { onItemClickListener(psalmNumberId) }
    }
  }
}