package com.alelk.pws.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.alelk.pws.database.entity.BookEntity
import com.alelk.pws.database.entity.BookStatisticEntity
import com.alelk.pws.database.entity.FavoriteEntity
import com.alelk.pws.database.entity.HistoryEntity
import com.alelk.pws.database.entity.SongEntity
import com.alelk.pws.database.entity.SongNumberEntity
import com.alelk.pws.database.entity.SongNumberTagEntity
import com.alelk.pws.database.entity.SongSongReferenceEntity
import com.alelk.pws.database.entity.TagEntity
import com.alelk.pws.database.entity.converter.DbTypeConverters
import com.alelk.pws.database.dao.BookDao
import com.alelk.pws.database.dao.BookStatisticDao
import com.alelk.pws.database.dao.FavoriteDao
import com.alelk.pws.database.dao.HistoryDao
import com.alelk.pws.database.dao.SongDao
import com.alelk.pws.database.dao.SongNumberDao
import com.alelk.pws.database.dao.SongSongReferenceDao
import com.alelk.pws.database.dao.TagDao
import com.alelk.pws.database.dao.SongNumberTagDao
import com.alelk.pws.database.helper.PwsDatabaseHelper

@Database(
  version = PwsDatabaseHelper.DATABASE_VERSION,
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

  //  abstract fun bookStatisticDao(): BookStatisticDao
  abstract fun favoriteDao(): FavoriteDao
  abstract fun historyDao(): HistoryDao
  abstract fun songDao(): SongDao
  abstract fun songNumberDao(): SongNumberDao
//  abstract fun songSongReferenceDao(): SongSongReferenceDao
//  abstract fun tagDao(): TagDao
//  abstract fun songNumberTagDao(): SongNumberTagDao
}

object DatabaseProvider {
  @Volatile
  private var INSTANCE: PwsDatabase? = null

  fun getDatabase(context: Context): PwsDatabase {
    return INSTANCE ?: synchronized(this) {
      val instance = Room.databaseBuilder(
        context.applicationContext,
        PwsDatabase::class.java,
        PwsDatabaseHelper.DATABASE_NAME
      ).build()
      INSTANCE = instance
      instance
    }
  }
}