package io.github.alelk.pws.domain.book.usecase

import io.github.alelk.pws.domain.book.model.BookSummary
import io.github.alelk.pws.domain.book.query.BookQuery
import io.github.alelk.pws.domain.book.query.BookSort
import io.github.alelk.pws.domain.book.repository.BookRepository
import kotlinx.coroutines.flow.Flow

class ObserveBooksUseCase(private val bookRepository: BookRepository) {

  operator fun invoke(query: BookQuery = BookQuery.Empty, sort: BookSort = BookSort.ByPriorityDesc): Flow<List<BookSummary>> =
    bookRepository.observeBooks(query, sort)
}