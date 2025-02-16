package io.github.alelk.pws.domain.model

import io.kotest.property.Arb
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.az
import io.kotest.property.arbitrary.string

fun Arb.Companion.bibleRef(bibleRef: Arb<String> = Arb.string(10..100, Codepoint.az())): Arb<BibleRef> = arbitrary { BibleRef(bibleRef.bind()) }