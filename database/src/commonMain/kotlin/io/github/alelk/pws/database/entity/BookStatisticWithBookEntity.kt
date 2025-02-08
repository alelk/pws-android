package io.github.alelk.pws.database.entity

import androidx.room.Embedded
import androidx.room.Relation

data class BookStatisticWithBookEntity(
  @Embedded
  val bookStatistic: BookStatisticEntity,
  @Relation(parentColumn = "bookid", entityColumn = "_id")
  val book: BookEntity,
)