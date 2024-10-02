package com.alelk.pws.database.model

@JvmInline
value class Person(val name: String) {
  init {
    require(name.isNotBlank()) { "person name is blank" }
  }
}
