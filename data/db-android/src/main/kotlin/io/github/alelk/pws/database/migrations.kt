package io.github.alelk.pws.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// History:
//   v14 — initial schema. Upgrades from older releases (pws.1.8.0.db etc.) get
//     a fresh v14 DB; books and user data are replayed by `migrateDataFromPrevDatabase`.
//   v15 — fix FTS4 songs_fts: drop backtick-quoted tokenizer arg that caused SQLiteException
//     on createAllTables for fresh installs (unicode61 tokenizer without extra args).

val MIGRATION_14_15 = object : Migration(14, 15) {
  override fun migrate(db: SupportSQLiteDatabase) {
    // Triggers (ON `songs`) survive DROP TABLE `songs_fts` — SQLite only drops triggers that
    // are defined ON the dropped table itself. The sync triggers are unchanged between v14 and
    // v15, so we only need to drop and recreate the FTS virtual table.
    db.execSQL("DROP TABLE IF EXISTS `songs_fts`")
    db.execSQL(
      "CREATE VIRTUAL TABLE IF NOT EXISTS `songs_fts` USING FTS4(" +
        "`name` TEXT, `author` TEXT, `translator` TEXT, `composer` TEXT, " +
        "`bibleref` TEXT, `lyric` TEXT NOT NULL, tokenize=unicode61, content=`songs`)"
    )
    db.execSQL("INSERT INTO `songs_fts`(`songs_fts`) VALUES('rebuild')")
  }
}
