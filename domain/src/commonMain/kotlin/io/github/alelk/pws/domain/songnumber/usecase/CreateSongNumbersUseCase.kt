package io.github.alelk.pws.domain.songnumber.usecase

import io.github.alelk.pws.domain.core.ids.BookId
import io.github.alelk.pws.domain.core.result.CreateResourceResult
import io.github.alelk.pws.domain.core.result.CreateResourcesResult
import io.github.alelk.pws.domain.core.transaction.TransactionRunner
import io.github.alelk.pws.domain.songnumber.model.SongNumberLink
import io.github.alelk.pws.domain.songnumber.repository.SongNumberWriteRepository

class CreateSongNumbersUseCase(
  private val writeRepository: SongNumberWriteRepository,
  private val txRunner: TransactionRunner
) {
  suspend operator fun invoke(bookId: BookId, links: List<SongNumberLink>): CreateResourcesResult<SongNumberLink> =
    txRunner.inRwTransaction {
      for (link in links) {
        when (val result = writeRepository.create(bookId, link)) {
          is CreateResourceResult.Success<*> -> continue
          is CreateResourceResult.AlreadyExists<*> -> return@inRwTransaction CreateResourcesResult.AlreadyExists(link)
          is CreateResourceResult.ValidationError<*> -> return@inRwTransaction CreateResourcesResult.ValidationError(link, result.message)
          is CreateResourceResult.UnknownError<*> -> return@inRwTransaction CreateResourcesResult.UnknownError(link, result.exception, result.message)
        }
      }
      CreateResourcesResult.Success(links)
    }
}