package io.github.alelk.pws.domain.cross.usecase

import io.github.alelk.pws.domain.core.SongNumber
import io.github.alelk.pws.domain.core.ids.SongId
import io.github.alelk.pws.domain.core.result.CreateResourceResult
import io.github.alelk.pws.domain.core.transaction.TransactionRunner
import io.github.alelk.pws.domain.song.command.CreateSongCommand
import io.github.alelk.pws.domain.song.repository.SongWriteRepository
import io.github.alelk.pws.domain.songnumber.model.SongNumberLink
import io.github.alelk.pws.domain.songnumber.repository.SongNumberWriteRepository

/**
 * Use case: create a Song aggregate and link it to multiple Books with explicit numbers.
 * Returns the freshly created SongDetail.
 *
 * Input: a collection of existing domain value objects [SongNumber], each holds (bookId, number).
 * Steps (RW transaction):
 *  1. Validate uniqueness of (bookId, number) per book among provided assignments.
 *  2. Create song.
 *  3. Link song to each book/number.
 */
class CreateSongAndLinkToBooksUseCase(
  private val songWriteRepository: SongWriteRepository,
  private val songNumberWriteRepository: SongNumberWriteRepository,
  private val txRunner: TransactionRunner
) {
  suspend operator fun invoke(command: CreateSongCommand, assignments: Collection<SongNumber>): CreateResourceResult<SongId> =
    txRunner.inRwTransaction {
      assignments
        .groupBy { it.bookId }
        .forEach { (bookId, list) ->
          val duplicates = list.groupBy { it.number }.filter { it.value.size > 1 }.keys
          if (duplicates.isNotEmpty())
            return@inRwTransaction CreateResourceResult
              .ValidationError(command.id, "Duplicate song numbers for book $bookId: ${duplicates.joinToString()}")
        }

      val songCreateResult = songWriteRepository.create(command)
      if (songCreateResult !is CreateResourceResult.Success) return@inRwTransaction songCreateResult

      for (assignment in assignments) {
        when (val r = songNumberWriteRepository.create(assignment.bookId, SongNumberLink(command.id, assignment.number))) {
          is CreateResourceResult.Success<*> ->
            continue

          is CreateResourceResult.ValidationError<*> ->
            return@inRwTransaction CreateResourceResult.ValidationError(command.id, r.message)

          is CreateResourceResult.AlreadyExists<*> ->
            return@inRwTransaction CreateResourceResult.ValidationError(command.id, "illegal state: song number already exists: $assignment")

          is CreateResourceResult.UnknownError<*> ->
            return@inRwTransaction CreateResourceResult.UnknownError(command.id, r.exception, r.message)
        }
      }
      songCreateResult
    }
}
