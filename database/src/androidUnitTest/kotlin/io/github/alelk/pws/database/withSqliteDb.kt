package io.github.alelk.pws.database

import android.database.sqlite.SQLiteDatabase
import io.github.alelk.pws.database.helper.unzip
import java.io.File

inline fun <T> withSqliteDb(dbZipFile: File, body: (db: SQLiteDatabase) -> T): T {
  val filename = dbZipFile.name.removeSuffix(".dbz") + ".db"
  val targetFile = File("test-db/$filename")
  if (targetFile.exists()) targetFile.delete()
  return try {
    println("unzip test database file $dbZipFile to $targetFile...")
    dbZipFile.unzip(File("test-db/"))
    SQLiteDatabase
      .openDatabase(targetFile.path, null, 0)
      .use(body)
  } finally {
    println("delete test database file $targetFile")
    if (targetFile.exists()) targetFile.delete()
  }
}