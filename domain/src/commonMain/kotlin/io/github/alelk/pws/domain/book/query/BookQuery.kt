package io.github.alelk.pws.domain.book.query

import io.github.alelk.pws.domain.core.Locale

/** Query parameters for filtering books. All fields optional. */
@ConsistentCopyVisibility
data class BookQuery private constructor(
  val locale: Locale? = null,
  val minPriority: Int? = null,
  val maxPriority: Int? = null
) {
  constructor(
    locale: Locale? = null,
    enabled: Boolean? = null,
    minPriority: Int? = null,
    maxPriority: Int? = null
  ) : this(
    locale = locale,
    minPriority = if (enabled == true) 1 else minPriority,
    maxPriority = if (enabled == false) 0 else maxPriority
  )

  fun isEmpty(): Boolean = locale == null && minPriority == null && maxPriority == null

  companion object {
    val Empty = BookQuery()
  }
}