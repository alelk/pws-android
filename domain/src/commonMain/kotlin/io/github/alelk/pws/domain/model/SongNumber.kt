package io.github.alelk.pws.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SongNumber(val bookId: BookId, val number: Int) {
  init {
    require(number > 0) { "song number should be greater than 0: $number" }
    require(number < 1_000_000) { "song number should be less than 1 000 000: $number" }
  }

  override fun toString(): String = "$bookId#$number"

  companion object {
    fun parse(string: String): SongNumber =
      kotlin.runCatching {
        val (bookId, number) = string.split('#')
        SongNumber(BookId.parse(bookId), number.toInt())
      }.getOrElse { e ->
        throw IllegalArgumentException("unable to parse song number from string '$string': expected format 'bookId#number': ${e.message}", e)
      }
  }
}