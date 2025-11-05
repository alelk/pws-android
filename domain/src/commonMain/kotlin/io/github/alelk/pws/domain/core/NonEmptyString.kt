package io.github.alelk.pws.domain.core

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@JvmInline
@Serializable(with = NonEmptyStringSerializer::class)
value class NonEmptyString(val value: String) {
  init { require(value.isNotBlank()) { "String is blank" } }
  override fun toString() = value
}