package io.github.alelk.pws.database.common.model

import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.map

fun Arb.Companion.year(min: Int = 1800, max: Int = 2099, range: IntRange = min..max) = Arb.int(range).map { Year(it) }