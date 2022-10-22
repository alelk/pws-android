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

import android.content.Context
import android.database.Cursor
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.alelk.pws.database.provider.PwsDataProviderContract.Books.COLUMN_BOOKDISPLAYNAME
import com.alelk.pws.database.provider.PwsDataProviderContract.PsalmNumbers.ReferencePsalms.COLUMN_PSALMNUMBER_ID
import com.alelk.pws.database.provider.PwsDataProviderContract.PsalmNumbers.ReferencePsalms.COLUMN_PSALMREF_REASON
import com.alelk.pws.database.provider.PwsDataProviderContract.PsalmNumbers.ReferencePsalms.COLUMN_PSALMREF_VOLUME
import com.alelk.pws.database.provider.PwsDataProviderContract.Psalms.COLUMN_PSALMNAME
import com.alelk.pws.database.provider.PwsDataProviderContract.Psalms.COLUMN_PSALMNUMBER
import com.alelk.pws.pwapp.R

/**
 * Referred Psalms Recycler View Adapter
 *
 * Created by Alex Elkin on 06.01.2017.
 */
class ReferredPsalmsRecyclerViewAdapter(
  private val mOnItemClickListener: (psalmNumberId: Long) -> Unit,
  private val mHeaderTextSize: Float
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

  private var mCursor: Cursor? = null
  fun swapCursor(cursor: Cursor?) {
    if (mCursor != null) mCursor!!.close()
    mCursor = cursor
    notifyDataSetChanged()
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
    if (viewType == TYPE_HEADER) {
      val v = LayoutInflater.from(parent.context)
        .inflate(R.layout.layout_referred_psalm_list_header, parent, false)
      return HeaderViewHolder(v)
    }
    val v = LayoutInflater.from(parent.context)
      .inflate(R.layout.layout_referred_psalm_list_item, parent, false)
    return ReferredViewHolder(v, parent.context)
  }

  override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
    if (position == 0) {
      if (mHeaderTextSize < MIN_HEADER_TEXT_SIZE) return
      val headerViewHolder = holder as HeaderViewHolder
      headerViewHolder.bind(mHeaderTextSize)
    } else {
      val referredViewHolder = holder as ReferredViewHolder
      if (mCursor != null && mCursor!!.moveToPosition(position - 1)) {
        referredViewHolder.bind(mCursor!!, mOnItemClickListener)
      }
    }
  }

  override fun getItemViewType(position: Int): Int {
    return if (position == 0) TYPE_HEADER else TYPE_ITEM
  }

  override fun getItemCount(): Int {
    return if (mCursor == null || mCursor!!.isClosed || mCursor!!.count == 0) 0 else mCursor!!.count + 1
  }

  private class ReferredViewHolder internal constructor(itemView: View, val mContext: Context) :
    RecyclerView.ViewHolder(itemView) {
    var psalmName: TextView
    var psalmNumber: TextView
    var bookDisplayName: TextView
    var reason: TextView
    var psalmNumberId: Long = 0

    init {
      psalmName = itemView.findViewById(R.id.txt_psalm_name)
      psalmNumber = itemView.findViewById(R.id.txt_psalm_number)
      bookDisplayName = itemView.findViewById(R.id.txt_book_name)
      reason = itemView.findViewById(R.id.txt_reason)
    }

    fun bind(cursor: Cursor, onItemClickListener: (psalmNumberId: Long) -> Unit) {
      psalmNumber.text = cursor.getString(cursor.getColumnIndex(COLUMN_PSALMNUMBER))
      psalmName.text = cursor.getString(cursor.getColumnIndex(COLUMN_PSALMNAME))
      bookDisplayName.text = cursor.getString(cursor.getColumnIndex(COLUMN_BOOKDISPLAYNAME))
      psalmNumberId = cursor.getLong(cursor.getColumnIndex(COLUMN_PSALMNUMBER_ID))
      var reasonTxt = ""
      if ("variation" == cursor.getString(cursor.getColumnIndex(COLUMN_PSALMREF_REASON))) {
        reasonTxt = mContext.getString(
          R.string.lbl_another_variant,
          cursor.getInt(cursor.getColumnIndex(COLUMN_PSALMREF_VOLUME))
        )
      }
      reason.text = reasonTxt
      itemView.setOnClickListener { v: View? -> onItemClickListener(psalmNumberId) }
    }
  }

  internal class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    var txtReferences: TextView

    init {
      txtReferences = itemView.findViewById(R.id.lbl_psalm_references)
    }

    fun bind(textSize: Float) {
      txtReferences.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
    }
  }

  companion object {
    private const val TYPE_HEADER = 0
    private const val TYPE_ITEM = 1
    private const val MIN_HEADER_TEXT_SIZE = 10f
  }
}