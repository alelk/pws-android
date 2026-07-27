package io.github.alelk.pws.database

import android.content.Context
import androidx.room.Room
import io.github.alelk.pws.database.security.KeyManager
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import timber.log.Timber

object PwsDatabaseProvider {
  @Volatile
  private var INSTANCE: PwsDatabase? = null

  fun getDatabase(context: Context): PwsDatabase = INSTANCE ?: synchronized(this) {
    INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
  }

  private fun buildDatabase(context: Context): PwsDatabase {
    if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
    System.loadLibrary("sqlcipher")
    initDatabase(context)
    val passphrase = if (BuildConfig.DB_ENCRYPTED) KeyManager.getOrCreatePassphrase(context) else ByteArray(0)
    return Room
      .databaseBuilder(context.applicationContext, PwsDatabase::class.java, DATABASE_NAME)
      .openHelperFactory(SupportOpenHelperFactory(passphrase))
      .addMigrations(MIGRATION_14_15)
      .build()
  }

  /** Must be called once on a background thread after the database is first obtained. */
  suspend fun runLegacyMigration(context: Context, database: PwsDatabase) {
    val passphrase = if (BuildConfig.DB_ENCRYPTED) KeyManager.getOrCreatePassphrase(context) else ByteArray(0)
    migrateDataFromPrevDatabase(context, database, passphrase)
  }

  const val DATABASE_NAME = "pws.db"
}
