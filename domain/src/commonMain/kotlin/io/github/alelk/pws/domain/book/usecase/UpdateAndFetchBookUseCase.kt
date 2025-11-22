package io.github.alelk.pws.domain.book.usecase

import io.github.alelk.pws.domain.book.command.UpdateBookCommand
import io.github.alelk.pws.domain.book.model.BookDetail
import io.github.alelk.pws.domain.book.repository.BookWriteRepository
import io.github.alelk.pws.domain.book.repository.BookReadRepository
import io.github.alelk.pws.domain.core.transaction.TransactionRunner

/**
 * Use case: update a book (patch semantics) and immediately fetch its BookDetail.
 * Wrapped in a RW transaction for atomicity & consistent snapshot.
 */
class UpdateAndFetchBookUseCase(
  private val writeRepository: BookWriteRepository,
  private val readRepository: BookReadRepository,
  private val txRunner: TransactionRunner
) {
  suspend operator fun invoke(command: UpdateBookCommand): BookDetail =
    txRunner.inRwTransaction {
      writeRepository.update(command)
      readRepository.get(command.id) ?: error("Book updated but not found: ${command.id}")
    }
}

