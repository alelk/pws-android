package io.github.alelk.pws.domain.song.command

import io.github.alelk.pws.domain.core.ids.SongId

sealed interface UpdateSongResult {
  data class Success(val songId: SongId) : UpdateSongResult
  data class NotFound(val songId: SongId) : UpdateSongResult
  data class ValidationError(val message: String) : UpdateSongResult
  data class UnknownError(val message: String, val exception: Throwable? = null) : UpdateSongResult
}