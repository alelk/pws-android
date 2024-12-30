package io.github.alelk.pws.domain.model

import io.kotest.property.Arb
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.az
import io.kotest.property.arbitrary.constant
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.string

fun Arb.Companion.bookExternalId(externalId: Arb<String> = Arb.string(4..15, Codepoint.az())): Arb<BookExternalId> =
  externalId.map { BookExternalId.parse(it) }

fun Arb.Companion.bookExternalId(externalId: String): Arb<BookExternalId> = Arb.bookExternalId(externalId = Arb.constant(externalId))