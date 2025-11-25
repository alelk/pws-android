package io.github.alelk.pws.domain.book.command

import io.github.alelk.pws.domain.core.ids.BookId

sealed interface CreateBookResult {
  data class Success(val bookId: BookId) : CreateBookResult
  data class AlreadyExists(val bookId: BookId) : CreateBookResult
  data class ValidationError(val message: String) : CreateBookResult
  data class UnknownError(val message: String, val exception: Throwable? = null) : CreateBookResult
}