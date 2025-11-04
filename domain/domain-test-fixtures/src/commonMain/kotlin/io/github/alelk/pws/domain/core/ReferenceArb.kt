package io.github.alelk.pws.domain.core

import io.kotest.property.Arb
import io.kotest.property.arbitrary.*

fun Arb.Companion.songRefReason(): Arb<SongRefReason> = Arb.enum<SongRefReason>()

fun Arb.Companion.bibleRef(): Arb<BibleRef> = arbitrary { BibleRef(text = Arb.string(5..100, Codepoint.az()).bind()) }

fun Arb.Companion.songRef(
  reason: Arb<SongRefReason> = Arb.songRefReason(),
  songNumber: Arb<SongNumber> = Arb.songNumber(),
  volume: Arb<Int> = Arb.int(min = 1, max = 100)
): Arb<SongRef> =
  arbitrary { SongRef(reason = reason.bind(), number = songNumber.bind(), volume = volume.bind()) }

fun Arb.Companion.reference(bibleRef: Arb<BibleRef> = Arb.bibleRef(), songRef: Arb<SongRef> = Arb.songRef()): Arb<Reference> =
  Arb.choice(bibleRef, songRef)