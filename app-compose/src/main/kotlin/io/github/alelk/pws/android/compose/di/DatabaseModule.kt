package io.github.alelk.pws.android.compose.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import io.github.alelk.pws.android.compose.BackupManager
import io.github.alelk.pws.android.compose.DataStoreBackupSettings
import io.github.alelk.pws.android.compose.appSettingsDataStore
import io.github.alelk.pws.database.PwsDatabase
import io.github.alelk.pws.database.PwsDatabaseProvider
import io.github.alelk.pws.portable.backup.BackupSettingsPort
import io.github.alelk.pws.portable.backup.ExportBackupUseCase
import io.github.alelk.pws.portable.backup.RestoreBackupUseCase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

internal val databaseModule = module {
  single<PwsDatabase> { PwsDatabaseProvider.getDatabase(androidContext()) }
  single<DataStore<Preferences>> { androidContext().appSettingsDataStore() }
  // Backup use cases over the repositories of repoRoomModule; settings: the raw `app-theme` of the DataStore above.
  single<BackupSettingsPort> { DataStoreBackupSettings(get<DataStore<Preferences>>()) }
  single { ExportBackupUseCase(get(), get(), get(), get(), get(), get(), get(), get()) }
  single { RestoreBackupUseCase(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
  single { BackupManager(get(), get()) }
}
