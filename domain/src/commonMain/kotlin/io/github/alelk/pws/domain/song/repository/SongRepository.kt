package io.github.alelk.pws.domain.song.repository

import io.github.alelk.pws.domain.core.ids.BookId
import io.github.alelk.pws.domain.core.ids.SongId
import io.github.alelk.pws.domain.song.model.SongDetail
import io.github.alelk.pws.domain.song.model.SongSummary
import kotlinx.coroutines.flow.Flow

interface SongRepository {
  fun observeSong(id: SongId): Flow<SongDetail?>
  fun observeSongsInBook(bookId: BookId): Flow<Map<Int, SongSummary>>
  suspend fun get(id: SongId): SongDetail?
  suspend fun delete(id: SongId)
}