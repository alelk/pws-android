package io.github.alelk.pws.database

import android.content.Context
import io.github.alelk.pws.database.PwsDatabaseProvider.DATABASE_NAME
import timber.log.Timber

/** Ensures the database directory exists before Room tries to create the file. */
internal fun initDatabase(context: Context) {
  val dbDir = context.getDatabasePath(DATABASE_NAME).parentFile ?: return
  if (!dbDir.exists()) {
    check(dbDir.mkdirs() || dbDir.exists()) { "failed to create database dir $dbDir" }
    Timber.i("created database dir: $dbDir")
  }
}
