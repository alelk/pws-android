package io.github.alelk.pws.backup.model

import io.github.alelk.pws.backup.model.serialization.BibleRefSerializer
import io.github.alelk.pws.backup.model.serialization.LocaleSerializer
import io.github.alelk.pws.backup.model.serialization.PersonSerializer
import io.github.alelk.pws.backup.model.serialization.TonalitySerializer
import io.github.alelk.pws.backup.model.serialization.VersionSerializer
import io.github.alelk.pws.domain.model.BibleRef
import io.github.alelk.pws.domain.model.Person
import io.github.alelk.pws.domain.model.Tonality
import io.github.alelk.pws.domain.model.Version
import kotlinx.serialization.Serializable
import java.util.Locale

@Serializable
data class Song(
  val number: SongNumber,
  val id: Long? = null,
  @Serializable(with = VersionSerializer::class)
  val version: Version,
  @Serializable(with = LocaleSerializer::class)
  val locale: Locale,
  val title: String,
  val lyric: String,
  val tonalities: List<@Serializable(with = TonalitySerializer::class) Tonality>? = null,
  @Serializable(with = PersonSerializer::class)
  val author: Person? = null,
  @Serializable(with = PersonSerializer::class)
  val translator: Person? = null,
  @Serializable(with = PersonSerializer::class)
  val composer: Person? = null,
  @Serializable(with = BibleRefSerializer::class)
  val bibleRef: BibleRef? = null,
)
