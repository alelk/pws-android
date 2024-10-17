package io.github.alelk.pws.database.common.model

import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.int

fun Arb.Companion.version(major: Arb<Int> = Arb.int(min = 0, max = 10_000), minor: Arb<Int> = Arb.int(min = 0, max = 10_000)): Arb<Version> =
  arbitrary { Version(major.bind(), minor.bind()) }