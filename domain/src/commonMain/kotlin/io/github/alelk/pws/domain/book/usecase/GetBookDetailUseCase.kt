package io.github.alelk.pws.domain.book.usecase

import io.github.alelk.pws.domain.book.model.BookDetail
import io.github.alelk.pws.domain.book.repository.BookReadRepository
import io.github.alelk.pws.domain.core.ids.BookId
import io.github.alelk.pws.domain.core.transaction.TransactionRunner

/**
 * Read use case: fetch a single BookDetail by id inside a read-only transaction.
 * Returns null if the book does not exist.
 */
class GetBookDetailUseCase(
  private val readRepository: BookReadRepository,
  private val txRunner: TransactionRunner
) {
  suspend operator fun invoke(id: BookId): BookDetail? = txRunner.inRoTransaction { readRepository.get(id) }
}
