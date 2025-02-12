package io.github.alelk.pws.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.execSQL
import androidx.room.useWriterConnection
import io.github.alelk.pws.database.dao.BookDao
import io.github.alelk.pws.database.dao.BookStatisticDao
import io.github.alelk.pws.database.dao.FavoriteDao
import io.github.alelk.pws.database.dao.HistoryDao
import io.github.alelk.pws.database.dao.SongDao
import io.github.alelk.pws.database.dao.SongNumberDao
import io.github.alelk.pws.database.dao.SongTagDao
import io.github.alelk.pws.database.dao.SongReferenceDao
import io.github.alelk.pws.database.dao.TagDao
import io.github.alelk.pws.database.entity.BookEntity
import io.github.alelk.pws.database.entity.BookStatisticEntity
import io.github.alelk.pws.database.entity.FavoriteEntity
import io.github.alelk.pws.database.entity.HistoryEntity
import io.github.alelk.pws.database.entity.SongEntity
import io.github.alelk.pws.database.entity.SongFtsEntity
import io.github.alelk.pws.database.entity.SongNumberEntity
import io.github.alelk.pws.database.entity.SongTagEntity
import io.github.alelk.pws.database.entity.SongReferenceEntity
import io.github.alelk.pws.database.entity.TagEntity
import io.github.alelk.pws.database.entity.converter.DbTypeConverters

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
abstract class PwsDatabase : RoomDatabase() {
  abstract fun bookDao(): BookDao
  abstract fun bookStatisticDao(): BookStatisticDao
  abstract fun favoriteDao(): FavoriteDao
  abstract fun historyDao(): HistoryDao
  abstract fun songDao(): SongDao
  abstract fun songNumberDao(): SongNumberDao
  abstract fun songReferenceDao(): SongReferenceDao
  abstract fun tagDao(): TagDao
  abstract fun songNumberTagDao(): SongTagDao

  suspend fun vacuumAndCheckpoint() = this.useWriterConnection {
    it.execSQL("VACUUM")
    it.execSQL("PRAGMA wal_checkpoint(TRUNCATE)")
  }
}