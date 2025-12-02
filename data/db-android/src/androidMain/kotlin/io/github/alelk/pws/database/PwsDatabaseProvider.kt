package io.github.alelk.pws.database

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.runBlocking
import timber.log.Timber

object PwsDatabaseProvider {
  @Volatile
  private var INSTANCE: PwsDatabase? = null

  fun getDatabase(context: Context): PwsDatabase = INSTANCE ?: synchronized(this) {
    // setup logging
    if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())

    // copy database from asset on the first app start
    initDatabase(context)

    val instance = Room.databaseBuilder(context.applicationContext, PwsDatabase::class.java, DATABASE_NAME).addCallback(callback = databaseCallbacks).build()
    INSTANCE = instance

    // migrate user data from previous database
    runBlocking { migrateDataFromPrevDatabase(context, instance) }
    instance
  }

  const val DATABASE_NAME = "pws.2.0.0.db"
}
