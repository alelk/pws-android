package io.github.alelk.pws.android.app.feature.books

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.github.alelk.pws.android.app.R
import io.github.alelk.pws.database.book.BookWithSongNumbersProjection

/**
 * Books Recycler View Adapter
 *
 * Created by Mykhailo Dmytriakha on 05.03.2024.
 */
class BooksRecyclerViewAdapter(
  private val onItemClickListener: (bookNumberId: BookWithSongNumbersProjection) -> Unit
) : ListAdapter<BookWithSongNumbersProjection, BooksRecyclerViewAdapter.BooksViewHolder>(BookDiffCallback()) {

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

    fun bind(book: BookWithSongNumbersProjection, onItemClickListener: (book: BookWithSongNumbersProjection) -> Unit) {
      bookName.text = book.book.displayName
      bookShortName.text = book.book.name
      bookNumber.text = book.book.displayShortName
      itemView.setOnClickListener { onItemClickListener(book) }
    }
  }

  class BookDiffCallback : DiffUtil.ItemCallback<BookWithSongNumbersProjection>() {
    override fun areItemsTheSame(oldItem: BookWithSongNumbersProjection, newItem: BookWithSongNumbersProjection): Boolean {
      return oldItem.book.id == newItem.book.id
    }

    override fun areContentsTheSame(oldItem: BookWithSongNumbersProjection, newItem: BookWithSongNumbersProjection): Boolean {
      return oldItem == newItem
    }
  }
}