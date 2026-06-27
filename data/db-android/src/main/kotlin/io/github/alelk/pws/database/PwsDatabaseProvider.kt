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

    System.loadLibrary("sqlcipher")

    val passphrase = if (BuildConfig.DB_ENCRYPTED) KeyManager.getOrCreatePassphrase(context) else ByteArray(0)

    // Copy and encrypt database from asset on the first app start
    initDatabase(context, passphrase)

    val instance =
      Room
        .databaseBuilder(context.applicationContext, PwsDatabase::class.java, DATABASE_NAME)
        .openHelperFactory(SupportOpenHelperFactory(passphrase))
        .addCallback(callback = databaseCallbacks)
        .build()
    INSTANCE = instance

    runBlocking {
      migrateDataFromPrevDatabase(context, instance, passphrase)
    }
    instance
  }

  const val DATABASE_NAME = "pws.db"
}
