package io.github.alelk.pws.domain.song.usecase

import io.github.alelk.pws.domain.core.transaction.TransactionRunner
import io.github.alelk.pws.domain.song.command.UpdateSongCommand
import io.github.alelk.pws.domain.song.command.UpdateSongResult
import io.github.alelk.pws.domain.song.repository.SongWriteRepository

/** Use case: update a song (patch semantics). */
class UpdateSongUseCase(
  private val writeRepository: SongWriteRepository,
  private val txRunner: TransactionRunner
) {
  suspend operator fun invoke(command: UpdateSongCommand): UpdateSongResult =
    txRunner.inRwTransaction {
      writeRepository.update(command)
    }
}

