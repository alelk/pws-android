package io.github.alelk.pws.database

import android.database.sqlite.SQLiteDatabase
import io.github.alelk.pws.database.helper.unzip
import java.io.File
import kotlin.io.path.createTempDirectory

internal inline fun <T> withSqliteDb(
  dbZipFile: File,
  patches: List<TestDbPatch> = emptyList(),
  readOnly: Boolean = true,
  body: (db: MigrationDbSource) -> T,
): T {
  val filename = dbZipFile.name.removeSuffix(".dbz") + ".db"
  // Own temp dir per call: the per-flavor test tasks of this module run in parallel, and a shared
  // directory let them delete or overwrite each other's database files.
  val dir = createTempDirectory("pws-test-db").toFile()
  val targetFile = File(dir, filename)
  return try {
    println("unzip test database file $dbZipFile to $targetFile...")
    dbZipFile.unzip(dir)
    applyPatches(targetFile, patches)
    AndroidSQLiteDbSource(
      SQLiteDatabase.openDatabase(
        targetFile.path,
        null,
        if (readOnly) SQLiteDatabase.OPEN_READONLY else SQLiteDatabase.OPEN_READWRITE,
      ),
    ).use(body)
  } finally {
    println("delete test database file $targetFile")
    dir.deleteRecursively()
  }
}

internal fun applyPatches(dbFile: File, patches: List<TestDbPatch>) {
  if (patches.isEmpty()) return
  SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
    patches.forEach { patch ->
      println("apply database patch ${patch::class.simpleName}...")
      patch.apply(db)
    }
  }
}

internal inline fun <T> withPwsDb(
  dbZipFile: File = File("src/test/resources/test-db/v11/pws.2.0.0.dbz"),
  patches: List<TestDbPatch> = emptyList(),
  readOnly: Boolean = false,
  body: (db: PwsDatabase) -> T,
): T = withSqliteDb(dbZipFile, patches, readOnly) { db ->
  val path = db.path
  db.close()
  println("open pws database $path...")
  val database = pwsDbFromFile(File(path))
  try {
    body(database)
  } finally {
    database.close()
  }
}
