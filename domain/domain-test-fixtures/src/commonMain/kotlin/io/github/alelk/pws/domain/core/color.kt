package io.github.alelk.pws.domain.core

import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.int

fun Arb.Companion.color(
  r: Arb<Int> = Arb.int(0..255),
  g: Arb<Int> = Arb.int(0..255),
  b: Arb<Int> = Arb.int(0..255),
): Arb<Color> =
  arbitrary { Color(r.bind(), g.bind(), b.bind()) }