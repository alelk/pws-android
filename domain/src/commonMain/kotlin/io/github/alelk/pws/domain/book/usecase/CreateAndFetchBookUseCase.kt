package io.github.alelk.pws.domain.book.usecase

import io.github.alelk.pws.domain.book.command.CreateBookCommand
import io.github.alelk.pws.domain.book.model.BookDetail
import io.github.alelk.pws.domain.book.repository.BookWriteRepository
import io.github.alelk.pws.domain.book.repository.BookReadRepository
import io.github.alelk.pws.domain.core.transaction.TransactionRunner

/**
 * Use case: create a book and immediately fetch its BookDetail.
 * Idiomatic when: write model (CreateBookCommand) differs from read model (BookDetail), and ID is provided externally.
 * If the write repository could return BookDetail directly, that would remove the extra roundtrip.
 */
class CreateAndFetchBookUseCase(
  private val writeRepository: BookWriteRepository,
  private val readRepository: BookReadRepository,
  private val txRunner: TransactionRunner
) {
  /**
   * Creates the book and returns BookDetail. Wrapped in a RW transaction to guarantee visibility and atomicity
   * across potential multi-step creation in the underlying persistence layer.
   *
   * Throws IllegalStateException if the book cannot be read back (should be impossible if create succeeded).
   */
  suspend operator fun invoke(command: CreateBookCommand): BookDetail =
    txRunner.inRwTransaction {
      writeRepository.create(command)
      readRepository.get(command.id)
        ?: error("Book just created but not found: ${command.id}")
    }
}

