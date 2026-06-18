package io.github.alelk.pws.database

import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import timber.log.Timber
import java.io.File

internal fun openReadOnlyDatabase(dbFile: File): SQLiteDatabase? {
  if (!dbFile.exists() || !dbFile.isFile) return null
  return runCatching {
    val db = SQLiteDatabase.openDatabase(dbFile.toString(), null, SQLiteDatabase.OPEN_READONLY)
    // Probe the database to detect encrypted files early (they throw on first access)
    db.rawQuery("SELECT count(*) FROM sqlite_master", null).use { it.moveToFirst() }
    db
  }.onFailure { e ->
    val msg = e.message.orEmpty()
    if (e is SQLiteException && (msg.contains("not a database", ignoreCase = true) ||
        msg.contains("SQLITE_NOTADB", ignoreCase = true) ||
        msg.contains("file is not a database", ignoreCase = true))
    ) {
      // Database was created by a newer version with SQLCipher encryption.
      // TODO: open with SQLCipher + KeyManager.getOrCreatePassphrase() when migrating 3.3.0 → next version.
      Timber.w("openReadOnlyDatabase: $dbFile appears to be SQLCipher-encrypted — skipping migration from this version")
    } else {
      Timber.e(e, "openReadOnlyDatabase: error opening $dbFile: ${e.message}")
    }
  }.getOrNull()
}
