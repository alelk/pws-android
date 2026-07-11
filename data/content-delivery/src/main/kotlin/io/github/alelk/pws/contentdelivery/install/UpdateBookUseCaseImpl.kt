package io.github.alelk.pws.contentdelivery.install

import io.github.alelk.pws.domain.booklibrary.model.BookCatalogEntry
import io.github.alelk.pws.domain.booklibrary.model.DownloadState
import io.github.alelk.pws.domain.booklibrary.usecase.InstallBookUseCase
import io.github.alelk.pws.domain.booklibrary.usecase.UpdateBookUseCase
import kotlinx.coroutines.flow.Flow

class UpdateBookUseCaseImpl(
    private val installBookUseCase: InstallBookUseCase,
) : UpdateBookUseCase {
    override fun invoke(entry: BookCatalogEntry): Flow<DownloadState> = installBookUseCase(entry)
}
