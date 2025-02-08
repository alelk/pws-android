package io.github.alelk.pws.database.entity

import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.constant
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.long

fun Arb.Companion.songNumberEntity(
  id: Arb<Long?> = Arb.constant(null),
  number: Arb<Int> = Arb.int(min = 1, max = 10_000),
  songId: Arb<Long> = Arb.long(min = 1, max = 1_000_000),
  bookId: Arb<Long> = Arb.long(min = 1, max = 1_000_000),
  priority: Arb<Int> = Arb.int(min = 1, max = 1_000),
): Arb<SongNumberEntity> = arbitrary {
  SongNumberEntity(
    id = id.bind(),
    number = number.bind(),
    songId = songId.bind(),
    bookId = bookId.bind(),
    priority = priority.bind()
  )
}