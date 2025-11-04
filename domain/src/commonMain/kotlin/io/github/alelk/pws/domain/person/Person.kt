package io.github.alelk.pws.domain.person

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@JvmInline
@Serializable(with = PersonSerializer::class)
value class Person(val name: String) {
  init {
    require(name.isNotBlank()) { "person name is blank" }
  }
}