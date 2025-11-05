package io.github.alelk.pws.domain.bookstatistic.command

import io.github.alelk.pws.domain.core.ids.BookId

/** Patch-like update for BookStatisticDetail. Any non-null field will be applied. */
data class UpdateBookStatisticCommand(
    val id: BookId,
    val priority: Int? = null,
    val readings: Int? = null,
    val rating: Int? = null,
) {
  init {
    if (priority != null) require(priority >= 0) { "priority must be >= 0" }
    if (readings != null) require(readings >= 0) { "readings must be >= 0" }
  }

  fun isEmpty(): Boolean = priority == null && readings == null && rating == null
}