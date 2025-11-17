package io.github.alelk.pws.data.repository.room.book

import io.github.alelk.pws.database.book.BookDao
import io.github.alelk.pws.domain.book.model.BookDetail
import io.github.alelk.pws.domain.book.model.BookSummary
import io.github.alelk.pws.domain.book.query.BookQuery
import io.github.alelk.pws.domain.book.query.BookSort
import io.github.alelk.pws.domain.book.query.bookSummaryComparator
import io.github.alelk.pws.domain.book.repository.BookObserveRepository
import io.github.alelk.pws.domain.book.repository.BookReadRepository
import io.github.alelk.pws.domain.core.ids.BookId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class BookRepositoryImpl(
  val bookDao: BookDao
) : BookObserveRepository, BookReadRepository {
  override fun observe(id: BookId): Flow<BookDetail?> =
    bookDao.observeBookDetail(id).map { it?.toDomain() }

  override fun observeMany(query: BookQuery, sort: BookSort): Flow<List<BookSummary>> =
    bookDao
      .observeBooksSummary(locale = query.locale, minPriority = query.minPriority, maxPriority = query.maxPriority)
      .distinctUntilChanged()
      .map { books -> books.map { it.toDomain() } }
      .map { books -> books.sortedWith(sort.bookSummaryComparator) }


  override suspend fun get(id: BookId): BookDetail? = bookDao.getBookDetail(id)?.toDomain()

  override suspend fun getMany(query: BookQuery, sort: BookSort): List<BookSummary> =
    bookDao
      .getBooksSummary(locale = query.locale, minPriority = query.minPriority, maxPriority = query.maxPriority)
      .map { it.toDomain() }
      .sortedWith(sort.bookSummaryComparator)
}