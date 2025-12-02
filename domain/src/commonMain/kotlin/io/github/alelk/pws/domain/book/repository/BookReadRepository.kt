package io.github.alelk.pws.domain.book.repository

import io.github.alelk.pws.domain.book.model.BookDetail
import io.github.alelk.pws.domain.book.model.BookSummary
import io.github.alelk.pws.domain.book.query.BookQuery
import io.github.alelk.pws.domain.book.query.BookSort
import io.github.alelk.pws.domain.core.ids.BookId

interface BookReadRepository {
  suspend fun get(id: BookId): BookDetail?
  suspend fun getMany(query: BookQuery = BookQuery.Empty, sort: BookSort = BookSort.ByPriorityDesc): List<BookSummary>
}