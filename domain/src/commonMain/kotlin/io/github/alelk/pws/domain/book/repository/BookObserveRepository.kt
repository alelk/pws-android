package io.github.alelk.pws.domain.book.repository

import io.github.alelk.pws.domain.book.model.BookDetail
import io.github.alelk.pws.domain.book.model.BookSummary
import io.github.alelk.pws.domain.book.query.BookQuery
import io.github.alelk.pws.domain.book.query.BookSort
import io.github.alelk.pws.domain.core.ids.BookId
import kotlinx.coroutines.flow.Flow

interface BookObserveRepository {
  fun observe(id: BookId): Flow<BookDetail?>
  fun observeMany(query: BookQuery = BookQuery.Empty, sort: BookSort = BookSort.ByPriorityDesc): Flow<List<BookSummary>>
}