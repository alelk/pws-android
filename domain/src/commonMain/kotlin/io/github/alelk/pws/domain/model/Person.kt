package io.github.alelk.pws.domain.model

import io.github.alelk.pws.domain.model.serialization.DefaultPersonSerializer
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@JvmInline
@Serializable(with = DefaultPersonSerializer::class)
value class Person(val name: String) {
  init {
    require(name.isNotBlank()) { "person name is blank" }
  }
}
