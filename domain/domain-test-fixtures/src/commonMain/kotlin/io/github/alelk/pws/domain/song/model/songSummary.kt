package io.github.alelk.pws.domain.song.model

import io.github.alelk.pws.domain.core.Locale
import io.github.alelk.pws.domain.core.NonEmptyString
import io.github.alelk.pws.domain.core.Version
import io.github.alelk.pws.domain.core.ids.SongId
import io.github.alelk.pws.domain.core.ids.songId
import io.github.alelk.pws.domain.core.locale
import io.github.alelk.pws.domain.core.nonEmptyString
import io.github.alelk.pws.domain.core.version
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.boolean

fun Arb.Companion.songSummary(
  id: Arb<SongId> = Arb.songId(),
  version: Arb<Version> = Arb.version(),
  locale: Arb<Locale> = Arb.locale(),
  name: Arb<NonEmptyString> = Arb.nonEmptyString(1..40),
  edited: Arb<Boolean> = Arb.boolean()
): Arb<SongSummary> =
  arbitrary {
    SongSummary(
      id = id.bind(),
      version = version.bind(),
      locale = locale.bind(),
      name = name.bind(),
      edited = edited.bind()
    )
  }

