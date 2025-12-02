package io.github.alelk.pws.domain.songnumber.usecase

import io.github.alelk.pws.domain.core.ids.BookId
import io.github.alelk.pws.domain.core.result.CreateResourceResult
import io.github.alelk.pws.domain.core.result.DeleteResourceResult
import io.github.alelk.pws.domain.core.result.ReplaceAllResourcesResult
import io.github.alelk.pws.domain.core.result.UpdateResourceResult
import io.github.alelk.pws.domain.core.transaction.TransactionRunner
import io.github.alelk.pws.domain.songnumber.model.SongNumberLink
import io.github.alelk.pws.domain.songnumber.repository.SongNumberReadRepository
import io.github.alelk.pws.domain.songnumber.repository.SongNumberWriteRepository

/**
 * Use case: Replace all existing associations for a book with the provided set.
 *  - Any missing songs are removed
 *  - Any new song numbers are inserted.
 *
 * Applied atomically.
 */
class ReplaceAllBookSongNumbersUseCase(
  val readRepository: SongNumberReadRepository,
  val writeRepository: SongNumberWriteRepository,
  val txRunner: TransactionRunner
) {

  suspend operator fun invoke(bookId: BookId, assignments: Collection<SongNumberLink>): ReplaceAllResourcesResult<SongNumberLink> =
    txRunner.inRwTransaction {
      val existingLinks = readRepository.getAllByBookId(bookId).toSet()
      val targetLinkSongIds = assignments.map { it.songId }.toSet()
      val existingLinkSongIds = existingLinks.map { it.songId }.toSet()

      val unchangedLinks = assignments intersect existingLinks
      val linksToDelete = existingLinks.filterNot { it.songId in targetLinkSongIds }
      val linksToCreate = assignments.filterNot { it.songId in existingLinkSongIds }
      val linksToUpdate = (assignments - linksToCreate.toSet() - unchangedLinks.toSet())

      for (link in linksToUpdate) {
        when (val result = writeRepository.update(bookId, link)) {
          is UpdateResourceResult.Success<*> -> continue
          is UpdateResourceResult.NotFound<*> -> error("illegal state: updating song number not found: $bookId $link")
          is UpdateResourceResult.ValidationError<*> -> return@inRwTransaction ReplaceAllResourcesResult.ValidationError(link, result.message)
          is UpdateResourceResult.UnknownError<*> -> return@inRwTransaction ReplaceAllResourcesResult.UnknownError(link, result.exception, result.message)
        }
      }

      for (link in linksToCreate) {
        when (val result = writeRepository.create(bookId, link)) {
          is CreateResourceResult.Success<*> -> continue
          is CreateResourceResult.AlreadyExists<*> -> error("illegal state: creating song number already exists: $bookId $link")
          is CreateResourceResult.ValidationError<*> -> return@inRwTransaction ReplaceAllResourcesResult.ValidationError(link, result.message)
          is CreateResourceResult.UnknownError<*> -> return@inRwTransaction ReplaceAllResourcesResult.UnknownError(link, result.exception, result.message)
        }
      }

      for (link in linksToDelete) {
        when (val result = writeRepository.delete(bookId, link.songId)) {
          is DeleteResourceResult.Success<*> -> continue
          is DeleteResourceResult.NotFound<*> -> error("illegal state: deleting song number not found: $bookId $link")
          is DeleteResourceResult.ValidationError<*> -> return@inRwTransaction ReplaceAllResourcesResult.ValidationError(link, result.message)
          is DeleteResourceResult.UnknownError<*> -> return@inRwTransaction ReplaceAllResourcesResult.UnknownError(link, result.exception, result.message)
        }
      }

      ReplaceAllResourcesResult.Success(created = linksToCreate, updated = linksToUpdate, unchanged = unchangedLinks.toList(), deleted = linksToDelete)
    }
}