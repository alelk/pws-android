package io.github.alelk.pws.domain.core

import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.double

/** Generates OptionalField values: Unchanged / Clear / Set(base).
 * [pUnchanged] and [pClear] are the probabilities for allowing custom probabilities for Unchanged and Clear; Set gets the remaining mass.
 **/
fun <T> Arb.Companion.optionalField(
  base: Arb<T>,
  pUnchanged: Double = 0.4,
  pClear: Double = 0.1
): Arb<OptionalField<T>> = arbitrary {
  require(pUnchanged >= 0 && pClear >= 0 && pUnchanged + pClear < 1.0) { "Invalid probabilities" }
  val r = Arb.double(0.0..1.0).bind()
  when {
    r < pUnchanged -> OptionalField.Unchanged
    r < pUnchanged + pClear -> OptionalField.Clear
    else -> OptionalField.Set(base.bind())
  }
}
