package io.github.alelk.pws.database.common.entity

import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.long

fun Arb.Companion.favoriteEntity(
  id: Arb<Long> = Arb.long(min = 1, max = 1_000_000),
  position: Arb<Int> = Arb.int(min = 0, max = 1000),
  psalmNumberId: Arb<Long> = Arb.long(min = 1, max = 1_000_000)
): Arb<FavoriteEntity> = arbitrary {
  FavoriteEntity(
    id = id.bind(),
    position = position.bind(),
    songNumberId = psalmNumberId.bind()
  )
}