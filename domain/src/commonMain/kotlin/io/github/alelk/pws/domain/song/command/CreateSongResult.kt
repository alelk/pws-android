package io.github.alelk.pws.domain.song.command

import io.github.alelk.pws.domain.core.ids.SongId

sealed interface CreateSongResult {
  data class Success(val songId: SongId) : CreateSongResult
  data class AlreadyExists(val songId: SongId) : CreateSongResult
  data class ValidationError(val message: String) : CreateSongResult
  data class UnknownError(val message: String, val exception: Throwable? = null) : CreateSongResult
}