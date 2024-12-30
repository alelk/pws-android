package io.github.alelk.pws.domain.model

@JvmInline
value class Person(val name: String) {
  init {
    require(name.isNotBlank()) { "person name is blank" }
  }
}
