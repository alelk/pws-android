package io.github.alelk.pws.domain.book.query

import io.github.alelk.pws.domain.core.Locale

/** Query parameters for filtering books. All fields optional. */
data class BookQuery(
  val locale: Locale? = null,
  val enabled: Boolean? = null,
  val minPriority: Int? = null,
  val maxPriority: Int? = null
) {
  init {
    if (maxPriority != null && maxPriority <= 0) require(enabled == null || !enabled) { "enabled must be null or false when maxPriority is <= 0" }
    if (minPriority != null && minPriority > 0) require(enabled == null || enabled) { "enabled must be null or true when minPriority is > 0" }
  }

  fun isEmpty(): Boolean = locale == null && enabled == null && minPriority == null && maxPriority == null

  fun normalize(): BookQuery = copy(minPriority = if (enabled == true) 1 else minPriority, maxPriority = if (enabled == false) 0 else maxPriority)

  companion object {
    val Empty = BookQuery()
  }
}