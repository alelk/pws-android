package io.github.alelk.pws.database

import android.content.Context
import androidx.room.Room
import io.github.alelk.pws.database.security.KeyManager
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import timber.log.Timber

object PwsDatabaseProvider {
  @Volatile
  private var INSTANCE: PwsDatabase? = null

  fun getDatabase(context: Context): PwsDatabase = INSTANCE ?: synchronized(this) {
    if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())

    val passphrase = KeyManager.getOrCreatePassphrase(context)

    // Copy and encrypt database from asset on the first app start
    initDatabase(context, passphrase)

    val instance = Room.databaseBuilder(context.applicationContext, PwsDatabase::class.java, DATABASE_NAME)
      .openHelperFactory(SupportOpenHelperFactory(passphrase))
      .addCallback(callback = databaseCallbacks)
      .build()
    INSTANCE = instance

    // Migrate user data from previous database version
    runBlocking { migrateDataFromPrevDatabase(context, instance) }
    instance
  }

  const val DB_VERSION = "3.3.0"
  const val DATABASE_NAME = "pws.$DB_VERSION.db"
}
