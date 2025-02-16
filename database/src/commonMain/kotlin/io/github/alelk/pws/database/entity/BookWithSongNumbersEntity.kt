package io.github.alelk.pws.database.entity

import androidx.room.Embedded
import androidx.room.Relation

data class BookWithSongNumbersEntity(
  @Embedded
  val book: BookEntity,
  @Relation(parentColumn = "id", entityColumn = "book_id")
  val songNumbers: List<SongNumberEntity>
)