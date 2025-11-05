package io.github.alelk.pws.database.history

import androidx.room.*
import io.github.alelk.pws.database.song_number.SongNumberEntity
import io.github.alelk.pws.domain.core.ids.BookId
import io.github.alelk.pws.domain.core.ids.SongId
import io.github.alelk.pws.domain.core.ids.SongNumberId
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
private fun currentDateTime() = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

@Entity(
  tableName = "history",
  foreignKeys = [
    ForeignKey(
      entity = SongNumberEntity::class,
      parentColumns = ["book_id", "song_id"], childColumns = ["book_id", "song_id"],
      onDelete = ForeignKey.CASCADE, onUpdate = ForeignKey.NO_ACTION
    )
  ],
  indices = [
    Index(name = "idx_history_song_id", value = ["song_id"]),
    Index(name = "idx_history_book_id", value = ["book_id"]),
    Index(name = "ids_history_book_id_song_id", value = ["book_id", "song_id"]),
    Index(name = "ids_history_book_id_song_id_access_timestamp", value = ["book_id", "song_id", "access_timestamp"], unique = true),
  ]
)
data class HistoryEntity(
  @ColumnInfo(name = "song_id") val songId: SongId,
  @ColumnInfo(name = "book_id") val bookId: BookId,
  @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0,
  @ColumnInfo(name = "access_timestamp") val accessTimestamp: LocalDateTime = currentDateTime()
) {

  constructor(songNumberId: SongNumberId, id: Long = 0, accessTimestamp: LocalDateTime = currentDateTime()) :
    this(songNumberId.songId, songNumberId.bookId, id, accessTimestamp)

  val songNumberId: SongNumberId get() = SongNumberId(bookId, songId)
}