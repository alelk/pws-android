package io.github.alelk.pws.domain.core.ids

import io.kotest.property.Arb
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.az
import io.kotest.property.arbitrary.constant
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.string

fun Arb.Companion.bookId(id: Arb<String> = Arb.string(4..15, Codepoint.az())): Arb<BookId> = id.map { BookId.parse(it) }

fun Arb.Companion.bookId(id: String): Arb<BookId> = Arb.constant(BookId.parse(id))