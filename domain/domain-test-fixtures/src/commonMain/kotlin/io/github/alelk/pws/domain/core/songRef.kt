package io.github.alelk.pws.domain.core

import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.int

fun Arb.Companion.songRef(
  reason: Arb<SongRefReason> = Arb.songRefReason(),
  songNumber: Arb<SongNumber> = Arb.songNumber(),
  volume: Arb<Int> = Arb.int(min = 1, max = 100)
): Arb<SongRef> =
  arbitrary { SongRef(reason = reason.bind(), number = songNumber.bind(), volume = volume.bind()) }