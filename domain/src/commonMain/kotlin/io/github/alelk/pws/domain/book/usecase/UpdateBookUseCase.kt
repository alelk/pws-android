package io.github.alelk.pws.domain.book.usecase

import io.github.alelk.pws.domain.book.command.UpdateBookCommand
import io.github.alelk.pws.domain.book.command.UpdateBookResult
import io.github.alelk.pws.domain.book.repository.BookWriteRepository
import io.github.alelk.pws.domain.core.transaction.TransactionRunner

/** Use case: update a book (patch semantics). */
class UpdateBookUseCase(
  private val writeRepository: BookWriteRepository,
  private val txRunner: TransactionRunner
) {
  suspend operator fun invoke(command: UpdateBookCommand): UpdateBookResult =
    txRunner.inRwTransaction {
      writeRepository.update(command)
    }
}

