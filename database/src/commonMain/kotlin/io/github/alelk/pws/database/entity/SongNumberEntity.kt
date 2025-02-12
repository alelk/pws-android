package io.github.alelk.pws.database.entity

import androidx.room.*
import io.github.alelk.pws.domain.model.BookId
import io.github.alelk.pws.domain.model.SongId
import io.github.alelk.pws.domain.model.SongNumberId

/** Number of song in book.
 *
 * Unique constraints:
 * - Pair (songId, bookId) - book cannot contain the same song twice.
 * - Pair (bookId, number) - book contain numbered list of songs and song number is unique in book
 */
@Entity(
  tableName = "song_numbers",
  primaryKeys = ["book_id", "song_id"],
  foreignKeys = [
    ForeignKey(entity = SongEntity::class, parentColumns = ["id"], childColumns = ["song_id"], onDelete = ForeignKey.CASCADE),
    ForeignKey(entity = BookEntity::class, parentColumns = ["id"], childColumns = ["book_id"], onDelete = ForeignKey.CASCADE)
  ],
  indices = [
    Index(name = "idx_songs_numbers_song_id", value = ["song_id"]),
    Index(name = "idx_song_numbers_book_id", value = ["book_id"]),
    Index(name = "idx_song_numbers_song_id_book_id", value = ["song_id", "book_id"], unique = true),
    Index(name = "idx_song_numbers_book_id_number", value = ["book_id", "number"], unique = true),
  ]
)
data class SongNumberEntity(
  @ColumnInfo(name = "book_id") val bookId: BookId,
  @ColumnInfo(name = "song_id") val songId: SongId,
  @ColumnInfo(name = "number") val number: Int,
  @ColumnInfo(name = "priority") val priority: Int,
) {
  val id: SongNumberId get() = SongNumberId(bookId, songId)

  constructor(songNumberId: SongNumberId, number: Int, priority: Int) : this(songNumberId.bookId, songNumberId.songId, number, priority)
}