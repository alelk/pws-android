package io.github.alelk.pws.database.song

import io.github.alelk.pws.domain.core.ids.BookId
import io.github.alelk.pws.domain.core.ids.SongId
import io.github.alelk.pws.domain.core.ids.SongNumberId

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