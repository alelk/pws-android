package io.github.alelk.pws.domain.song.usecase

import io.github.alelk.pws.domain.core.transaction.TransactionRunner
import io.github.alelk.pws.domain.song.command.CreateSongCommand
import io.github.alelk.pws.domain.song.command.CreateSongResult
import io.github.alelk.pws.domain.song.repository.SongWriteRepository

class CreateSongUseCase(
  private val writeRepository: SongWriteRepository,
  private val txRunner: TransactionRunner
) {
  suspend operator fun invoke(command: CreateSongCommand): CreateSongResult =
    txRunner.inRwTransaction {
      writeRepository.create(command)
    }
}