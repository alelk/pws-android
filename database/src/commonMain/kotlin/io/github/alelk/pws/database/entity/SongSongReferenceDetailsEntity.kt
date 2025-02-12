package io.github.alelk.pws.database.entity

import io.github.alelk.pws.domain.model.BookId
import io.github.alelk.pws.domain.model.SongId

/** Reference between two songs. */
data class SongSongReferenceDetailsEntity(
  val songId: SongId,
  val refSongId: SongId,
  val refReason: SongRefReason,
  val volume: Int,
  val refSongName: String,
  val refSongNumber: Int,
  val refSongNumberBookId: BookId,
  val refSongNumberBookDisplayName: String,
)