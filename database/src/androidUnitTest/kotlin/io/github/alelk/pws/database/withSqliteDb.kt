package io.github.alelk.pws.database

import android.database.sqlite.SQLiteDatabase
import io.github.alelk.pws.database.helper.unzip
import java.io.File

inline fun <T> withSqliteDb(dbZipFile: File, patches: List<TestDbPatch> = emptyList(), readOnly: Boolean = true, body: (db: SQLiteDatabase) -> T): T {
  val filename = dbZipFile.name.removeSuffix(".dbz") + ".db"
  val targetFile = File("test-db/$filename")
  if (targetFile.exists()) targetFile.delete()
  return try {
    println("unzip test database file $dbZipFile to $targetFile...")
    dbZipFile.unzip(File("test-db/"))
    if (patches.isNotEmpty())
      SQLiteDatabase.openDatabase(targetFile.path, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
        patches.forEach { patch ->
          println("apply database patch ${patch::class.simpleName}...")
          patch.apply(db)
        }
      }
    SQLiteDatabase
      .openDatabase(targetFile.path, null, if (readOnly) SQLiteDatabase.OPEN_READONLY else SQLiteDatabase.OPEN_READWRITE)
      .use(body)
  } finally {
    println("delete test database file $targetFile")
    if (targetFile.exists()) targetFile.delete()
  }
}

inline fun <T> withPwsDb(
  dbZipFile: File = File("src/androidUnitTest/resources/test-db/v11/pws.2.0.0.dbz"),
  patches: List<TestDbPatch> = emptyList(),
  readOnly: Boolean = false,
  body: (db: PwsDatabase) -> T
): T =
  withSqliteDb(dbZipFile, patches, readOnly) { db ->
    db.close()
    println("open pws database ${db.path}...")
    body(pwsDbForTest(inMemory = false, db.path))
  }
