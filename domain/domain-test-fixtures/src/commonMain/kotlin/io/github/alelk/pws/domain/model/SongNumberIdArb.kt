package io.github.alelk.pws.domain.model

import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary

fun Arb.Companion.songNumberId(
  bookId: Arb<BookId> = Arb.bookId(),
  songId: Arb<SongId> = Arb.songId()
): Arb<SongNumberId> =
  arbitrary {
    SongNumberId(bookId.bind(), songId.bind())
  }