package io.github.alelk.pws.domain.tonality

import io.kotest.property.Arb
import io.kotest.property.arbitrary.enum

fun Arb.Companion.tonality() = Arb.enum<Tonality>()