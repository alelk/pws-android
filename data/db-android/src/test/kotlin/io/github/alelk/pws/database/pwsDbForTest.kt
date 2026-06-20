package io.github.alelk.pws.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import java.io.File

fun pwsDbForTest(inMemory: Boolean, name: String): PwsDatabase {
  val context = ApplicationProvider.getApplicationContext<Context>()
  return if (inMemory) Room.inMemoryDatabaseBuilder(context, PwsDatabase::class.java).build()
  else Room.databaseBuilder(context, PwsDatabase::class.java, name).build()
}

/**
 * Opens [dbFile] (an existing, current-version SQLite file) as a Room [PwsDatabase].
 *
 * Room copies the prepackaged file into the app database directory and validates its schema
 * identity hash against the generated schema, so opening fails if [dbFile] is out of sync with
 * the Room entities.
 */
fun pwsDbFromFile(dbFile: File, name: String = "pws-from-file.db"): PwsDatabase {
  val context = ApplicationProvider.getApplicationContext<Context>()
  context.getDatabasePath(name).also { dest ->
    if (dest.exists()) dest.delete()
  }
  return Room.databaseBuilder(context, PwsDatabase::class.java, name)
    .createFromFile(dbFile)
    .build()
}
