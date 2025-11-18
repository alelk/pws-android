package io.github.alelk.pws.domain.core

import io.kotest.property.Arb
import io.kotest.property.arbitrary.choice
import io.kotest.property.arbitrary.constant
import io.kotest.property.arbitrary.map

/** Helper to generate OptionalField values: Unchanged / Clear / Set(base). */
fun <T> Arb.Companion.optionalField(base: Arb<T>): Arb<OptionalField<T>> =
  Arb.choice(
    Arb.constant(OptionalField.Unchanged),
    Arb.constant(OptionalField.Clear),
    base.map { OptionalField.Set(it) }
  )