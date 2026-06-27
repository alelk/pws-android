package io.github.alelk.pws.database

import android.database.Cursor
import java.io.Closeable
import android.database.sqlite.SQLiteDatabase as AndroidSQLiteDatabase
import net.zetetic.database.sqlcipher.SQLiteDatabase as SQLCipherDatabase

/**
 * Read-only view over a previous on-disk PWS database, decoupled from the concrete driver.
 *
 * Legacy databases (≤ 3.0.0) are plain SQLite files opened with [AndroidSQLiteDatabase]; databases
 * starting from 3.2.3 are SQLCipher-encrypted and opened with [SQLCipherDatabase]. Both drivers
 * expose the same query surface but are unrelated Java types, so the migration readers
 * ([io.github.alelk.pws.database.support.PwsDbDataProvider] implementations and
 * [io.github.alelk.pws.database.support.fetchData]) depend on this abstraction instead.
 */
internal interface MigrationDbSource : Closeable {
  /** SQLite `user_version` (the PWS schema version) of the source database. */
  val version: Int

  /** On-disk path of the source database (used for logging). */
  val path: String

  /** Mirrors `SQLiteDatabase.query(boolean, …)` of both drivers; both return [android.database.Cursor]. */
  fun query(
    distinct: Boolean,
    table: String,
    columns: Array<String>,
    selection: String?,
    selectionArgs: Array<String>?,
    groupBy: String?,
    having: String?,
    orderBy: String?,
    limit: String?
  ): Cursor
}

/** [MigrationDbSource] backed by a plain (unencrypted) [AndroidSQLiteDatabase]. */
internal class AndroidSQLiteDbSource(private val db: AndroidSQLiteDatabase) : MigrationDbSource {
  override val version: Int get() = db.version
  override val path: String get() = db.path ?: ""
  override fun query(
    distinct: Boolean, table: String, columns: Array<String>, selection: String?,
    selectionArgs: Array<String>?, groupBy: String?, having: String?, orderBy: String?, limit: String?
  ): Cursor = db.query(distinct, table, columns, selection, selectionArgs, groupBy, having, orderBy, limit)

  override fun close() = db.close()
}

/** [MigrationDbSource] backed by a SQLCipher-encrypted [SQLCipherDatabase]. */
internal class SqlCipherDbSource(private val db: SQLCipherDatabase) : MigrationDbSource {
  override val version: Int get() = db.version
  override val path: String get() = db.path ?: ""
  override fun query(
    distinct: Boolean, table: String, columns: Array<String>, selection: String?,
    selectionArgs: Array<String>?, groupBy: String?, having: String?, orderBy: String?, limit: String?
  ): Cursor = db.query(distinct, table, columns, selection, selectionArgs, groupBy, having, orderBy, limit)

  override fun close() = db.close()
}
