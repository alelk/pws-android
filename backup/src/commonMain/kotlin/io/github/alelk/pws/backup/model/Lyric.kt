package io.github.alelk.pws.backup.model

@JvmInline
value class Lyric(val text: String) {
  init {
    require(text.isNotBlank()) { "lyric text is blank" }
  }
}
