package io.github.alelk.pws.backup.model

import io.github.alelk.pws.domain.bible.BibleRef
import io.github.alelk.pws.domain.core.Locale
import io.github.alelk.pws.domain.person.Person
import io.github.alelk.pws.domain.core.ids.SongId
import io.github.alelk.pws.domain.tonality.Tonality
import io.github.alelk.pws.domain.core.Version
import kotlinx.serialization.Serializable

@Serializable
data class Song(
  val number: SongNumber,
  val id: SongId? = null,
  val version: Version,
  val locale: Locale,
  val name: String,
  val lyric: String,
  val tonalities: List<Tonality>? = null,
  val author: Person? = null,
  val translator: Person? = null,
  val composer: Person? = null,
  val bibleRef: BibleRef? = null,
)
