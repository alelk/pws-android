package io.github.alelk.pws.database.book

import androidx.room.Embedded
import androidx.room.Relation
import io.github.alelk.pws.database.song_number.SongNumberEntity

data class BookWithSongNumbersProjection(
    @Embedded
  val book: BookEntity,
    @Relation(parentColumn = "id", entityColumn = "book_id")
  val songNumbers: List<SongNumberEntity>
)