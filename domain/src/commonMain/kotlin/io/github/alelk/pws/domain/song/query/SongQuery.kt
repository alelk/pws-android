package io.github.alelk.pws.domain.song.query

import io.github.alelk.pws.domain.core.ids.BookId

/** Query parameters for filtering songs. */
data class SongQuery(
  val bookId: BookId? = null,
  val minNumber: Int? = null,
  val maxNumber: Int? = null
) {
  init {
    require(minNumber == null || minNumber > 0) { "minNumber must be > 0" }
    require(maxNumber == null || maxNumber > 0) { "maxNumber must be > 0" }
    if (minNumber != null && maxNumber != null) require(minNumber <= maxNumber) { "minNumber must be <= maxNumber" }
  }

  fun isEmpty(): Boolean = listOf(bookId, minNumber, maxNumber).all { it == null }

  fun normalize(): SongQuery = copy()

  companion object {
    val Empty = SongQuery()
  }
}