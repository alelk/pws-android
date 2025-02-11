package io.github.alelk.pws.domain.model

data class Timestamp(val date: Date, val time: Time) {
  val year: Year get() = date.year
  val month: Month get() = date.month
  val dayOfMonth: Int get() = date.dayOfMonth
  val hours: Int get() = time.hours
  val minutes: Int get() = time.minutes
  val seconds: Int get() = time.seconds

  override fun toString(): String = "$date $time"

  companion object {
    const val FORMAT = "yyyy-MM-dd HH:mm:ss"

    fun parse(timestamp: String): Timestamp {
      val (d, t) = timestamp.split(' ').also { require(it.size == 2) { "invalid timestamp: '$timestamp', expected format: $FORMAT" } }
      return kotlin.runCatching {
        Timestamp(Date.parse(d), Time.parse(t))
      }.getOrElse { throw IllegalArgumentException("invalid timestamp '$timestamp': ${it.message}", it) }
    }
  }
}

expect fun Timestamp.Companion.now(): Timestamp

