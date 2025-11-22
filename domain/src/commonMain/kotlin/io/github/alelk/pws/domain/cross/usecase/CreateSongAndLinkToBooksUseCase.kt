package io.github.alelk.pws.domain.cross.usecase

import io.github.alelk.pws.domain.core.SongNumber
import io.github.alelk.pws.domain.core.transaction.TransactionRunner
import io.github.alelk.pws.domain.song.command.CreateSongCommand
import io.github.alelk.pws.domain.song.model.SongDetail
import io.github.alelk.pws.domain.song.repository.SongReadRepository
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
 *  4. Read back SongDetail.
 */
class CreateSongAndLinkToBooksUseCase(
  private val songWriteRepository: SongWriteRepository,
  private val songReadRepository: SongReadRepository,
  private val songNumberWriteRepository: SongNumberWriteRepository,
  private val txRunner: TransactionRunner
) {
  suspend operator fun invoke(command: CreateSongCommand, assignments: Collection<SongNumber>): SongDetail =
    txRunner.inRwTransaction {
      // 1. Uniqueness validation per book (SongNumber constructor already validates number > 0).
      assignments
        .groupBy { it.bookId }
        .forEach { (bookId, list) ->
          val duplicates = list.groupBy { it.number }.filter { it.value.size > 1 }.keys
          require(duplicates.isEmpty()) { "duplicate numbers for book $bookId: ${duplicates.joinToString()}" }
        }

      // 2. Create song.
      songWriteRepository.create(command)

      // 3. Link song to books.
      assignments.forEach { sn ->
        songNumberWriteRepository.create(sn.bookId, SongNumberLink(command.id, sn.number))
      }

      // 4. Read back detail.
      songReadRepository.get(command.id)
        ?: error("Song created but not found: ${command.id}")
    }
}
