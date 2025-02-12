package io.github.alelk.pws.database.entity

import androidx.room.*
import io.github.alelk.pws.domain.model.BookId

@Entity(
  tableName = "bookstatistic",
  foreignKeys = [
    ForeignKey(entity = BookEntity::class, parentColumns = ["id"], childColumns = ["id"], onDelete = ForeignKey.CASCADE, onUpdate = ForeignKey.NO_ACTION)
  ]
)
data class BookStatisticEntity(
  @PrimaryKey @ColumnInfo(name = "id") val id: BookId,
  @ColumnInfo(name = "priority") val priority: Int?,
  @ColumnInfo(name = "readings") val readings: Int? = null,
  @ColumnInfo(name = "rating") val rating: Int? = null
)
