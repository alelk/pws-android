package io.github.alelk.pws.database.common.model

import io.kotest.property.Arb
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.az
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.string

fun Arb.Companion.person(name: Arb<String> = Arb.string(5..40, Codepoint.az())): Arb<Person> = name.map { Person(it) }