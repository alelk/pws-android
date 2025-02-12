package io.github.alelk.pws.database.entity

import androidx.room.Embedded
import androidx.room.Relation

data class SongNumberWithBookEntity(
  @Embedded
  val songNumber: SongNumberEntity,
  @Relation(parentColumn = "book_id", entityColumn = "id")
  val book: BookEntity
)