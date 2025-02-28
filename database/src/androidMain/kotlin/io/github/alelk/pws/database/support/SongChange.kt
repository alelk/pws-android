package io.github.alelk.pws.database.support

import io.github.alelk.pws.domain.model.BibleRef
import io.github.alelk.pws.domain.model.SongNumber
import io.github.alelk.pws.domain.model.Tonality

/** Changes of edited song. */
data class SongChange(
  val number: SongNumber,
  val lyric: String,
  val tonalities: List<Tonality>? = null,
  val bibleRef: BibleRef? = null,
)
