package io.github.alelk.pws.domain.model

import io.github.alelk.pws.domain.model.serialization.DefaultColorSerializer
import kotlinx.serialization.Serializable

@Serializable(with = DefaultColorSerializer::class)
data class Color(val r: Int, val g: Int, val b: Int) {
  init {
    require(r in 0..255) { "invalid color: red must be between 0 and 255" }
    require(g in 0..255) { "invalid color: green must be between 0 and 255" }
    require(b in 0..255) { "invalid color: blue must be between 0 and 255" }
  }

  override fun toString(): String {
    fun Int.hex() = toString(16).padStart(2, '0')
    return "#${r.hex()}${g.hex()}${b.hex()}"
  }

  companion object {
    val colorPattern = Regex("^#([0-9A-Fa-f]{6})$")

    @OptIn(ExperimentalStdlibApi::class)
    fun parse(hex: String): Color =
      colorPattern.find(hex)?.groups?.let {
        val hexValue = checkNotNull(it[1]) { "invalid hex value: ${it[0]}" }.value
        val r = hexValue.substring(0..1).hexToInt()
        val g = hexValue.substring(2..3).hexToInt()
        val b = hexValue.substring(4..5).hexToInt()
        Color(r, g, b)
      } ?: throw IllegalArgumentException("invalid color: '$hex', expected pattern $colorPattern")
  }
}