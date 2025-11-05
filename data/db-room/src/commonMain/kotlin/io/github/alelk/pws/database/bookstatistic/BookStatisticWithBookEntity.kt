package io.github.alelk.pws.database.bookstatistic

import androidx.room.Embedded
import androidx.room.Relation
import io.github.alelk.pws.database.book.BookEntity

@Deprecated("")
data class BookStatisticWithBookEntity(
  @Embedded
  val bookStatistic: BookStatisticEntity,
  @Relation(parentColumn = "id", entityColumn = "id")
  val book: BookEntity,
)