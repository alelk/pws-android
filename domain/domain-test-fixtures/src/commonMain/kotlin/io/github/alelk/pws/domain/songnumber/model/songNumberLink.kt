package io.github.alelk.pws.domain.songnumber.model

import io.github.alelk.pws.domain.core.ids.SongId
import io.github.alelk.pws.domain.core.ids.songId
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.int

fun Arb.Companion.songNumberLink(
  songId: Arb<SongId> = Arb.songId(),
  number: Arb<Int> = Arb.int(1, 10_000)
) =
  arbitrary { SongNumberLink(songId = songId.bind(), number = number.bind()) }