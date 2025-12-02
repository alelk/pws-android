package io.github.alelk.pws.domain.core.pagination

data class Page<T, K: Any>(val items: List<T>, val nextKey: K?) {
  val isLast: Boolean get() = nextKey == null
}