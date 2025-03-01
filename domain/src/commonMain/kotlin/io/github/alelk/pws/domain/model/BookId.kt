package io.github.alelk.pws.domain.model

import io.github.alelk.pws.domain.model.serialization.DefaultBookIdSerializer
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@JvmInline
@Serializable(with = DefaultBookIdSerializer::class)
value class BookId private constructor(val identifier: String) {

  init {
    require(pattern.matches(identifier)) {
      "book id should contain only letters, digits and '-', '_' symbols; should not start with digit; should not end with '-' or '_'"
    }
  }

  companion object {
    val pattern = Regex("""^\p{L}+([\p{L}\d_-]*[\p{L}\d])?$""")

    fun parse(identifier: String): BookId = BookId(identifier)
  }

  override fun toString(): String = identifier
}
