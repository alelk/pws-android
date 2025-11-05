package io.github.alelk.pws.domain.bookstatistic.query

/** Query parameters for filtering book statistics. */
@ConsistentCopyVisibility
 data class BookStatisticQuery private constructor(
  val minPriority: Int? = null,
  val maxPriority: Int? = null,
) {
  constructor(
    enabled: Boolean? = null,
    minPriority: Int? = null,
    maxPriority: Int? = null,
  ) : this(
    minPriority = if (enabled == true) 1 else minPriority,
    maxPriority = if (enabled == false) 0 else maxPriority,
  )

  fun isEmpty(): Boolean = minPriority == null && maxPriority == null

  companion object {
    val Empty = BookStatisticQuery()
  }
}