package io.github.alelk.pws.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// History:
//   v14 — initial schema. Upgrades from older releases (pws.1.8.0.db etc.) get
//     a fresh v14 DB; books and user data are replayed by `migrateDataFromPrevDatabase`.
//   v15 — songs_fts declared without the default `remove_diacritics=1` tokenizer argument.
//     Shipped as a fix for a SQLiteException on createAllTables, but that crash comes from a
//     half-created database file, not from the FTS declaration — the v14 statement is accepted
//     by the bundled SQLCipher engine (see `discardHalfCreatedDatabase`). The version stays: the
//     schema is already at 15 on devices.

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
