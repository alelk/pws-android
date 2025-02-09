package io.github.alelk.pws.database.entity

import androidx.room.Embedded
import androidx.room.Relation

data class SongNumberWithSongEntity(
  @Embedded
  val songNumber: SongNumberEntity,
  @Relation(parentColumn = "psalmid", entityColumn = "_id")
  val song: SongEntity,
)