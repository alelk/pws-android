package io.github.alelk.pws.domain.core

import io.kotest.property.Arb
import io.kotest.property.arbitrary.*

fun Arb.Companion.songRefReason(): Arb<SongRefReason> = Arb.enum<SongRefReason>()





