package io.github.alelk.pws.domain.book.usecase

import io.github.alelk.pws.domain.book.model.BookSummary
import io.github.alelk.pws.domain.book.query.BookQuery
import io.github.alelk.pws.domain.book.query.BookSort
import io.github.alelk.pws.domain.book.repository.BookReadRepository
import io.github.alelk.pws.domain.core.transaction.TransactionRunner

/**
 * Read use case: fetch multiple books (summaries) by query & sort.
 * Always executed inside a read-only transaction for snapshot consistency.
 */
class GetBooksUseCase(
  private val readRepository: BookReadRepository,
  private val txRunner: TransactionRunner
) {
  suspend operator fun invoke(
    query: BookQuery = BookQuery.Empty,
    sort: BookSort = BookSort.ByPriorityDesc
  ): List<BookSummary> = txRunner.inRoTransaction { readRepository.getMany(query, sort) }
}
