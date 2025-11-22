package io.github.alelk.pws.domain.song.repository

import io.github.alelk.pws.domain.core.ids.SongId
import io.github.alelk.pws.domain.song.model.SongDetail
import io.github.alelk.pws.domain.song.model.SongSummary
import io.github.alelk.pws.domain.song.query.SongQuery
import io.github.alelk.pws.domain.song.query.SongSort

interface SongReadRepository {
  suspend fun get(id: SongId): SongDetail?
  suspend fun getMany(query: SongQuery = SongQuery.Empty, sort: SongSort = SongSort.ById): List<SongSummary>
}

