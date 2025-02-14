package io.github.alelk.pws.backup.model

import io.github.alelk.pws.backup.model.serialization.BookIdSerializer
import io.github.alelk.pws.domain.model.BookId
import kotlinx.serialization.Serializable

@Serializable
data class SongNumber(
  @Serializable(with = BookIdSerializer::class)
  val bookId: BookId,
  val number: Int
) {
  init {
    require(number > 0) { "song number should be greater than 0: $number" }
  }
}