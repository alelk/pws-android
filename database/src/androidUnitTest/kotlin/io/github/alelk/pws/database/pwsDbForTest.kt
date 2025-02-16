package io.github.alelk.pws.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider

actual fun pwsDbForTest(inMemory: Boolean, name: String): PwsDatabase {
  val context = ApplicationProvider.getApplicationContext<Context>()
  return if (inMemory) Room.inMemoryDatabaseBuilder(context, PwsDatabase::class.java).build()
  else Room.databaseBuilder(context, PwsDatabase::class.java, name).build()
}
