package io.github.alelk.pws.domain.cross.repository

import io.github.alelk.pws.domain.core.ids.BookId
import io.github.alelk.pws.domain.core.ids.SongId

/**
 * Many-to-many association between Book and Song.
 * Pure link management; no aggregate field mutation.
 */
interface BookSongAssociationWriteRepository {
  suspend fun link(bookId: BookId, songId: SongId): Boolean
  suspend fun unlink(bookId: BookId, songId: SongId): Boolean
  suspend fun replaceBookSongs(bookId: BookId, songs: Collection<SongId>)
}