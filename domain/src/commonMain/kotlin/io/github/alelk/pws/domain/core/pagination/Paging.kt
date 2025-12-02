package io.github.alelk.pws.domain.core.pagination

sealed interface Paging {
  data class Offset(val limit: Int, val offset: Int = 0) : Paging
  data class Keyset<K : Any>(val limit: Int, val afterKey: K?) : Paging
}