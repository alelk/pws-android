package io.github.alelk.pws.database.common

import io.kotest.property.Arb
import io.kotest.property.arbitrary.of
import java.util.Locale

fun Arb.Companion.locale(): Arb<Locale> = Arb.of(setOf("ru", "en", "uk").map { Locale.forLanguageTag(it) })