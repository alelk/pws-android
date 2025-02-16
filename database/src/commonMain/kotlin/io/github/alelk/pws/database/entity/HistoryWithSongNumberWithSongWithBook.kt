package io.github.alelk.pws.database.entity

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class HistoryWithSongNumberWithSongWithBook(
  @Embedded
  val history: HistoryEntity,
  @Relation(parentColumn = "song_id", entityColumn = "id")
  val song: SongEntity,
  @Relation(parentColumn = "book_id", entityColumn = "id")
  val book: BookEntity,
  @Relation(
    parentColumn = "song_id",
    entityColumn = "song_id",
    associateBy = Junction(
      value = SongNumberEntity::class,
      parentColumn = "book_id",
      entityColumn = "book_id"
    )
  )
  val songNumber: SongNumberEntity,
)