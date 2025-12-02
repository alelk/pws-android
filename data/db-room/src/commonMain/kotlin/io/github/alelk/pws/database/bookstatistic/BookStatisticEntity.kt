package io.github.alelk.pws.database.bookstatistic

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import io.github.alelk.pws.database.book.BookEntity
import io.github.alelk.pws.domain.core.ids.BookId

@Entity(
  tableName = "book_statistic",
  foreignKeys = [
      ForeignKey(
          entity = BookEntity::class,
          parentColumns = ["id"],
          childColumns = ["id"],
          onDelete = ForeignKey.CASCADE,
          onUpdate = ForeignKey.NO_ACTION
      )
  ]
)
data class BookStatisticEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: BookId,
    @ColumnInfo(name = "priority") val priority: Int?,
    @ColumnInfo(name = "readings") val readings: Int? = null,
    @ColumnInfo(name = "rating") val rating: Int? = null
)