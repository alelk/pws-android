package io.github.alelk.pws.database.entity

import androidx.room.Embedded
import androidx.room.Relation

data class BookStatisticWithBookEntity(
  @Embedded
  val bookStatistic: BookStatisticEntity,
  @Relation(parentColumn = "id", entityColumn = "id")
  val book: BookEntity,
)