package com.alelk.pws.database.support.entity

import androidx.room.*

@Entity(tableName = "psalmnumbers")
data class SongNumberEntityV120(
  @PrimaryKey @ColumnInfo(name = "_id") val id: Long? = null,
  @ColumnInfo(name = "number") val number: Int,
  @ColumnInfo(name = "psalmid") val songId: Long,
  @ColumnInfo(name = "bookid") val bookId: Long,
  @ColumnInfo(name = "priority") val priority: Int,
)