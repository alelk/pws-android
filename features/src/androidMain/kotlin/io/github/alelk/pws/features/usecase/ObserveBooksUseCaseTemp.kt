package io.github.alelk.pws.features.usecase

import io.github.alelk.pws.domain.book.model.BookSummary
import io.github.alelk.pws.domain.book.query.BookQuery
import io.github.alelk.pws.domain.book.query.BookSort
import io.github.alelk.pws.domain.book.repository.BookRepository
import kotlinx.coroutines.flow.Flow

/** Temporary until domain provides a proper ObserveBooksUseCase. */
class ObserveBooksUseCaseTemp(private val repository: BookRepository) {
  operator fun invoke(query: BookQuery = BookQuery.Empty, sort: BookSort = BookSort.ByPriorityDesc): Flow<List<BookSummary>> =
    repository.observeBooks(query, sort)
  // TODO remove once real use case exists in domain module.
}