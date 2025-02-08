package io.github.alelk.pws.database.entity

import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.orNull

fun Arb.Companion.bookStatisticEntity(
  id: Arb<Long?> = Arb.long(min = 1, max = 1_000_000),
  bookId: Arb<Long> = Arb.long(min = 1, max = 1_000_000),
  userPreference: Arb<Int?> = Arb.int(min = 0, max = 100).orNull(),
  readings: Arb<Int?> = Arb.int(min = 0, max = 1000).orNull(),
  rating: Arb<Int?> = Arb.int(min = 1, max = 5).orNull()
): Arb<BookStatisticEntity> = arbitrary {
  BookStatisticEntity(
    id = id.bind(),
    bookId = bookId.bind(),
    userPreference = userPreference.bind(),
    readings = readings.bind(),
    rating = rating.bind()
  )
}