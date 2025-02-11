package io.github.alelk.pws.domain.model

import kotlin.jvm.JvmInline

@JvmInline
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
