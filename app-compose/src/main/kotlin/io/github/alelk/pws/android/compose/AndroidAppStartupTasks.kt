package io.github.alelk.pws.android.compose

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import io.github.alelk.pws.contentdelivery.install.SeedBooksFromAssetsUseCase
import io.github.alelk.pws.database.LegacyMigrationOutcome
import io.github.alelk.pws.database.PwsDatabase
import io.github.alelk.pws.database.PwsDatabaseProvider
import io.github.alelk.pws.domain.telemetry.Telemetry
import io.github.alelk.pws.features.platform.AppStartupTasks
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android [AppStartupTasks]: the startup work of `MainActivity`, moved as is (plan 2026-09-30, step 05.1).
 *
 * Both tasks wait for [migrationGate] (README §6 p. 2): a book seeded or a backup restored before the
 * legacy-database migration has finished would cost the user the favorites and history of every
 * other book (plan 2026-09-29, C2).
 */
class AndroidAppStartupTasks internal constructor(
  private val migrationGate: LegacyMigrationGate,
  private val telemetry: Telemetry,
  private val operations: StartupOperations,
  private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AppStartupTasks {

  constructor(
    context: Context,
    migrationGate: LegacyMigrationGate,
    telemetry: Telemetry,
    seedBooksFromAssets: SeedBooksFromAssetsUseCase,
    database: () -> PwsDatabase,
    dataStore: () -> DataStore<Preferences>,
  ) : this(
    migrationGate = migrationGate,
    telemetry = telemetry,
    operations = StartupOperations(
      seedBooksFromAssets = { seedBooksFromAssets.invoke() },
      hasPendingLegacyMigration = { PwsDatabaseProvider.hasPendingLegacyMigration(context) },
      retryLegacyMigration = { PwsDatabaseProvider.runLegacyMigration(context, database(), countAttempt = false) },
      applyPendingRestore = { PwsBackupAgent.applyPendingRestoreIfNeeded(context, database(), dataStore()) },
    ),
  )

  /** The platform calls behind the tasks; a seam for tests. */
  internal class StartupOperations(
    val seedBooksFromAssets: suspend () -> Boolean,
    val hasPendingLegacyMigration: () -> Boolean,
    val retryLegacyMigration: suspend () -> List<LegacyMigrationOutcome>,
    val applyPendingRestore: suspend () -> Unit,
  )

  override suspend fun seedPreloadedBooks(): Boolean = migrationGate.afterMigration { operations.seedBooksFromAssets() }

  // Re-apply pending user data on every new book install — a partially migrated legacy database and a
  // pending backup restore are both kept until their books are installed, so each new install may
  // unlock more records. Both only after the startup migration finished.
  override suspend fun onBooksInstalled(count: Int) {
    if (count > 0) {
      migrationGate.await()
      withContext(ioDispatcher) {
        if (operations.hasPendingLegacyMigration()) {
          runCatching { operations.retryLegacyMigration() }
            .onSuccess { telemetry.reportLegacyMigration(it) }
            .onFailure { telemetry.recordError(it, "legacy_migration_retry_failed") }
        }
        operations.applyPendingRestore()
      }
    }
  }
}
