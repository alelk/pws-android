package io.github.alelk.pws.database.favorite

import io.github.alelk.pws.domain.core.ids.BookId
import io.github.alelk.pws.domain.core.ids.SongId
import io.github.alelk.pws.domain.core.ids.SongNumberId
import io.github.alelk.pws.domain.core.ids.bookId
import io.github.alelk.pws.domain.core.ids.songId
import io.github.alelk.pws.domain.core.ids.songNumberId
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