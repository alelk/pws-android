package io.github.alelk.pws.backup.model

import kotlin.jvm.JvmInline

@JvmInline
value class Lyric(val text: String) {
  init {
    require(text.isNotBlank()) { "lyric text is blank" }
  }
}
