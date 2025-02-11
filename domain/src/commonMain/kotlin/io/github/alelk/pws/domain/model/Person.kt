package io.github.alelk.pws.domain.model

import kotlin.jvm.JvmInline

@JvmInline
value class Person(val name: String) {
  init {
    require(name.isNotBlank()) { "person name is blank" }
  }
}
