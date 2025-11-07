package io.github.alelk.pws.domain.song.usecase

import io.github.alelk.pws.domain.core.ids.SongId
import io.github.alelk.pws.domain.song.model.SongDetail
import io.github.alelk.pws.domain.song.repository.SongRepository
import kotlinx.coroutines.flow.Flow

class ObserveSongUseCase(val songRepository: SongRepository) {
  operator fun invoke(songId: SongId): Flow<SongDetail?> = songRepository.observe(songId)
}