package io.github.alelk.pws.database.entity

import androidx.room.Embedded
import androidx.room.Relation

data class SongNumberWithSongWithBook(
  @Embedded
  val songNumber: SongNumberEntity,
  @Relation(parentColumn = "psalmid", entityColumn = "_id")
  val song: SongEntity,
  @Relation(parentColumn = "bookid", entityColumn = "_id")
  val book: BookEntity
) {
  val bookId: Long get() = checkNotNull(book.id) { "book id cannot be null" }
  val songNumberId: Long get() = checkNotNull(songNumber.id) { "song number id cannot be null" }
}