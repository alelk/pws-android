package io.github.alelk.pws.database.entity

import androidx.room.Embedded
import androidx.room.Relation

data class BookWithSongNumbersEntity(
  @Embedded
  val book: BookEntity,
  @Relation(parentColumn = "_id", entityColumn = "bookid")
  val songNumbers: List<SongNumberEntity>
)