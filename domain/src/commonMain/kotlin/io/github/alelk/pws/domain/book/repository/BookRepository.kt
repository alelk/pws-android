package io.github.alelk.pws.domain.book.repository

import io.github.alelk.pws.domain.book.model.BookDetail
import io.github.alelk.pws.domain.book.model.BookSummary
import io.github.alelk.pws.domain.book.query.BookQuery
import io.github.alelk.pws.domain.book.query.BookSort
import io.github.alelk.pws.domain.core.ids.BookId
import kotlinx.coroutines.flow.Flow

interface BookRepository {
  fun observeBook(id: BookId): Flow<BookDetail?>
  fun observeBooks(query: BookQuery = BookQuery.Empty, sort: BookSort = BookSort.ByPriorityDesc): Flow<List<BookSummary>>

  suspend fun get(id: BookId): BookDetail?
}