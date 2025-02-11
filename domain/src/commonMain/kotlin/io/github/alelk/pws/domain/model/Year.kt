package io.github.alelk.pws.domain.model

import kotlin.jvm.JvmInline

@JvmInline
value class Year(private val value: Int) {
  init {
    require(value < 2999) { "invalid year: $value" }
    require(value > 0) { "invalid year $value" }
  }

  override fun toString(): String = value.toString().padStart(4, '0')

  companion object {
    const val FORMAT = "yyyy"
    fun parse(year: String) = Year(requireNotNull(year.dropWhile { it == '0' }.toIntOrNull()) { "invalid year: $year" })
    fun now() = Timestamp.now().year
  }
}
