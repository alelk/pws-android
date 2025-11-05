package io.github.alelk.pws.features.usecase

import io.github.alelk.pws.domain.core.ids.BookId
import io.github.alelk.pws.domain.song.model.SongSummary
import io.github.alelk.pws.domain.song.repository.SongRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Temporary until domain provides a proper use case. */
class ObserveSongsInBookUseCaseTemp(private val repository: SongRepository) {
  operator fun invoke(bookId: BookId): Flow<List<Pair<Int, SongSummary>>> =
    repository.observeAllInBook(bookId).map { map -> map.toSortedMap().map { (num, summary) -> num to summary } }
  // TODO remove when domain adds a use case.
}

