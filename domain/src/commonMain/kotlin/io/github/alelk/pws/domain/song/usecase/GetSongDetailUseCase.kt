package io.github.alelk.pws.domain.song.usecase

import io.github.alelk.pws.domain.core.ids.SongId
import io.github.alelk.pws.domain.core.transaction.TransactionRunner
import io.github.alelk.pws.domain.song.model.SongDetail
import io.github.alelk.pws.domain.song.repository.SongReadRepository

/**
 * Read use case: fetch a single SongDetail by id inside a read-only transaction.
 * Returns null if the song does not exist.
 */
class GetSongDetailUseCase(
  private val readRepository: SongReadRepository,
  private val txRunner: TransactionRunner
) {
  suspend operator fun invoke(id: SongId): SongDetail? =
    txRunner.inRoTransaction { readRepository.get(id) }
}
