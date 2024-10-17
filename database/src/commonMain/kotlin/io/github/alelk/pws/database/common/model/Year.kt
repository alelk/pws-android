package io.github.alelk.pws.database.common.model

@JvmInline
value class Year(private val value: Int) {
  init {
    require(value < 2999) { "invalid year: $value" }
    require(value > 0) { "invalid year $value" }
  }

  override fun toString(): String = value.toString()

  companion object {
    fun parse(year: String) = Year(year.toInt())
  }
}
