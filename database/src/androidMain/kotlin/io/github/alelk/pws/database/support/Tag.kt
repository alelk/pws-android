package io.github.alelk.pws.database.support

import io.github.alelk.pws.domain.model.BookId
import io.github.alelk.pws.domain.model.Color
import io.github.alelk.pws.domain.model.TagId

data class Tag(
  val id: TagId,
  val name: String,
  val color: Color,
  val predefined: Boolean,
  val songNumbers: Map<BookId, Set<Int>> = emptyMap()
) {
  init {
    require(name.isNotBlank()) { "tag $id name should not be blank" }
    songNumbers.forEach { (bookId, numbers) ->
      numbers.forEach { require(it > 0) { "invalid song number $it for book $bookId" } }
    }
  }
}