package io.github.alelk.pws.backup.model

import io.github.alelk.pws.domain.core.BibleRef
import io.github.alelk.pws.domain.core.Locale
import io.github.alelk.pws.domain.person.Person
import io.github.alelk.pws.domain.core.ids.SongId
import io.github.alelk.pws.domain.tonality.Tonality
import io.github.alelk.pws.domain.core.Version
import io.github.alelk.pws.domain.song.model.BibleRefSerializer
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
  @Serializable(with = BibleRefSerializer::class)
  val bibleRef: BibleRef? = null,
)
