package io.github.alelk.pws.domain.core

import io.kotest.property.Arb
import io.kotest.property.arbitrary.choice

fun Arb.Companion.reference(bibleRef: Arb<BibleRef> = Arb.bibleRef(), songRef: Arb<SongRef> = Arb.songRef()): Arb<Reference> =
  Arb.choice(bibleRef, songRef)