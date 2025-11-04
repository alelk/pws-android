package io.github.alelk.pws.database.book_statistic

import androidx.room.Embedded
import androidx.room.Relation
import io.github.alelk.pws.database.book.BookEntity

data class BookStatisticWithBookEntity(
    @Embedded
  val bookStatistic: BookStatisticEntity,
    @Relation(parentColumn = "id", entityColumn = "id")
  val book: BookEntity,
)