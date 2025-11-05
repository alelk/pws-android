package io.github.alelk.pws.backup.model

import io.github.alelk.pws.domain.core.ids.BookId
import kotlinx.serialization.Serializable

@Serializable
data class SongNumber(val bookId: BookId, val number: Int) {
  init {
    require(number > 0) { "song number should be greater than 0: $number" }
  }
}