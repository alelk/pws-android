package io.github.alelk.pws.database

import androidx.room.Room

actual fun pwsDbForTest(inMemory: Boolean, name: String): PwsDatabase =
  if (inMemory) Room.inMemoryDatabaseBuilder<PwsDatabase>().build()
  else Room.databaseBuilder<PwsDatabase>(name).build()