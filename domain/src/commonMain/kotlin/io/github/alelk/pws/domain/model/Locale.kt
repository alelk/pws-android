package io.github.alelk.pws.domain.model

import io.github.alelk.pws.domain.model.serialization.DefaultLocaleSerializer
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@JvmInline
@Serializable(with = DefaultLocaleSerializer::class)
value class Locale private constructor(val value: String) {
  init {
    require(value.isNotBlank()) { "locale value must not be blank" }
  }

  override fun toString() = value

  companion object {
    val EN = Locale("en")
    val RU = Locale("ru")
    val UK = Locale("uk")

    fun of(value: String): Locale = Locale(value.lowercase())
  }
}
