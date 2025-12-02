package io.github.alelk.pws.domain.songnumber.repository

import io.github.alelk.pws.domain.core.SongNumber
import io.github.alelk.pws.domain.core.ids.BookId
import io.github.alelk.pws.domain.core.ids.SongId
import io.github.alelk.pws.domain.songnumber.model.SongNumberLink

/** Many-to-many association between Book and Song. */
interface SongNumberReadRepository {
  suspend fun getAllByBookId(bookId: BookId): List<SongNumberLink>
  suspend fun getAllBySongId(songId: SongId): List<SongNumber>
  suspend fun get(bookId: BookId, songId: SongId): SongNumber?
  suspend fun get(link: SongNumberLink): SongNumber?
  suspend fun get(link: SongNumber): SongNumberLink?
  suspend fun count(bookId: BookId): Int
}