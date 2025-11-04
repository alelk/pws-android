package io.github.alelk.pws.database.song_number

import io.github.alelk.pws.domain.core.ids.BookId
import io.github.alelk.pws.domain.core.ids.SongId
import io.github.alelk.pws.domain.core.ids.SongNumberId
import io.github.alelk.pws.domain.core.ids.bookId
import io.github.alelk.pws.domain.core.ids.songId
import io.github.alelk.pws.domain.core.ids.songNumberId
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