package io.github.alelk.pws.features.usecase

import io.github.alelk.pws.domain.book.model.BookSummary
import io.github.alelk.pws.domain.book.query.BookQuery
import io.github.alelk.pws.domain.book.query.BookSort
import io.github.alelk.pws.domain.book.repository.BookRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Temporary extension to satisfy features module until domain exposes observeBooks. */
fun BookRepository.observeBooks(query: BookQuery, sort: BookSort): Flow<List<BookSummary>> = flow {
  // TODO remove once real observeBooks exists or features updated.
  emit(emptyList())
}

