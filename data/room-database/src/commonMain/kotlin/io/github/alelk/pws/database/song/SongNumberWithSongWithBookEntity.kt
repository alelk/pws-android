package io.github.alelk.pws.database.song

import androidx.room.Embedded
import androidx.room.Relation
import io.github.alelk.pws.database.book.BookEntity
import io.github.alelk.pws.database.song_number.SongNumberEntity

data class SongNumberWithSongWithBookEntity(
    @Embedded
  val songNumber: SongNumberEntity,
    @Relation(parentColumn = "song_id", entityColumn = "id")
  val song: SongEntity,
    @Relation(parentColumn = "book_id", entityColumn = "id")
  val book: BookEntity
)