package io.github.alelk.pws.domain.book.repository

import io.github.alelk.pws.domain.book.command.CreateBookCommand
import io.github.alelk.pws.domain.book.command.CreateBookResult
import io.github.alelk.pws.domain.book.command.UpdateBookCommand
import io.github.alelk.pws.domain.book.command.UpdateBookResult

interface BookWriteRepository {
  suspend fun create(bookCommand: CreateBookCommand): CreateBookResult
  suspend fun update(bookCommand: UpdateBookCommand): UpdateBookResult
}