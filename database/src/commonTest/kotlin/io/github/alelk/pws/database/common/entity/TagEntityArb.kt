package io.github.alelk.pws.database.common.entity

import io.github.alelk.pws.domain.model.Color
import io.github.alelk.pws.domain.model.TagId
import io.github.alelk.pws.domain.model.tagId
import io.github.alelk.pws.domain.model.color
import io.kotest.property.Arb
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.az
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string

fun Arb.Companion.tagEntity(
    id: Arb<TagId> = Arb.tagId(),
    name: Arb<String> = Arb.string(5..30, Codepoint.az()),
    priority: Arb<Int> = Arb.int(min = 1, max = 1_000),
    predefined: Arb<Boolean> = Arb.boolean(),
    color: Arb<Color> = Arb.color()
) = arbitrary {
  TagEntity(id = id.bind(), name = name.bind(), priority = priority.bind(), color = color.bind(), predefined = predefined.bind())
}