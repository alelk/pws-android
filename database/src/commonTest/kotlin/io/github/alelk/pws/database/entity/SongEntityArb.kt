package io.github.alelk.pws.database.entity

import io.github.alelk.pws.domain.locale
import io.github.alelk.pws.domain.model.Person
import io.github.alelk.pws.domain.model.Tonality
import io.github.alelk.pws.domain.model.Version
import io.github.alelk.pws.domain.model.Year
import io.github.alelk.pws.domain.model.person
import io.github.alelk.pws.domain.model.tonality
import io.github.alelk.pws.domain.model.version
import io.github.alelk.pws.domain.model.year
import io.kotest.property.Arb
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.az
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.orNull
import io.kotest.property.arbitrary.string
import java.util.Locale

fun Arb.Companion.songEntity(
  id: Arb<Long> = Arb.long(min = 1, max = 1_000_000),
  version: Arb<Version> = Arb.version(),
  locale: Arb<Locale> = Arb.locale(),
  name: Arb<String> = Arb.string(5..40, Codepoint.az()),
  text: Arb<String> = Arb.string(10..500, Codepoint.az()),
  author: Arb<Person?> = Arb.person().orNull(),
  translator: Arb<Person?> = Arb.person().orNull(),
  composer: Arb<Person?> = Arb.person().orNull(),
  tonalities: Arb<List<Tonality>?> = Arb.list(Arb.tonality(), 0..3).orNull(),
  year: Arb<Year?> = Arb.year().orNull(),
  bibleRef: Arb<String?> = Arb.string(5..50, Codepoint.az()).orNull()
): Arb<SongEntity> = arbitrary {
  SongEntity(
    id = id.bind(),
    version = version.bind(),
    locale = locale.bind(),
    name = name.bind(),
    lyric = text.bind(),
    author = author.bind(),
    translator = translator.bind(),
    composer = composer.bind(),
    tonalities = tonalities.bind(),
    year = year.bind(),
    bibleRef = bibleRef.bind()
  )
}