package io.github.alelk.pws.database.book

import androidx.room.ColumnInfo
import io.github.alelk.pws.domain.core.Locale
import io.github.alelk.pws.domain.core.Version
import io.github.alelk.pws.domain.core.ids.BookId
import io.github.alelk.pws.domain.core.ids.SongNumberId

data class BookSummaryProjection(
  @ColumnInfo(name = "id") val id: BookId,
  @ColumnInfo(name = "version") val version: Version,
  @ColumnInfo(name = "locale") val locale: Locale,
  @ColumnInfo(name = "name") val name: String,
  @ColumnInfo(name = "display_short_name") val displayShortName: String,
  @ColumnInfo(name = "display_name") val displayName: String,
  @ColumnInfo(name = "count_songs") val countSongs: Int,
  @ColumnInfo(name = "first_song_number_id") val firstSongNumberId: SongNumberId?,
  @ColumnInfo(name = "priority") val priority: Int,
)