package io.github.alelk.pws.domain.cross.repository

import io.github.alelk.pws.domain.core.ids.BookId
import io.github.alelk.pws.domain.core.ids.SongId

/** Many-to-many association between Book and Song. */
interface BookSongAssociationReadRepository {
  suspend fun getSongs(bookId: BookId): List<SongId>
  suspend fun getBooks(songId: SongId): List<BookId>
  suspend fun isLinked(bookId: BookId, songId: SongId): Boolean
}
