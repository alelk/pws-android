package io.github.alelk.pws.domain.book.usecase

import io.github.alelk.pws.domain.book.command.CreateBookCommand
import io.github.alelk.pws.domain.book.command.CreateBookResult
import io.github.alelk.pws.domain.book.repository.BookWriteRepository
import io.github.alelk.pws.domain.core.transaction.TransactionRunner

class CreateBookUseCase(
  private val writeRepository: BookWriteRepository,
  private val txRunner: TransactionRunner
) {
  suspend operator fun invoke(command: CreateBookCommand): CreateBookResult =
    txRunner.inRwTransaction {
      writeRepository.create(command)
    }
}

