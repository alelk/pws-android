package io.github.alelk.pws.android.compose.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import io.github.alelk.pws.android.compose.DataStoreUserPreferencesRepository
import io.github.alelk.pws.domain.preferences.repository.UserPreferencesRepository
import org.koin.dsl.module

/**
 * The settings repository over the DataStore of [databaseModule]. Loaded after featuresModule so it
 * overrides its in-memory default.
 */
internal val preferencesModule = module {
  single<UserPreferencesRepository> { DataStoreUserPreferencesRepository(get<DataStore<Preferences>>()) }
}
