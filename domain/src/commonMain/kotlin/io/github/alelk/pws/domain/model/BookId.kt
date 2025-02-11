package io.github.alelk.pws.domain.model

import kotlin.jvm.JvmInline

@JvmInline
value class BookId private constructor(val identifier: String) {

  init {
    require(pattern.matches(identifier)) {
      "Book id should contain only letters, digits and '-', '_' symbols; should not start with digit; should not end with '-' or '_'"
    }
  }

  companion object {
    val pattern = Regex("""^\p{L}+([\p{L}\d_-]*[\p{L}\d])?$""")

    fun parse(identifier: String): BookId = BookId(identifier)
  }

  override fun toString(): String = identifier
}
