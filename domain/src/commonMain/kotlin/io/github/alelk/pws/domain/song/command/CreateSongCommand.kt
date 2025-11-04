package io.github.alelk.pws.domain.song.command

import io.github.alelk.pws.domain.bible.BibleRef
import io.github.alelk.pws.domain.core.Locale
import io.github.alelk.pws.domain.core.NonEmptyString
import io.github.alelk.pws.domain.core.Version
import io.github.alelk.pws.domain.core.Year
import io.github.alelk.pws.domain.core.ids.BookId
import io.github.alelk.pws.domain.core.ids.SongId
import io.github.alelk.pws.domain.core.ids.TagId
import io.github.alelk.pws.domain.person.Person
import io.github.alelk.pws.domain.cross.projection.SongReferenceWithSongAndBook
import io.github.alelk.pws.domain.song.lyric.Lyric
import io.github.alelk.pws.domain.tonality.Tonality

/** Command to create a Song. **/
data class CreateSongCommand(
  val id: SongId,
  val bookId: BookId,
  val version: Version,
  val locale: Locale,
  val name: NonEmptyString,
  val lyric: Lyric,
  val author: Person?,
  val translator: Person? = null,
  val composer: Person? = null,
  val tonalities: List<Tonality>? = null,
  val tags: List<TagId> = emptyList(),
  val year: Year? = null,
  val bibleRef: BibleRef? = null,
  val songReferences: List<SongReferenceWithSongAndBook> = emptyList(),
  val edited: Boolean = false,
)