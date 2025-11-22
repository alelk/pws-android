package io.github.alelk.pws.domain.book.repository

import io.github.alelk.pws.domain.book.command.CreateBookCommand
import io.github.alelk.pws.domain.book.command.UpdateBookCommand

interface BookWriteRepository {

  suspend fun create(bookCommand: CreateBookCommand)

  suspend fun update(bookCommand: UpdateBookCommand): Boolean
}