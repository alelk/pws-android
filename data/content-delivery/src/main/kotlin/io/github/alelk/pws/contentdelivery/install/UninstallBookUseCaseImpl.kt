package io.github.alelk.pws.contentdelivery.install

import arrow.core.Either
import io.github.alelk.pws.domain.booklibrary.repository.BookContentWriter
import io.github.alelk.pws.domain.booklibrary.usecase.UninstallBookUseCase
import io.github.alelk.pws.domain.core.error.DeleteError
import io.github.alelk.pws.domain.core.ids.BookId
import timber.log.Timber

/**
 * Uninstalls a downloaded book through [BookContentWriter.uninstall] (one transaction, pws-core): Left
 * [DeleteError.NotFound] for a book that is not installed, [DeleteError.ValidationError] for a built-in book.
 */
class UninstallBookUseCaseImpl(private val writer: BookContentWriter) : UninstallBookUseCase {
  override suspend fun invoke(bookId: BookId): Either<DeleteError, Unit> {
    Timber.i("Uninstalling book $bookId")
    return writer.uninstall(bookId)
      .onRight { Timber.i("Uninstall complete: $bookId") }
      .onLeft { Timber.w("Uninstall of $bookId failed: ${it.message}") }
  }
}
