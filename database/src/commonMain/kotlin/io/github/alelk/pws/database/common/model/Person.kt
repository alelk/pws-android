package io.github.alelk.pws.database.common.model

@JvmInline
value class Person(val name: String) {
  init {
    require(name.isNotBlank()) { "person name is blank" }
  }
}
