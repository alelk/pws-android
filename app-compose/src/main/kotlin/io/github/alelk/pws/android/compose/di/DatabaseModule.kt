package io.github.alelk.pws.android.compose.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import io.github.alelk.pws.android.compose.BackupManager
import io.github.alelk.pws.android.compose.appSettingsDataStore
import io.github.alelk.pws.database.PwsDatabase
import io.github.alelk.pws.database.PwsDatabaseProvider
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

internal val databaseModule = module {
  single<PwsDatabase> { PwsDatabaseProvider.getDatabase(androidContext()) }
  single<DataStore<Preferences>> { androidContext().appSettingsDataStore() }
  single { BackupManager(get<PwsDatabase>(), get<DataStore<Preferences>>()) }
}
