package io.github.alelk.pws.database.common.model

import io.kotest.property.Arb
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.az
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.string

fun Arb.Companion.tagId(tagId: Arb<String> = Arb.string(4..15, Codepoint.az())): Arb<TagId> = tagId.map { it.toTagId() }