package io.github.alelk.pws.domain.song.command

import io.github.alelk.pws.domain.core.BibleRef
import io.github.alelk.pws.domain.core.Locale
import io.github.alelk.pws.domain.core.NonEmptyString
import io.github.alelk.pws.domain.core.OptionalField
import io.github.alelk.pws.domain.core.Version
import io.github.alelk.pws.domain.core.Year
import io.github.alelk.pws.domain.core.ids.SongId
import io.github.alelk.pws.domain.tonality.Tonality
import io.github.alelk.pws.domain.person.Person
import io.github.alelk.pws.domain.song.lyric.Lyric

/** Patch-like update for Song. */
data class UpdateSongCommand(
  val id: SongId,
  val version: Version? = null,
  val name: NonEmptyString? = null,
  val locale: Locale? = null,
  val lyric: Lyric? = null,
  val author: OptionalField<Person?> = OptionalField.Unchanged,
  val translator: OptionalField<Person?> = OptionalField.Unchanged,
  val composer: OptionalField<Person?> = OptionalField.Unchanged,
  val year: OptionalField<Year?> = OptionalField.Unchanged,
  val bibleRef: OptionalField<BibleRef?> = OptionalField.Unchanged,
  val tonalities: OptionalField<List<Tonality>?> = OptionalField.Unchanged,
  val expectVersion: Version? = null
) {

  /** Returns true if this command includes any change request (ie. not all fields are default). */
  fun hasChanges(): Boolean =
    version != null ||
      name != null ||
      locale != null ||
      lyric != null ||
      author !is OptionalField.Unchanged ||
      translator !is OptionalField.Unchanged ||
      composer !is OptionalField.Unchanged ||
      year !is OptionalField.Unchanged ||
      bibleRef !is OptionalField.Unchanged ||
      tonalities !is OptionalField.Unchanged
}
