package io.github.alelk.pws.android.compose.di

import io.github.alelk.pws.android.compose.AndroidAppStartupTasks
import io.github.alelk.pws.android.compose.BackupManager
import io.github.alelk.pws.android.compose.LegacyMigrationGate
import io.github.alelk.pws.database.PwsDatabase
import io.github.alelk.pws.features.platform.AppStartupTasks
import kotlinx.coroutines.CompletableDeferred
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * [legacyMigrationDone] opens once the legacy-database migration has finished; seeding, onboarding and
 * the pending backup restore wait for it (see [LegacyMigrationGate]).
 */
internal fun startupModule(legacyMigrationDone: CompletableDeferred<Unit>) = module {
  single { LegacyMigrationGate(legacyMigrationDone) }
  // The platform half of pws-core's AppStartupModel (featuresModule): seeding and the post-install
  // migration retry / backup restore, both behind the gate above.
  single<AppStartupTasks> {
    AndroidAppStartupTasks(
      context = androidContext(),
      migrationGate = get(),
      telemetry = get(),
      seedBooksFromAssets = get(),
      database = { get<PwsDatabase>() },
      backupManager = { get<BackupManager>() },
    )
  }
}
