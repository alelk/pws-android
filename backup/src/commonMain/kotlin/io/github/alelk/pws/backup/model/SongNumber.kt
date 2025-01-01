package io.github.alelk.pws.backup.model

import io.github.alelk.pws.backup.model.serialization.BookExternalIdSerializer
import io.github.alelk.pws.domain.model.BookExternalId
import kotlinx.serialization.Serializable

@Serializable
data class SongNumber(
  @Serializable(with = BookExternalIdSerializer::class)
  val bookId: BookExternalId,
  val number: Int
) {
  init {
    require(number > 0) { "song number should be greater than 0: $number" }
  }
}