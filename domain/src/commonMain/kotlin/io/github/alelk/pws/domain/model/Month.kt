package io.github.alelk.pws.domain.model

enum class Month(val value: Int) {
  JANUARY(1),
  FEBRUARY(2),
  MARCH(3),
  APRIL(4),
  MAY(5),
  JUNE(6),
  JULY(7),
  AUGUST(8),
  SEPTEMBER(9),
  OCTOBER(10),
  NOVEMBER(11),
  DECEMBER(12);

  override fun toString(): String = value.toString().padStart(2, '0')

  companion object {
    const val FORMAT = "MM"
    fun of(monthNumber: Int): Month = checkNotNull(entries.firstOrNull { it.value == monthNumber }) { "invalid month (expected 01-12): $monthNumber" }
    fun parse(month: String): Month = of(requireNotNull(month.toIntOrNull()) { "invalid moth number: $month" })
    fun now() = Timestamp.now().month
  }
}