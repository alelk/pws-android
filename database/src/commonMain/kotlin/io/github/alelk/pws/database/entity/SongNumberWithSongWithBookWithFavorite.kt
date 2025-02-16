package io.github.alelk.pws.database.entity

import androidx.room.Embedded
import androidx.room.Relation

data class SongNumberWithSongWithBookWithFavorite(
  @Embedded
  val songNumber: SongNumberEntity,
  @Relation(parentColumn = "song_id", entityColumn = "id")
  val song: SongEntity,
  @Relation(parentColumn = "book_id", entityColumn = "id")
  val book: BookEntity,
  @Relation(parentColumn = "(song_id, book_id)", entityColumn = "(song_id, book_id)")
  val favorite: FavoriteEntity?
)