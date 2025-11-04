package io.github.alelk.pws.domain.book.query

import io.github.alelk.pws.domain.core.Locale

/** Query parameters for filtering books. All fields optional. */
data class BookQuery(
    val locale: Locale? = null,
    val enabled: Boolean? = null,
    val minPriority: Int? = null
) {
  fun isEmpty(): Boolean = locale == null && enabled == null && minPriority == null

  fun normalize(): BookQuery = copy()

  companion object {
    val Empty = BookQuery()
  }
}