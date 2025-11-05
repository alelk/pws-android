package io.github.alelk.pws.domain.tag

import io.github.alelk.pws.domain.core.Color
import io.github.alelk.pws.domain.core.ids.TagId

data class Tag(
  val id: TagId,
  val name: String,
  val priority: Int,
  val color: Color,
  val predefined: Boolean = false
) {
  init {
    require(name.isNotBlank()) { "Name must not be blank" }
  }
}