package io.github.alelk.pws.domain.core

import io.kotest.property.Arb
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.az
import io.kotest.property.arbitrary.string

fun Arb.Companion.bibleRef(): Arb<BibleRef> = arbitrary { BibleRef(text = Arb.string(5..100, Codepoint.az()).bind()) }