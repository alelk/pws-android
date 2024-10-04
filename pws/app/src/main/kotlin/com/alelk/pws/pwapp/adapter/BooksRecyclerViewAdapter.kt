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

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.alelk.pws.database.dao.Book
import com.alelk.pws.pwapp.R
import com.alelk.pws.pwapp.adapter.BooksRecyclerViewAdapter.BooksViewHolder

/**
 * Books Recycler View Adapter
 *
 * Created by Mykhailo Dmytriakha on 05.03.2024.
 */
class BooksRecyclerViewAdapter(private val onItemClickListener: (bookNumberId: Long) -> Unit) : ListAdapter<Book, BooksViewHolder>(BookDiffCallback()) {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BooksViewHolder {
    val view = LayoutInflater.from(parent.context)
      .inflate(R.layout.layout_books_list_item, parent, false)
    return BooksViewHolder(view)
  }

  override fun onBindViewHolder(holder: BooksViewHolder, position: Int) {
    val book = getItem(position)
    holder.bind(book, onItemClickListener)
  }

  class BooksViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val bookName: TextView = itemView.findViewById(R.id.txt_book_name)
    private val bookShortName: TextView = itemView.findViewById(R.id.txt_book_short_name)
    private val bookNumber: TextView = itemView.findViewById(R.id.txt_book_number)

    fun bind(book: Book, onItemClickListener: (bookNumberId: Long) -> Unit) {
      bookName.text = book.displayName
      bookShortName.text = book.name
      bookNumber.text = book.id.toString()
      itemView.setOnClickListener { onItemClickListener(book.firstSongNumberId) }
    }
  }

  class BookDiffCallback : DiffUtil.ItemCallback<Book>() {
    override fun areItemsTheSame(oldItem: Book, newItem: Book): Boolean {
      return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Book, newItem: Book): Boolean {
      return oldItem == newItem
    }
  }
}