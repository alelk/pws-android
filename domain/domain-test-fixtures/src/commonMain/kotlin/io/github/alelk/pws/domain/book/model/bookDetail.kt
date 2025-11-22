package io.github.alelk.pws.domain.book.model

import io.github.alelk.pws.domain.core.Locale
import io.github.alelk.pws.domain.core.NonEmptyString
import io.github.alelk.pws.domain.core.Version
import io.github.alelk.pws.domain.core.Year
import io.github.alelk.pws.domain.core.ids.BookId
import io.github.alelk.pws.domain.core.ids.SongNumberId
import io.github.alelk.pws.domain.person.Person
import io.github.alelk.pws.domain.core.year
import io.github.alelk.pws.domain.core.version
import io.github.alelk.pws.domain.core.locale
import io.github.alelk.pws.domain.core.ids.bookId
import io.github.alelk.pws.domain.core.ids.songNumberId
import io.github.alelk.pws.domain.person.person
import io.github.alelk.pws.domain.core.nonEmptyString
import io.kotest.property.Arb
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.orNull
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.az
import io.kotest.property.arbitrary.string

fun Arb.Companion.bookDetail(
  id: Arb<BookId> = Arb.bookId(),
  version: Arb<Version> = Arb.version(),
  locale: Arb<Locale> = Arb.locale(),
  name: Arb<NonEmptyString> = Arb.nonEmptyString(1..25),
  displayShortName: Arb<NonEmptyString> = Arb.nonEmptyString(1..7),
  displayName: Arb<NonEmptyString> = name,
  releaseDate: Arb<Year?> = Arb.year(1900, 2099).orNull(0.4),
  authors: Arb<List<Person>?> = Arb.list(Arb.person(), 0..3).orNull(0.5),
  creators: Arb<List<Person>?> = Arb.list(Arb.person(), 0..3).orNull(0.5),
  reviewers: Arb<List<Person>?> = Arb.list(Arb.person(), 0..3).orNull(0.5),
  editors: Arb<List<Person>?> = Arb.list(Arb.person(), 0..3).orNull(0.5),
  description: Arb<String?> = Arb.string(1..200, Codepoint.az()).orNull(0.5),
  preface: Arb<String?> = Arb.string(1..200, Codepoint.az()).orNull(0.5),
  firstSongNumberId: Arb<SongNumberId?> = Arb.songNumberId().orNull(0.6),
  countSongs: Arb<Int> = Arb.int(0..100),
  enabled: Arb<Boolean> = Arb.boolean(),
  priority: Arb<Int> = Arb.int(0..20)
): Arb<BookDetail> =
  arbitrary {
    val enabledValue = enabled.bind()
    val firstSong = firstSongNumberId.bind()
    val rawCount = countSongs.bind()
    val adjustedCount = when {
      firstSong == null -> 0
      rawCount == 0 -> 1
      else -> rawCount
    }
    BookDetail(
      id = id.bind(),
      version = version.bind(),
      locale = locale.bind(),
      name = name.bind(),
      displayShortName = displayShortName.bind(),
      displayName = displayName.bind(),
      releaseDate = releaseDate.bind(),
      authors = authors.bind(),
      creators = creators.bind(),
      reviewers = reviewers.bind(),
      editors = editors.bind(),
      description = description.bind(),
      preface = preface.bind(),
      firstSongNumberId = firstSong,
      countSongs = adjustedCount,
      enabled = enabledValue,
      priority = priority.bind().let { p -> if (enabledValue && p == 0) 1 else p }
    )
  }
