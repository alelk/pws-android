package io.github.alelk.pws.database.entity

import io.github.alelk.pws.domain.model.BookId
import io.github.alelk.pws.domain.model.SongId
import io.github.alelk.pws.domain.model.SongNumberId

data class SongSearchResultEntity(
  val songId: SongId,
  val bookId: BookId,
  val songName: String,
  val songNumber: Int,
  val bookDisplayName: String,
  val snippet: String
) {
  val songNumberId: SongNumberId get() = SongNumberId(bookId, songId)
}