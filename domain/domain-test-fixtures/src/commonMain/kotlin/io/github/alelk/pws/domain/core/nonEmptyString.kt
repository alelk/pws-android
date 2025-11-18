package io.github.alelk.pws.domain.core

import io.kotest.property.Arb
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.az
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.string

/**
 * Arbitrary generator for NonEmptyString.
 *
 * sizeRange must start at >= 1 to satisfy the NonEmptyString invariant.
 */
fun Arb.Companion.nonEmptyString(
  sizeRange: IntRange = 1..40,
  codepoints: Arb<Codepoint> = Codepoint.az()
): Arb<NonEmptyString> {
  require(sizeRange.first > 0) { "sizeRange must have min >= 1" }
  return Arb.string(sizeRange, codepoints).map { NonEmptyString(it) }
}
