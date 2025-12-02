package io.github.alelk.pws.domain.book.repository

import io.github.alelk.pws.domain.book.command.CreateBookCommand
import io.github.alelk.pws.domain.book.command.UpdateBookCommand
import io.github.alelk.pws.domain.core.ids.BookId
import io.github.alelk.pws.domain.core.result.CreateResourceResult
import io.github.alelk.pws.domain.core.result.DeleteResourceResult
import io.github.alelk.pws.domain.core.result.UpdateResourceResult

interface BookWriteRepository {
  suspend fun create(bookCommand: CreateBookCommand): CreateResourceResult<BookId>
  suspend fun update(bookCommand: UpdateBookCommand): UpdateResourceResult<BookId>
  suspend fun delete(bookId: BookId): DeleteResourceResult<BookId>
}