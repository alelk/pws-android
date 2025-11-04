package io.github.alelk.pws.database.support

import io.github.alelk.pws.domain.bible.BibleRef
import io.github.alelk.pws.domain.core.SongNumber
import io.github.alelk.pws.domain.tonality.Tonality

/** Changes of edited song. */
data class SongChange(
  val number: SongNumber,
  val lyric: String,
  val tonalities: List<Tonality>? = null,
  val bibleRef: BibleRef? = null,
) {
  init {
    require(lyric.isNotBlank()) { "song $number lyric should not be blank" }
  }
}
