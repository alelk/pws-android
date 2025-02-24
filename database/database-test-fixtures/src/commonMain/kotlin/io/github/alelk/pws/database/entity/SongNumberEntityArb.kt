package io.github.alelk.pws.database.entity

import io.github.alelk.pws.domain.model.BookId
import io.github.alelk.pws.domain.model.SongId
import io.github.alelk.pws.domain.model.SongNumberId
import io.github.alelk.pws.domain.model.bookId
import io.github.alelk.pws.domain.model.songId
import io.github.alelk.pws.domain.model.songNumberId
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.int

fun Arb.Companion.songNumberEntity(
  number: Arb<Int> = Arb.int(min = 1, max = 10_000),
  bookId: Arb<BookId> = Arb.bookId(),
  songId: Arb<SongId> = Arb.songId(),
  songNumberId: Arb<SongNumberId> = Arb.songNumberId(bookId, songId),
  priority: Arb<Int> = Arb.int(min = 1, max = 1_000),
): Arb<SongNumberEntity> = arbitrary {
  SongNumberEntity(
    number = number.bind(),
    songNumberId = songNumberId.bind(),
    priority = priority.bind()
  )
}