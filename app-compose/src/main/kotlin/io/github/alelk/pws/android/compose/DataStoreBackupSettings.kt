package io.github.alelk.pws.android.compose

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import io.github.alelk.pws.portable.backup.BackupSettingsPort
import kotlinx.coroutines.flow.first

/**
 * [BackupSettingsPort] over the app settings DataStore: the raw value of [appThemeKey] (G3), exactly as the backup has
 * always read and written it — an unset key is absent from the backup, a stored value goes into it as is.
 */
class DataStoreBackupSettings(private val dataStore: DataStore<Preferences>) : BackupSettingsPort {
  override suspend fun read(): Map<String, String> {
    val theme = dataStore.data.first()[appThemeKey]
    return if (theme == null) emptyMap() else mapOf(appThemeKey.name to theme)
  }

  override suspend fun apply(settings: Map<String, String>) {
    val theme = settings[appThemeKey.name] ?: return
    dataStore.edit { it[appThemeKey] = theme }
  }
}
