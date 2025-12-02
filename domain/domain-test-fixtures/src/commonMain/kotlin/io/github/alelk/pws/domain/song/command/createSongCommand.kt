package io.github.alelk.pws.domain.song.command

import io.github.alelk.pws.domain.core.BibleRef
import io.github.alelk.pws.domain.core.Locale
import io.github.alelk.pws.domain.core.NonEmptyString
import io.github.alelk.pws.domain.core.Version
import io.github.alelk.pws.domain.core.Year
import io.github.alelk.pws.domain.core.bibleRef
import io.github.alelk.pws.domain.core.ids.SongId
import io.github.alelk.pws.domain.core.ids.songId
import io.github.alelk.pws.domain.core.nonEmptyString
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
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.orNull

fun Arb.Companion.createSongCommand(
  id: Arb<SongId> = Arb.songId(),
  locale: Arb<Locale> = Arb.of(Locale.EN, Locale.RU, Locale.UK),
  name: Arb<NonEmptyString> = Arb.nonEmptyString(1..40),
  lyric: Arb<Pair<Lyric, Locale>> = Arb.lyric(),
  version: Arb<Version> = Arb.version(),
  author: Arb<Person?> = Arb.person().orNull(0.5),
  translator: Arb<Person?> = Arb.person().orNull(0.7),
  composer: Arb<Person?> = Arb.person().orNull(0.7),
  tonalities: Arb<List<Tonality>?> = Arb.list(Arb.tonality(), 0..3).orNull(0.5),
  year: Arb<Year?> = Arb.year().orNull(0.6),
  bibleRef: Arb<BibleRef?> = Arb.bibleRef().orNull(0.8),
  edited: Arb<Boolean> = Arb.boolean()
): Arb<CreateSongCommand> = arbitrary {
  val (lyricValue, _) = lyric.bind()
  CreateSongCommand(
    id = id.bind(),
    locale = locale.bind(),
    name = name.bind(),
    lyric = lyricValue,
    version = version.bind(),
    author = author.bind(),
    translator = translator.bind(),
    composer = composer.bind(),
    tonalities = tonalities.bind(),
    year = year.bind(),
    bibleRef = bibleRef.bind(),
    edited = edited.bind(),
  )
}
