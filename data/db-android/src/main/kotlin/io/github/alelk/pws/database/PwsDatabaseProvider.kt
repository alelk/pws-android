package io.github.alelk.pws.database

import android.content.Context
import androidx.room.Room
import io.github.alelk.pws.database.security.KeyManager
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

object PwsDatabaseProvider {
  @Volatile
  private var INSTANCE: PwsDatabase? = null

  fun getDatabase(context: Context): PwsDatabase = INSTANCE ?: synchronized(this) {
    INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
  }

  private fun buildDatabase(context: Context): PwsDatabase {
    System.loadLibrary("sqlcipher")
    initDatabase(context)
    val passphrase = if (BuildConfig.DB_ENCRYPTED) KeyManager.getOrCreatePassphrase(context) else ByteArray(0)
    return Room
      .databaseBuilder(context.applicationContext, PwsDatabase::class.java, DATABASE_NAME)
      .openHelperFactory(SelfHealingOpenHelperFactory(SupportOpenHelperFactory(passphrase), passphrase))
      .addMigrations(MIGRATION_14_15)
      .build()
  }

  private val legacyMigrationMutex = Mutex()

  /**
   * Migrates user data from legacy database files (older app versions / the RuStore fork) into
   * [database]. Must run on a background thread, once at app start **before** anything installs
   * books (seeding, onboarding, pending backup restore) — see `LegacyMigrationGate` in the shell.
   *
   * [countAttempt] = `true` for the startup run (bounded by the retry cap); pass `false` for the
   * extra retries triggered by a book install, see [hasPendingLegacyMigration]. Runs are serialised.
   *
   * @return one outcome per legacy file found (empty when there is nothing to migrate).
   */
  suspend fun runLegacyMigration(
    context: Context,
    database: PwsDatabase,
    countAttempt: Boolean = true,
  ): List<LegacyMigrationOutcome> = legacyMigrationMutex.withLock {
    if (!hasPendingLegacyDatabase(context)) return@withLock emptyList()
    val passphrase = if (BuildConfig.DB_ENCRYPTED) KeyManager.getOrCreatePassphrase(context) else ByteArray(0)
    migrateDataFromPrevDatabase(context, database, passphrase, countAttempt)
  }

  /**
   * True while a legacy database file is still waiting (e.g. a previous run could not match
   * records of books that were not installed yet). Cheap: file existence checks only.
   */
  fun hasPendingLegacyMigration(context: Context): Boolean = hasPendingLegacyDatabase(context)

  const val DATABASE_NAME = "pws.db"
}
