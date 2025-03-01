package io.github.alelk.pws.domain.model

import io.github.alelk.pws.domain.model.serialization.DefaultYearSerializer
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@JvmInline
@Serializable(with = DefaultYearSerializer::class)
value class Year(private val value: Int) {
  init {
    require(value < 2999) { "invalid year: $value" }
    require(value > 0) { "invalid year $value" }
  }

  override fun toString(): String = value.toString().padStart(4, '0')

  companion object {
    const val FORMAT = "yyyy"
    fun parse(year: String) = Year(requireNotNull(year.dropWhile { it == '0' }.toIntOrNull()) { "invalid year: $year" })
  }
}
