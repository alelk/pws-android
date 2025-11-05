package io.github.alelk.pws.domain.song.lyric

sealed interface LyricPart {
  val numbers: Set<Int>
  val text: String
}

internal fun LyricPart.validate() {
  numbers.forEach {
    require(it > 0) { "lyric part number should be greater than 0: $it" }
  }
  require(text.isNotBlank()) { "lyric part text should not be blank" }
}

data class Chorus(override val numbers: Set<Int>, override val text: String) : LyricPart {
  init {
    validate()
  }
}

data class Verse(override val numbers: Set<Int>, override val text: String) : LyricPart {
  init {
    validate()
  }
}

data class Bridge(override val numbers: Set<Int>, override val text: String) : LyricPart {
  init {
    validate()
  }
}

fun LyricPart.withNumbers(numbers: Set<Int>): LyricPart =
  when (this) {
    is Bridge -> this.copy(numbers = this.numbers + numbers)
    is Chorus -> this.copy(numbers = this.numbers + numbers)
    is Verse -> this.copy(numbers = this.numbers + numbers)
  }