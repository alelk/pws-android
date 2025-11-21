package io.github.alelk.pws.domain.song.command

import io.github.alelk.pws.domain.core.BibleRef
import io.github.alelk.pws.domain.core.Locale
import io.github.alelk.pws.domain.core.NonEmptyString
import io.github.alelk.pws.domain.core.OptionalField
import io.github.alelk.pws.domain.core.Version
import io.github.alelk.pws.domain.core.Year
import io.github.alelk.pws.domain.core.bibleRef
import io.github.alelk.pws.domain.core.ids.SongId
import io.github.alelk.pws.domain.core.ids.songId
import io.github.alelk.pws.domain.core.nonEmptyString
import io.github.alelk.pws.domain.core.optionalField
import io.github.alelk.pws.domain.core.version
import io.github.alelk.pws.domain.core.year
import io.github.alelk.pws.domain.person.Person
import io.github.alelk.pws.domain.person.person
import io.github.alelk.pws.domain.song.lyric.Lyric
import io.github.alelk.pws.domain.song.lyric.lyric
import io.github.alelk.pws.domain.tonality.Tonality
import io.github.alelk.pws.domain.tonality.tonality
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.orNull

fun Arb.Companion.updateSongCommand(
  id: Arb<SongId> = Arb.songId(),
  version: Arb<Version?> = Arb.version().orNull(0.5),
  name: Arb<NonEmptyString?> = Arb.nonEmptyString(1..40).orNull(0.5),
  locale: Arb<Locale?> = Arb.of(Locale.EN, Locale.RU, Locale.UK).orNull(0.6),
  lyric: Arb<Pair<Lyric, Locale>?> = Arb.lyric().orNull(0.6),
  author: Arb<OptionalField<Person?>> = Arb.optionalField(Arb.person().orNull(0.5)),
  translator: Arb<OptionalField<Person?>> = Arb.optionalField(Arb.person().orNull(0.7)),
  composer: Arb<OptionalField<Person?>> = Arb.optionalField(Arb.person().orNull(0.7)),
  year: Arb<OptionalField<Year?>> = Arb.optionalField(Arb.year().orNull(0.6)),
  bibleRef: Arb<OptionalField<BibleRef?>> = Arb.optionalField(Arb.bibleRef().orNull(0.8)),
  tonalities: Arb<OptionalField<List<Tonality>?>> = Arb.optionalField(Arb.list(Arb.tonality(), 0..3).orNull(0.5)),
  expectVersion: Arb<Version?> = Arb.version().orNull(0.7)
): Arb<UpdateSongCommand> = arbitrary {
  val lyricPair = lyric.bind()
  UpdateSongCommand(
    id = id.bind(),
    version = version.bind(),
    name = name.bind(),
    locale = locale.bind(),
    lyric = lyricPair?.first,
    author = author.bind(),
    translator = translator.bind(),
    composer = composer.bind(),
    year = year.bind(),
    bibleRef = bibleRef.bind(),
    tonalities = tonalities.bind(),
    expectVersion = expectVersion.bind(),
  )
}
