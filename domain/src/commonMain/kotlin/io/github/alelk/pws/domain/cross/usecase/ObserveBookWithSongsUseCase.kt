package io.github.alelk.pws.domain.cross.usecase

import io.github.alelk.pws.domain.book.repository.BookObserveRepository
import io.github.alelk.pws.domain.core.ids.BookId
import io.github.alelk.pws.domain.cross.projection.BookWithSongs
import io.github.alelk.pws.domain.song.repository.SongRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine


class ObserveBookWithSongsUseCase(
  private val bookRepository: BookObserveRepository,
  private val songRepository: SongRepository
) {
  operator fun invoke(bookId: BookId): Flow<BookWithSongs?> =
    combine(
      bookRepository.observe(bookId),
      songRepository.observeAllInBook(bookId)
    ) { book, songs -> book?.let { BookWithSongs(it, songs) } }
}