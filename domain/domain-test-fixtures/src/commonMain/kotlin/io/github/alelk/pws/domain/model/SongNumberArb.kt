package io.github.alelk.pws.domain.model

import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.int

fun Arb.Companion.songNumber(
  bookId: Arb<BookId> = Arb.bookId(),
  number: Arb<Int> = Arb.int(1..999_999)
): Arb<SongNumber> =
  arbitrary {
    SongNumber(bookId.bind(), number.bind())
  }