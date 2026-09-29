package io.github.alelk.pws.android.compose.payment

import androidx.datastore.preferences.core.edit
import io.github.alelk.pws.android.compose.appThemeKey
import io.github.alelk.pws.android.compose.songTextExpandedKey
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/** T-A05: the fork's theme / expanded-text settings are carried over once, without touching the fork file. */
class LegacySettingsImporterTest : FunSpec({

  test("imports app-theme and song-text-expanded, skips song-text-size, leaves the fork file unchanged") {
    val legacyFile = legacyPreferencesFile("lifetime.preferences_pb") // app-theme=dark, expanded=false, size=18
    val hashBefore = legacyFile.sha256()
    val appSettings = FakePreferencesDataStore()
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    try {
      val legacy = LegacyRuStoreEntitlementStore(fileDataStore(legacyFile))
      LegacySettingsImporter(legacy.preferences, appSettings).importOnce()
    } finally {
      scope.cancel()
    }
    appSettings.state.value[appThemeKey] shouldBe "dark"
    appSettings.state.value[songTextExpandedKey] shouldBe false
    appSettings.state.value.asMap().keys.map { it.name }.sorted() shouldBe
      listOf("app-theme", "legacy_settings_imported", "song-text-expanded")
    legacyFile.sha256() shouldBe hashBefore
  }

  test("a second start does not overwrite a theme the user changed in the new app") {
    val legacyFile = legacyPreferencesFile("none.preferences_pb")
    val appSettings = FakePreferencesDataStore()
    val importer = LegacySettingsImporter(LegacyRuStoreEntitlementStore(fileDataStore(legacyFile)).preferences, appSettings)

    importer.importOnce()
    appSettings.state.value[appThemeKey] shouldBe "dark"

    appSettings.edit { it[appThemeKey] = "light" }
    importer.importOnce()
    appSettings.state.value[appThemeKey] shouldBe "light"
  }

  test("an unknown legacy theme id is not imported") {
    val appSettings = FakePreferencesDataStore()
    val legacy = FakePreferencesDataStore(
      androidx.datastore.preferences.core.mutablePreferencesOf(
        androidx.datastore.preferences.core.stringPreferencesKey("app-theme") to "sepia",
      ),
    )
    LegacySettingsImporter(legacy.data, appSettings).importOnce()
    appSettings.state.value[appThemeKey] shouldBe null
  }
})
