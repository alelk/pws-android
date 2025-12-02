package io.github.alelk.pws.domain

import io.kotest.property.Arb
import io.kotest.property.RandomSource
import io.kotest.property.Sample


fun <A, K> Arb<A>.distinctBy(attempts: Int = 100, selector: (A) -> K) = object : Arb<A>() {

  private val seen = mutableSetOf<K>()

  override fun edgecase(rs: RandomSource): A? = this@distinctBy.edgecase(rs)

  override fun sample(rs: RandomSource): Sample<A> {
    var iterations = 0
    return generateSequence {
      if ((iterations++) < attempts) this@distinctBy.sample(rs) else null
    }.filter { seen.add(selector(it.value)) }
      .first()
  }

}