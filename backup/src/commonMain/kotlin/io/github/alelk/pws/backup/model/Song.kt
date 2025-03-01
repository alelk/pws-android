package io.github.alelk.pws.backup.model

import io.github.alelk.pws.domain.model.BibleRef
import io.github.alelk.pws.domain.model.Locale
import io.github.alelk.pws.domain.model.Person
import io.github.alelk.pws.domain.model.SongId
import io.github.alelk.pws.domain.model.Tonality
import io.github.alelk.pws.domain.model.Version
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
