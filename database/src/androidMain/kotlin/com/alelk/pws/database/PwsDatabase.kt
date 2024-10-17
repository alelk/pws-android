package com.alelk.pws.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.alelk.pws.database.common.entity.converter.DbTypeConverters
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
import com.alelk.pws.database.helper.PwsDatabaseHelper.Companion.DATABASE_VERSION
import io.github.alelk.pws.database.common.entity.BookEntity
import io.github.alelk.pws.database.common.entity.BookStatisticEntity
import io.github.alelk.pws.database.common.entity.FavoriteEntity
import io.github.alelk.pws.database.common.entity.HistoryEntity
import io.github.alelk.pws.database.common.entity.SongEntity
import io.github.alelk.pws.database.common.entity.SongFtsEntity
import io.github.alelk.pws.database.common.entity.SongNumberEntity
import io.github.alelk.pws.database.common.entity.SongNumberTagEntity
import io.github.alelk.pws.database.common.entity.SongSongReferenceEntity
import io.github.alelk.pws.database.common.entity.TagEntity
import timber.log.Timber
import kotlin.time.measureTime

@Database(
  version = DATABASE_VERSION,
  entities = [
    BookEntity::class,
    BookStatisticEntity::class,
    FavoriteEntity::class,
    HistoryEntity::class,
    SongEntity::class,
    SongFtsEntity::class,
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
      )
        .addMigrations(MIGRATION_6_7)
        .addCallback(databaseCallbacks)
        .build()
      INSTANCE = instance
      instance
    }
  }
}

private val databaseCallbacks = object : RoomDatabase.Callback() {
  override fun onCreate(db: SupportSQLiteDatabase) {
    super.onCreate(db)
    runCatching {
      db.withTransaction {
        Timber.i("Database created, filling the FTS table...")
        execSQL(
          """
            INSERT OR REPLACE INTO songs_fts (rowid, name, author, translator, composer, bibleref, text)
            SELECT _id, name, author, translator, composer, bibleref, text FROM psalms
            """
        )
        Timber.i("FTS table filled with data.")
      }
    }.onFailure { e -> Timber.e(e, e.message) }
  }
}

private val MIGRATION_6_7 = object : Migration(6, 7) {
  override fun migrate(db: SupportSQLiteDatabase) {
    val migrationTime = measureTime {
      db.withTransaction {
        Timber.i("migrate pws database from version 6 to version 7")

        Timber.i("drop table psalms_fts")
        db.execSQL("DROP TABLE IF EXISTS psalms_fts")

        Timber.i("drop table songs_fts")
        db.execSQL("DROP TABLE IF EXISTS songs_fts")

        Timber.i("create new songs_fts table with porter tokenizer")
        db.execSQL(
          """
            CREATE VIRTUAL TABLE songs_fts 
            USING fts4(name, bibleref, text, author, composer, translator, tokenize=porter, content=`psalms`)
            """
        )

        Timber.i("fill songs_fts table with data...")
        db.execSQL(
          """
            INSERT OR REPLACE INTO songs_fts (rowid, name, author, translator, composer, bibleref, text)
            SELECT _id, name, author, translator, composer, bibleref, text FROM psalms
            """
        )
      }
    }
    Timber.i("migration 6 -> 7 completed, time: $migrationTime")
  }
}

private fun <T> SupportSQLiteDatabase.withTransaction(body: SupportSQLiteDatabase.() -> T): T =
  try {
    this.beginTransaction()
    body().also { this.setTransactionSuccessful() }
  } finally {
    this.endTransaction()
  }
