package io.github.alelk.pws.domain.book.model

import io.github.alelk.pws.domain.core.Locale
import io.github.alelk.pws.domain.core.NonEmptyString
import io.github.alelk.pws.domain.core.Version
import io.github.alelk.pws.domain.core.ids.BookId
import io.github.alelk.pws.domain.core.ids.SongNumberId
import io.github.alelk.pws.domain.core.ids.bookId
import io.github.alelk.pws.domain.core.ids.songNumberId
import io.github.alelk.pws.domain.core.nonEmptyString
import io.github.alelk.pws.domain.core.version
import io.github.alelk.pws.domain.core.locale
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.orNull

fun Arb.Companion.bookSummary(
  id: Arb<BookId> = Arb.bookId(),
  version: Arb<Version> = Arb.version(),
  locale: Arb<Locale> = Arb.locale(),
  name: Arb<NonEmptyString> = Arb.nonEmptyString(1..25),
  displayShortName: Arb<NonEmptyString> = Arb.nonEmptyString(1..7),
  displayName: Arb<NonEmptyString> = name,
  countSongs: Arb<Int> = Arb.int(0..100),
  firstSongNumberId: Arb<SongNumberId?> = Arb.songNumberId().orNull(0.6),
  enabled: Arb<Boolean> = Arb.boolean(),
  priority: Arb<Int> = Arb.int(0..20)
): Arb<BookSummary> =
  arbitrary {
    val enabledValue = enabled.bind()
    val firstSong = firstSongNumberId.bind()
    val rawCount = countSongs.bind()
    val adjustedCount = when {
      firstSong == null -> 0
      rawCount == 0 -> 1
      else -> rawCount
    }
    BookSummary(
      id = id.bind(),
      version = version.bind(),
      locale = locale.bind(),
      name = name.bind(),
      displayShortName = displayShortName.bind(),
      displayName = displayName.bind(),
      countSongs = adjustedCount,
      firstSongNumberId = firstSong,
      enabled = enabledValue,
      priority = priority.bind().let { p -> if (enabledValue && p == 0) 1 else p }
    )
  }

