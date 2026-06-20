package io.github.alelk.pws.contentdelivery.install

import arrow.core.Either
import io.github.alelk.pws.domain.booklibrary.usecase.UninstallBookUseCase
import io.github.alelk.pws.domain.core.error.DeleteError
import io.github.alelk.pws.domain.core.ids.BookId

class UninstallBookUseCaseImpl(
    private val uninstaller: BookUninstallerImpl,
) : UninstallBookUseCase {
    override suspend fun invoke(bookId: BookId): Either<DeleteError, Unit> =
        runCatching { uninstaller.uninstall(bookId) }
            .fold(
                onSuccess = { Either.Right(Unit) },
                onFailure = { e ->
                    when {
                        e is IllegalStateException && e.message?.contains("not installed") == true ->
                            Either.Left(DeleteError.NotFound)
                        e is IllegalStateException && e.message?.contains("Cannot uninstall built-in") == true ->
                            Either.Left(DeleteError.ValidationError(e.message ?: "Cannot uninstall built-in book"))
                        else -> Either.Left(DeleteError.UnknownError(e))
                    }
                },
            )
}
