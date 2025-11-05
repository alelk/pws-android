package io.github.alelk.pws.database.favorite

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import io.github.alelk.pws.database.song_number.SongNumberEntity
import io.github.alelk.pws.domain.core.ids.BookId
import io.github.alelk.pws.domain.core.ids.SongId
import io.github.alelk.pws.domain.core.ids.SongNumberId

@Entity(
  tableName = "favorites",
  primaryKeys = ["song_id", "book_id"],
  foreignKeys = [
      ForeignKey(
          entity = SongNumberEntity::class,
          parentColumns = ["song_id", "book_id"], childColumns = ["song_id", "book_id"],
          onDelete = ForeignKey.Companion.CASCADE, onUpdate = ForeignKey.Companion.NO_ACTION
      )
  ],
  indices = [
      Index(name = "idx_favorites_song_id", value = ["song_id"]),
      Index(name = "idx_favorites_book_id", value = ["book_id"]),
      Index(name = "idx_favorites_position", value = ["position"])
  ],
)
data class FavoriteEntity(
    @ColumnInfo(name = "song_id") val songId: SongId,
    @ColumnInfo(name = "book_id") val bookId: BookId,
    @ColumnInfo(name = "position") val position: Int
) {
  val songNumberId: SongNumberId get() = SongNumberId(bookId, songId)

  constructor(songNumberId: SongNumberId, position: Int) : this(songNumberId.songId, songNumberId.bookId, position)
}