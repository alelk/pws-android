package io.github.alelk.pws.database

import timber.log.Timber
import java.io.File
import net.zetetic.database.sqlcipher.SQLiteDatabase as SQLCipherDatabase

/** Schema state of an on-disk database file, read before Room gets to open it. */
internal data class DatabaseFileState(val userVersion: Int, val objectCount: Int)

/**
 * Deletes [dbFile] when it carries a schema Room never finished creating, and reports whether it did.
 *
 * `user_version` stays 0 until Room commits the transaction that creates the schema, and nothing can
 * write user data before that — so a file that already holds schema objects at version 0 is the
 * leftover of an interrupted creation and dropping it loses nothing. Such a leftover is otherwise
 * permanent: Room runs `createAllTables` on every launch as long as the version is 0 (that is its
 * prepackaged-database path), and re-creating `songs_fts` over the FTS4 shadow tables of the
 * previous attempt fails with `vtable constructor failed: songs_fts` — the same crash, forever.
 *
 * Every other file is kept, including one we cannot read at all: a passphrase the Keystore can no
 * longer decrypt must not cost the user their favorites and history.
 */
internal fun discardHalfCreatedDatabase(
  dbFile: File,
  passphrase: ByteArray,
  readState: (File, ByteArray) -> DatabaseFileState? = ::readDatabaseFileState
): Boolean {
  if (!dbFile.isFile || dbFile.length() == 0L) return false
  val state = readState(dbFile, passphrase) ?: return false
  if (state.userVersion != 0 || state.objectCount == 0) {
    Timber.w("database $dbFile did not open, but it holds data (user_version=${state.userVersion}, ${state.objectCount} schema objects) — keeping it")
    return false
  }
  Timber.w("database $dbFile is a leftover of an interrupted creation (user_version=0, ${state.objectCount} schema objects) — deleting it")
  return deleteDatabaseFiles(dbFile)
}

private fun readDatabaseFileState(dbFile: File, passphrase: ByteArray): DatabaseFileState? =
  runCatching {
    // Opened read-write on purpose: a half-created file usually has a hot journal, and rolling it
    // back — which is what tells us the real state — needs write access.
    SQLCipherDatabase.openDatabase(dbFile.absolutePath, passphrase, null, SQLCipherDatabase.OPEN_READWRITE, null).use { db ->
      val objectCount =
        db.rawQuery("SELECT count(*) FROM sqlite_master WHERE name != 'android_metadata'", null)
          .use { if (it.moveToFirst()) it.getInt(0) else 0 }
      DatabaseFileState(userVersion = db.version, objectCount = objectCount)
    }
  }.onFailure { e -> Timber.e(e, "cannot read state of database $dbFile: ${e.message}") }.getOrNull()

/** Removes the database with its journal/WAL side files (mirrors `SQLiteDatabase.deleteDatabase`). */
private fun deleteDatabaseFiles(dbFile: File): Boolean {
  val deleted = dbFile.delete()
  listOf("-journal", "-wal", "-shm").forEach { suffix -> File(dbFile.path + suffix).delete() }
  dbFile.parentFile?.listFiles { file -> file.name.startsWith("${dbFile.name}-mj") }?.forEach { it.delete() }
  return deleted
}
