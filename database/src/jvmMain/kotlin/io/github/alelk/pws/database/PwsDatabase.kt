package io.github.alelk.pws.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.execSQL
import androidx.room.useWriterConnection
import io.github.alelk.pws.database.common.entity.BookEntity
import io.github.alelk.pws.database.common.entity.BookStatisticEntity
import io.github.alelk.pws.database.common.entity.FavoriteEntity
import io.github.alelk.pws.database.common.entity.HistoryEntity
import io.github.alelk.pws.database.common.entity.SongEntity
import io.github.alelk.pws.database.common.entity.SongNumberEntity
import io.github.alelk.pws.database.common.entity.SongNumberTagEntity
import io.github.alelk.pws.database.common.entity.SongSongReferenceEntity
import io.github.alelk.pws.database.common.entity.TagEntity
import io.github.alelk.pws.database.common.entity.converter.DbTypeConverters
import io.github.alelk.pws.database.dao.BookDao
import io.github.alelk.pws.database.dao.BookStatisticDao
import io.github.alelk.pws.database.dao.FavoriteDao
import io.github.alelk.pws.database.dao.HistoryDao
import io.github.alelk.pws.database.dao.SongDao
import io.github.alelk.pws.database.dao.SongNumberDao
import io.github.alelk.pws.database.dao.SongNumberTagDao
import io.github.alelk.pws.database.dao.SongSongReferenceDao
import io.github.alelk.pws.database.dao.TagDao

@Database(
  version = 8,
  entities = [
    BookEntity::class,
    BookStatisticEntity::class,
    FavoriteEntity::class,
    HistoryEntity::class,
    SongEntity::class,
    SongNumberEntity::class,
    SongSongReferenceEntity::class,
    TagEntity::class,
    SongNumberTagEntity::class
  ],
  exportSchema = false
)
@TypeConverters(DbTypeConverters::class)
abstract class PwsDatabase : RoomDatabase() {
  abstract fun bookDao(): BookDao
  abstract fun bookStatisticDao(): BookStatisticDao
  abstract fun favoriteDao(): FavoriteDao
  abstract fun historyDao(): HistoryDao
  abstract fun songDao(): SongDao
  abstract fun songNumberDao(): SongNumberDao
  abstract fun songSongReferenceDao(): SongSongReferenceDao
  abstract fun tagDao(): TagDao
  abstract fun songNumberTagDao(): SongNumberTagDao

  suspend fun vacuumAndCheckpoint() = this.useWriterConnection {
    it.execSQL("VACUUM")
    it.execSQL("PRAGMA wal_checkpoint(TRUNCATE)")
  }
}