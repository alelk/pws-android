package io.github.alelk.pws.domain.model

data class Date(
  val year: Year,
  val month: Month,
  val dayOfMonth: Int
) {
  init {
    require(dayOfMonth in 1..31) { "invalid day of month: $dayOfMonth" }
  }

  override fun toString(): String = "${year}-${month}-${dayOfMonth.toString().padStart(2, '0')}"

  companion object {
    const val FORMAT = "yyyy-MM-dd"
    fun parse(date: String): Date {
      val (y, m, d) = date.split('-').also { require(it.size == 3) { "invalid date: '$date', expected format: $FORMAT" } }
      return runCatching {
        Date(Year.parse(y), Month.parse(m), d.dropWhile { it == '0' }.toInt())
      }.getOrElse { throw IllegalArgumentException("invalid date '$date': ${it.message}", it) }
    }

    fun now() = Timestamp.now().date
  }
}