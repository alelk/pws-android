package io.github.alelk.pws.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.execSQL
import androidx.room.useWriterConnection
import io.github.alelk.pws.database.book.BookDao
import io.github.alelk.pws.database.bookstatistic.BookStatisticDao
import io.github.alelk.pws.database.favorite.FavoriteDao
import io.github.alelk.pws.database.history.HistoryDao
import io.github.alelk.pws.database.song.SongDao
import io.github.alelk.pws.database.song_number.SongNumberDao
import io.github.alelk.pws.database.song_tag.SongTagDao
import io.github.alelk.pws.database.song_reference.SongReferenceDao
import io.github.alelk.pws.database.tag.TagDao
import io.github.alelk.pws.database.book.BookEntity
import io.github.alelk.pws.database.bookstatistic.BookStatisticEntity
import io.github.alelk.pws.database.favorite.FavoriteEntity
import io.github.alelk.pws.database.history.HistoryEntity
import io.github.alelk.pws.database.song.SongEntity
import io.github.alelk.pws.database.song.SongFtsEntity
import io.github.alelk.pws.database.song_number.SongNumberEntity
import io.github.alelk.pws.database.song_tag.SongTagEntity
import io.github.alelk.pws.database.song_reference.SongReferenceEntity
import io.github.alelk.pws.database.tag.TagEntity

@Database(
  version = 11,
  entities = [
    BookEntity::class,
    BookStatisticEntity::class,
    FavoriteEntity::class,
    HistoryEntity::class,
    SongEntity::class,
    SongFtsEntity::class,
    SongNumberEntity::class,
    SongReferenceEntity::class,
    TagEntity::class,
    SongTagEntity::class
  ],
  exportSchema = false
)
@TypeConverters(DbTypeConverters::class)
@ConstructedBy(PwsDatabaseConstructor::class)
abstract class PwsDatabase : RoomDatabase() {
  abstract fun bookDao(): BookDao
  abstract fun bookStatisticDao(): BookStatisticDao
  abstract fun favoriteDao(): FavoriteDao
  abstract fun historyDao(): HistoryDao
  abstract fun songDao(): SongDao
  abstract fun songNumberDao(): SongNumberDao
  abstract fun songReferenceDao(): SongReferenceDao
  abstract fun tagDao(): TagDao
  abstract fun songTagDao(): SongTagDao

  suspend fun vacuumAndCheckpoint() = this.useWriterConnection {
    it.execSQL("VACUUM")
    it.execSQL("PRAGMA wal_checkpoint(TRUNCATE)")
  }
}