package io.github.alelk.pws.domain.model

data class Time(
  val hours: Int,
  val minutes: Int,
  val seconds: Int
) {
  init {
    require(hours in 0..23) { "invalid hours: $hours" }
    require(minutes in 0..59) { "invalid minutes: $minutes" }
    require(seconds in 0..59) { "invalid seconds: $seconds" }
  }

  override fun toString(): String = "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"

  companion object {
    const val FORMAT = "HH:mm:ss"
    fun parse(time: String): Time {
      val (h, m, s) = time.split(':').also { require(it.size == 3) { "invalid time: '$time', expected format: $FORMAT" } }
      return runCatching {
        Time(h.toInt(), m.toInt(), s.toInt())
      }.getOrElse { throw IllegalArgumentException("invalid time '$time': ${it.message}", it) }
    }

    fun now() = Timestamp.now().time
  }
}