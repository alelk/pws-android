package io.github.alelk.pws.database

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.runBlocking

object PwsDatabaseProvider {
  @Volatile
  private var INSTANCE: PwsDatabase? = null

  fun getDatabase(context: Context): PwsDatabase {
    return INSTANCE ?: synchronized(this) {
      initDatabase(context)
      val instance = Room.databaseBuilder(context.applicationContext, PwsDatabase::class.java, DATABASE_NAME).addCallback(callback = databaseCallbacks).build()
      INSTANCE = instance
      runBlocking { migrateDataFromPrevDatabase(context, instance) }
      instance
    }
  }

  const val DATABASE_NAME = "pws.2.0.0.db"
}
