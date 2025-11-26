package io.github.alelk.pws.domain.song.usecase

import io.github.alelk.pws.domain.core.ids.SongId
import io.github.alelk.pws.domain.core.result.CreateResourceResult
import io.github.alelk.pws.domain.core.transaction.TransactionRunner
import io.github.alelk.pws.domain.song.command.CreateSongCommand
import io.github.alelk.pws.domain.song.repository.SongWriteRepository

class CreateSongUseCase(
  private val writeRepository: SongWriteRepository,
  private val txRunner: TransactionRunner
) {
  suspend operator fun invoke(command: CreateSongCommand): CreateResourceResult<SongId> =
    txRunner.inRwTransaction {
      writeRepository.create(command)
    }
}