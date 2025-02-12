package io.github.alelk.pws.database.entity

import io.github.alelk.pws.domain.model.BookId
import io.github.alelk.pws.domain.model.SongId
import io.github.alelk.pws.domain.model.SongNumberId
import io.github.alelk.pws.domain.model.bookId
import io.github.alelk.pws.domain.model.songId
import io.github.alelk.pws.domain.model.songNumberId
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.removeEdgecases
import io.kotest.property.kotlinx.datetime.datetime
import kotlinx.datetime.LocalDateTime

fun Arb.Companion.historyEntity(
  id: Arb<Long?>? = Arb.long(min = 0, max = 1_000_000_000).removeEdgecases(),
  bookId: Arb<BookId> = Arb.bookId(),
  songId: Arb<SongId> = Arb.songId(),
  songNumberId: Arb<SongNumberId> = Arb.songNumberId(bookId, songId),
  accessTimestamp: Arb<LocalDateTime> = Arb.datetime(yearRange = 2025..2050)
): Arb<HistoryEntity> = arbitrary {
  HistoryEntity(
    id = id?.bind() ?: 0,
    songNumberId = songNumberId.bind(),
    accessTimestamp = accessTimestamp.bind()
  )
}