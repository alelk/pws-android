package io.github.alelk.pws.domain.cross.projection

import io.github.alelk.pws.domain.book.model.BookDetail
import io.github.alelk.pws.domain.song.model.SongSummary

data class BookWithSongs(
  val book: BookDetail,
  val songs: Map<Int, SongSummary>
)
