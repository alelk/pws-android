package io.github.alelk.pws.domain.book.usecase

import io.github.alelk.pws.domain.book.command.CreateBookCommand
import io.github.alelk.pws.domain.book.repository.BookWriteRepository
import io.github.alelk.pws.domain.core.ids.BookId
import io.github.alelk.pws.domain.core.result.CreateResourceResult
import io.github.alelk.pws.domain.core.transaction.TransactionRunner

class CreateBookUseCase(
  private val writeRepository: BookWriteRepository,
  private val txRunner: TransactionRunner
) {
  suspend operator fun invoke(command: CreateBookCommand): CreateResourceResult<BookId> =
    txRunner.inRwTransaction {
      writeRepository.create(command)
    }
}

