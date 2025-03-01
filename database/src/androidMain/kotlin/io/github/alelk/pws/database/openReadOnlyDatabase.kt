package io.github.alelk.pws.database

import android.database.sqlite.SQLiteDatabase
import timber.log.Timber
import java.io.File

internal fun openReadOnlyDatabase(dbFile: File): SQLiteDatabase? =
  runCatching {
    if (dbFile.exists() && dbFile.isFile) SQLiteDatabase.openDatabase(dbFile.toString(), null, SQLiteDatabase.OPEN_READONLY)
    else null
  }.onFailure { e ->
    Timber.e(e, "error opening read-only database $dbFile: ${e.message}")
  }.getOrNull()