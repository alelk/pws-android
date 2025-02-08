package io.github.alelk.pws.database.entity

import androidx.room.Embedded
import androidx.room.Relation

data class SongNumberWithSongWithBookWithFavorite(
  @Embedded
  val songNumber: SongNumberEntity,
  @Relation(parentColumn = "psalmid", entityColumn = "_id")
  val song: SongEntity,
  @Relation(parentColumn = "bookid", entityColumn = "_id")
  val book: BookEntity,
  @Relation(parentColumn = "_id", entityColumn = "psalmnumberid")
  val favorite: FavoriteEntity?
) {
  val bookId: Long get() = checkNotNull(book.id) { "book id cannot be null" }
  val songNumberId: Long get() = checkNotNull(songNumber.id) { "song number id cannot be null" }
}