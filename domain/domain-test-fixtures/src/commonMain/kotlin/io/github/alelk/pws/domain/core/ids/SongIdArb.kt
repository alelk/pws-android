package io.github.alelk.pws.domain.core.ids

import io.kotest.property.Arb
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.map

fun Arb.Companion.songId(id: Arb<Long> = Arb.long(1..1_000_000L)): Arb<SongId> = id.map { SongId(it) }