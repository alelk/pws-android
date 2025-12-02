package io.github.alelk.pws.domain.core

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@JvmInline
@Serializable(with = YearSerializer::class)
value class Year(private val value: Int) : Comparable<Year> {
  init {
    require(value < 2999) { "invalid year: $value" }
    require(value > 0) { "invalid year $value" }
  }

  override fun compareTo(other: Year): Int = value.compareTo(other.value)

  override fun toString(): String = value.toString().padStart(4, '0')

  companion object {
    const val FORMAT = "yyyy"
    fun parse(year: String) = Year(requireNotNull(year.dropWhile { it == '0' }.toIntOrNull()) { "invalid year: $year" })
  }
}
