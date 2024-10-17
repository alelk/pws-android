package io.github.alelk.pws.database.common.entity

import androidx.room.*

@Entity(
  tableName = "bookstatistic",
  foreignKeys = [
    ForeignKey(entity = BookEntity::class, parentColumns = ["_id"], childColumns = ["bookid"], onDelete = ForeignKey.CASCADE)
  ],
  indices = [Index(name = "idx_bookstatistic_bookid", value = ["bookid"], unique = true)]
)
data class BookStatisticEntity(
  @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "_id") val id: Long? = null,
  @ColumnInfo(name = "bookid") val bookId: Long,
  @ColumnInfo(name = "userpref") val userPreference: Int?,
  @ColumnInfo(name = "readings") val readings: Int? = null,
  @ColumnInfo(name = "rating") val rating: Int? = null
)
