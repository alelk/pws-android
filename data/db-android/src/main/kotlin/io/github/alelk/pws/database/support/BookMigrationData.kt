package io.github.alelk.pws.database.support

import io.github.alelk.pws.database.book.BookEntity
import io.github.alelk.pws.database.bookstatistic.BookStatisticEntity
import io.github.alelk.pws.database.installed_book.InstalledBookEntity
import io.github.alelk.pws.database.song.SongEntity
import io.github.alelk.pws.database.song_number.SongNumberEntity

internal data class BookMigrationData(
  val book: BookEntity,
  val bookStatistic: BookStatisticEntity,
  val installedBook: InstalledBookEntity,
  val songs: List<SongEntity>,
  val songNumbers: List<SongNumberEntity>,
)
