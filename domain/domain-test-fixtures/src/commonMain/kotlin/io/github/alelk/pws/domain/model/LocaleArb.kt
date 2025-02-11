package io.github.alelk.pws.domain.model

import io.kotest.property.Arb
import io.kotest.property.arbitrary.of

fun Arb.Companion.locale(): Arb<Locale> = Arb.of(setOf("ru", "en", "uk").map { Locale.of(it) })