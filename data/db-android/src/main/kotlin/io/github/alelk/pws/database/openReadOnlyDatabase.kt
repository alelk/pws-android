package io.github.alelk.pws.database

import android.database.sqlite.SQLiteException
import timber.log.Timber
import java.io.File
import android.database.sqlite.SQLiteDatabase as AndroidSQLiteDatabase
import net.zetetic.database.sqlcipher.SQLiteDatabase as SQLCipherDatabase

/**
 * Opens a previous on-disk database read-only as a [MigrationDbSource].
 *
 * Legacy databases (≤ 3.0.0) are plain SQLite files; databases starting from 3.2.3 are
 * SQLCipher-encrypted with the device passphrase from
 * [io.github.alelk.pws.database.security.KeyManager.getOrCreatePassphrase].
 *
 * We try the plain driver first and, if the file turns out to be encrypted (the plain driver
 * reports "file is not a database" on first access), fall back to SQLCipher using [passphrase].
 * The same passphrase is persisted in the Keystore-backed prefs and survives app updates, so it
 * decrypts a file written by a previous encrypted version.
 *
 * Requires the SQLCipher native library to be loaded (done in [PwsDatabaseProvider.getDatabase]).
 */
internal fun openReadOnlyDatabase(dbFile: File, passphrase: ByteArray): MigrationDbSource? {
  if (!dbFile.exists() || !dbFile.isFile) return null

  // 1. Try the plain SQLite driver (unencrypted legacy databases).
  val plain = runCatching {
    val db = AndroidSQLiteDatabase.openDatabase(dbFile.toString(), null, AndroidSQLiteDatabase.OPEN_READONLY)
    try {
      // Probe the database — encrypted files open fine but throw on first access.
      db.rawQuery("SELECT count(*) FROM sqlite_master", null).use { it.moveToFirst() }
      db
    } catch (e: Throwable) {
      db.close()
      throw e
    }
  }
  plain.onSuccess { return AndroidSQLiteDbSource(it) }
  plain.onFailure { e ->
    val msg = e.message.orEmpty()
    val looksEncrypted = e is SQLiteException &&
      (msg.contains("not a database", ignoreCase = true) || msg.contains("SQLITE_NOTADB", ignoreCase = true))
    if (!looksEncrypted) {
      Timber.e(e, "openReadOnlyDatabase: error opening $dbFile as plain SQLite: ${e.message}")
      return null
    }
    Timber.i("openReadOnlyDatabase: $dbFile is SQLCipher-encrypted — opening with device passphrase")
  }

  // 2. Fall back to SQLCipher (encrypted databases, 3.2.3+).
  return runCatching {
    val db = SQLCipherDatabase.openDatabase(dbFile.absolutePath, passphrase, null, SQLCipherDatabase.OPEN_READONLY, null)
    db.rawQuery("SELECT count(*) FROM sqlite_master", null).use { it.moveToFirst() }
    SqlCipherDbSource(db)
  }.onFailure { e ->
    Timber.e(e, "openReadOnlyDatabase: failed to open $dbFile with SQLCipher: ${e.message}")
  }.getOrNull()
}
