package io.github.alelk.pws.database.entity

import io.github.alelk.pws.domain.bible.BibleRef
import io.github.alelk.pws.domain.core.Locale
import io.github.alelk.pws.domain.core.locale
import io.github.alelk.pws.domain.person.Person
import io.github.alelk.pws.domain.core.ids.SongId
import io.github.alelk.pws.domain.tonality.Tonality
import io.github.alelk.pws.domain.core.Version
import io.github.alelk.pws.domain.core.Year
import io.github.alelk.pws.domain.model.bibleRef
import io.github.alelk.pws.domain.person.person
import io.github.alelk.pws.domain.core.ids.songId
import io.github.alelk.pws.domain.tonality.tonality
import io.github.alelk.pws.domain.core.version
import io.github.alelk.pws.domain.core.year
import io.kotest.property.Arb
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.az
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.orNull
import io.kotest.property.arbitrary.removeEdgecases
import io.kotest.property.arbitrary.string

fun Arb.Companion.songEntity(
  id: Arb<SongId> = Arb.songId().removeEdgecases(),
  version: Arb<Version> = Arb.version(),
  locale: Arb<Locale> = Arb.locale(),
  name: Arb<String> = Arb.string(5..40, Codepoint.az()),
  lyric: Arb<String> = Arb.string(10..500, Codepoint.az()),
  author: Arb<Person?> = Arb.person().orNull(),
  translator: Arb<Person?> = Arb.person().orNull(),
  composer: Arb<Person?> = Arb.person().orNull(),
  tonalities: Arb<List<Tonality>?> = Arb.list(Arb.tonality(), 0..3).orNull(),
  year: Arb<Year?> = Arb.year().orNull(),
  bibleRef: Arb<BibleRef?> = Arb.bibleRef().orNull(0.5)
): Arb<SongEntity> = arbitrary {
  SongEntity(
    id = id.bind(),
    version = version.bind(),
    locale = locale.bind(),
    name = name.bind(),
    lyric = lyric.bind(),
    author = author.bind(),
    translator = translator.bind(),
    composer = composer.bind(),
    tonalities = tonalities.bind(),
    year = year.bind(),
    bibleRef = bibleRef.bind()
  )
}