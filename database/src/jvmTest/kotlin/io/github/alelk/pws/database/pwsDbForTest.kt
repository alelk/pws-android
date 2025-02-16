package io.github.alelk.pws.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

actual fun pwsDbForTest(inMemory: Boolean, name: String): PwsDatabase =
  if (inMemory) Room.inMemoryDatabaseBuilder<PwsDatabase>().setDriver(BundledSQLiteDriver()).build()
  else Room.databaseBuilder<PwsDatabase>(name).build()