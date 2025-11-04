package io.github.alelk.pws.database.book_statistic

import io.github.alelk.pws.domain.core.ids.BookId
import io.github.alelk.pws.domain.core.ids.bookId
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.orNull

fun Arb.Companion.bookStatisticEntity(
  id: Arb<BookId> = Arb.bookId(),
  userPreference: Arb<Int?> = Arb.int(min = 0, max = 100).orNull(),
  readings: Arb<Int?> = Arb.int(min = 0, max = 1000).orNull(),
  rating: Arb<Int?> = Arb.int(min = 1, max = 5).orNull()
): Arb<BookStatisticEntity> = arbitrary {
  BookStatisticEntity(
    id = id.bind(),
    priority = userPreference.bind(),
    readings = readings.bind(),
    rating = rating.bind()
  )
}