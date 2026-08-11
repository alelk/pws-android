package io.github.alelk.pws.database

import android.database.sqlite.SQLiteException
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import timber.log.Timber

/**
 * Wraps [delegate] so a database file left half-created is discarded and built again instead of
 * crashing the app on every launch.
 *
 * Nothing is thrown away blindly: the retry only happens for a file that cannot hold user data
 * yet — see [discardHalfCreatedDatabase] — and only once per helper, so a failure that survives the
 * fresh start still surfaces.
 */
internal class SelfHealingOpenHelperFactory(
  private val delegate: SupportSQLiteOpenHelper.Factory,
  private val passphrase: ByteArray
) : SupportSQLiteOpenHelper.Factory {

  override fun create(configuration: SupportSQLiteOpenHelper.Configuration): SupportSQLiteOpenHelper =
    SelfHealingOpenHelper(
      newDelegate = { delegate.create(configuration) },
      discardDatabase = {
        val name = configuration.name
        name != null && discardHalfCreatedDatabase(configuration.context.getDatabasePath(name), passphrase)
      }
    )
}

internal class SelfHealingOpenHelper(
  private val newDelegate: () -> SupportSQLiteOpenHelper,
  private val discardDatabase: () -> Boolean
) : SupportSQLiteOpenHelper {

  private var delegate: SupportSQLiteOpenHelper = newDelegate()
  private var writeAheadLoggingEnabled: Boolean? = null
  private var healed = false

  override val databaseName: String? get() = delegate.databaseName

  override val writableDatabase: SupportSQLiteDatabase get() = open { it.writableDatabase }

  override val readableDatabase: SupportSQLiteDatabase get() = open { it.readableDatabase }

  override fun setWriteAheadLoggingEnabled(enabled: Boolean) {
    writeAheadLoggingEnabled = enabled
    delegate.setWriteAheadLoggingEnabled(enabled)
  }

  override fun close() = delegate.close()

  private fun open(openDatabase: (SupportSQLiteOpenHelper) -> SupportSQLiteDatabase): SupportSQLiteDatabase =
    try {
      openDatabase(delegate)
    } catch (e: SQLiteException) {
      // The database file itself is broken (e.g. `vtable constructor failed: songs_fts`).
      retryOnFreshDatabase(e, openDatabase)
    } catch (e: IllegalStateException) {
      // Room rejected the schema it found at version 0 ("Pre-packaged database has an invalid schema").
      retryOnFreshDatabase(e, openDatabase)
    }

  private fun retryOnFreshDatabase(
    error: Throwable,
    openDatabase: (SupportSQLiteOpenHelper) -> SupportSQLiteDatabase
  ): SupportSQLiteDatabase {
    if (healed) throw error
    healed = true
    Timber.e(error, "failed to open database: ${error.message}")
    runCatching { delegate.close() }
    if (!discardDatabase()) throw error
    delegate = newDelegate().also { helper -> writeAheadLoggingEnabled?.let(helper::setWriteAheadLoggingEnabled) }
    return openDatabase(delegate)
  }
}
