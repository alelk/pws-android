package io.github.alelk.pws.domain.book.command

import io.github.alelk.pws.domain.core.ids.BookId

sealed interface UpdateBookResult {
  data class Success(val bookId: BookId) : UpdateBookResult
  data class NotFound(val bookId: BookId) : UpdateBookResult
  data class ValidationError(val message: String) : UpdateBookResult
  data class UnknownError(val message: String, val exception: Throwable? = null) : UpdateBookResult
}