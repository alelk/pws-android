package io.github.alelk.pws.database

import android.content.Context
import io.github.alelk.pws.database.PwsDatabaseProvider.DATABASE_NAME
import io.github.alelk.pws.database.helper.unzip
import timber.log.Timber
import java.io.FileOutputStream

private const val ASSETS_DB_FOLDER = "db"

internal fun initDatabase(context: Context) {
  val dbFile = context.getDatabasePath(DATABASE_NAME)
  if (dbFile.exists() && dbFile.isFile) Timber.d("found existing database $dbFile")
  else {
    Timber.i("database $dbFile not found, init new database from assets $ASSETS_DB_FOLDER...")
    val am = context.assets
    val dbFolder = checkNotNull(dbFile.parentFile) { "error getting parent file of target database $dbFile" }
    if (!dbFolder.exists() || !dbFolder.isDirectory) {
      if (!dbFolder.mkdirs()) Timber.e("could not create directory $dbFolder")
    }
    val fileList = checkNotNull(am.list(ASSETS_DB_FOLDER)) { "no database files found in asset directory $ASSETS_DB_FOLDER" }
    check(fileList.isNotEmpty()) { "no database files found in asset directory $ASSETS_DB_FOLDER" }
    val zipFile = dbFolder.resolve(dbFile.name + "z")
    runCatching {
      if (zipFile.exists()) zipFile.delete()
      FileOutputStream(zipFile).use { zipOutputStream ->
        val filenamePrefix = "${DATABASE_NAME}z"
        for (i in 1..fileList.filter { it.contains(filenamePrefix) }.size) {
          val filename = "$ASSETS_DB_FOLDER/${DATABASE_NAME}z.$i"
          am.open(filename).use { inputStream -> inputStream.copyTo(zipOutputStream, bufferSize = 1024) }
          Timber.i("copying success for asset file $filename")
        }
      }
    }.onFailure { e ->
      Timber.e(e, "error copying database file $dbFile from asset folder $ASSETS_DB_FOLDER: ${e.message}")
      if (zipFile.exists()) zipFile.delete()
    }.getOrThrow()
    zipFile.unzip(dbFolder)
    check(dbFile.exists() && dbFile.isFile) { "database file from asset $ASSETS_DB_FOLDER has invalid name! expected $dbFile" }
  }
}