package io.github.alelk.pws.domain.songnumber.repository

import io.github.alelk.pws.domain.core.ids.BookId
import io.github.alelk.pws.domain.core.ids.SongId
import io.github.alelk.pws.domain.songnumber.model.SongNumberLink

/**
 * Many-to-many association between Book and Song with an extra ordinal (song number) within the book.
 * Invariants per book:
 *  - (bookId, songId) is unique.
 *  - (bookId, number) is unique.
 *  - number > 0 (1-based indexing).
 *
 * Implementations MUST ensure atomicity of each write operation for a single book.
 */
interface SongNumberWriteRepository {

  /** Link a song to a book with a specific number. */
  suspend fun create(bookId: BookId, link: SongNumberLink)

  /** Change the song's number within the book. */
  suspend fun update(bookId: BookId, link: SongNumberLink)

  /** Remove association. */
  suspend fun delete(bookId: BookId, songId: SongId)

  /**
   * Replace all existing associations for a book with the provided set.
   * Any missing songs are removed; any new song numbers are inserted.
   * MUST be applied atomically.
   */
  suspend fun updateAll(bookId: BookId, assignments: Collection<SongNumberLink>)
}