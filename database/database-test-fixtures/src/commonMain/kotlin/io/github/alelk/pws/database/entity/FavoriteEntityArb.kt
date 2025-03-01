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

fun Arb.Companion.favoriteEntity(
  bookId: Arb<BookId> = Arb.bookId(),
  songId: Arb<SongId> = Arb.songId(),
  songNumberId: Arb<SongNumberId> = Arb.songNumberId(bookId = bookId, songId = songId),
  position: Arb<Int> = Arb.int(min = 0, max = 1000)
): Arb<FavoriteEntity> =
  arbitrary {
    FavoriteEntity(songNumberId = songNumberId.bind(), position = position.bind())
  }