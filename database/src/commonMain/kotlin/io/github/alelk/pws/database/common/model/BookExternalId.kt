package io.github.alelk.pws.database.common.model

@JvmInline
value class BookExternalId private constructor(val identifier: String) {

  init {
    require(pattern.matches(identifier)) {
      "Book external id should contain only letters, digits and '-', '_' symbols; should not start with digit; should not end with '-' or '_'"
    }
  }

  companion object {
    val pattern = Regex("""^\p{L}+([\p{L}\d_-]*[\p{L}\d])?$""")

    fun parse(identifier: String): BookExternalId = BookExternalId(identifier)
  }

  override fun toString(): String = identifier
}
