package io.github.alelk.pws.domain.songnumber.repository

import io.github.alelk.pws.domain.core.ids.BookId
import io.github.alelk.pws.domain.core.ids.SongId
import io.github.alelk.pws.domain.core.ids.SongNumberId
import io.github.alelk.pws.domain.core.result.CreateResourceResult
import io.github.alelk.pws.domain.core.result.DeleteResourceResult
import io.github.alelk.pws.domain.core.result.UpdateResourceResult
import io.github.alelk.pws.domain.songnumber.model.SongNumberLink

/**
 * Many-to-many association between Book and Song with an extra ordinal (song number) within the book.
 * Invariants per book:
 *  - (bookId, songId) is unique.
 *  - (bookId, number) is unique.
 *  - number > 0 (1-based indexing).
 */
interface SongNumberWriteRepository {

  /** Link a song to a book with a specific number. */
  suspend fun create(bookId: BookId, link: SongNumberLink): CreateResourceResult<SongNumberLink>

  /** Change the song's number within the book. */
  suspend fun update(bookId: BookId, link: SongNumberLink): UpdateResourceResult<SongNumberLink>

  /** Remove association. */
  suspend fun delete(bookId: BookId, songId: SongId): DeleteResourceResult<SongNumberId>
}