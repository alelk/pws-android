package io.github.alelk.pws.domain.song.lyric

import kotlin.jvm.JvmInline

@JvmInline
value class Lyric(private val parts: List<LyricPart>) : List<LyricPart> by parts {
  init {
    require(parts.isNotEmpty()) { "Lyric must have at least one part" }
  }
}
