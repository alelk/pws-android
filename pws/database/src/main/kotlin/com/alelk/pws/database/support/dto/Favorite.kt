package com.alelk.pws.database.support.dto

import androidx.room.ColumnInfo

data class Favorite(
  @ColumnInfo("edition") val bookExternalId: String,
  @ColumnInfo("number") val songNumber: Int,
  @ColumnInfo("position") val position: Int
)
