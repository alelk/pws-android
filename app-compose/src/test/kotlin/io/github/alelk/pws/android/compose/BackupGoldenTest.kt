package io.github.alelk.pws.android.compose

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import br.com.colman.kotest.FeatureSpec
import br.com.colman.kotest.android.extensions.robolectric.RobolectricTest
import io.github.alelk.pws.database.PwsDatabase
import io.github.alelk.pws.database.backup.BackupGolden
import io.github.alelk.pws.database.backup.BackupScenario
import io.github.alelk.pws.database.backup.BackupScenario.dumpUserTables
import io.github.alelk.pws.database.backup.BackupScenario.seedCatalog
import io.github.alelk.pws.database.backup.BackupScenario.seedRestoreTarget
import io.github.alelk.pws.database.backup.BackupScenario.seedUserData
import io.github.alelk.pws.domain.core.Color
import io.github.alelk.pws.domain.core.Locale
import io.github.alelk.pws.domain.core.Version
import io.github.alelk.pws.portable.BackupService
import io.github.alelk.pws.portable.model.Backup
import io.github.alelk.pws.portable.model.BookPreference
import io.github.alelk.pws.portable.model.Song
import io.github.alelk.pws.portable.model.SongNumber
import io.github.alelk.pws.portable.model.Tag
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import org.koin.core.context.stopKoin
import java.io.File
import java.util.UUID

/**
 * Golden (characterization) test of [BackupManager] (plan 2026-09-30, step 06.2): the shared [BackupScenario] is
 * exported to YAML and its restore input is restored, and both results must equal the snapshots in [BackupGolden],
 * recorded from the pre-refactoring implementation. `:portable-data` checks its use cases against the same snapshots.
 */
@RobolectricTest(sdk = [34])
class BackupGoldenTest :
  FeatureSpec({

    afterTest { runCatching { stopKoin() } }

    fun inMemoryDb(): PwsDatabase {
      val ctx = ApplicationProvider.getApplicationContext<Context>()
      return Room.inMemoryDatabaseBuilder(ctx, PwsDatabase::class.java).allowMainThreadQueries().build()
    }

    fun dataStore(scope: CoroutineScope): DataStore<Preferences> {
      val ctx = ApplicationProvider.getApplicationContext<Context>()
      val file = File(ctx.filesDir, "ds-${UUID.randomUUID()}.preferences_pb")
      return PreferenceDataStoreFactory.create(scope = scope) { file }
    }

    feature("backup golden snapshots") {
      scenario("export of the scenario equals the golden YAML") {
        runTest {
          val source = inMemoryDb()
          source.seedCatalog()
          source.seedUserData()
          val ds = dataStore(backgroundScope)
          ds.edit { it[stringPreferencesKey("app-theme")] = "dark" }

          val backup = BackupManager(source, ds).exportBackup(source = "golden")

          BackupService().writeAsString(backup.pinnedMetadata()) shouldBe BackupGolden.EXPORT_YAML
          source.close()
        }
      }

      scenario("restore of the scenario input writes the golden tables") {
        runTest {
          val dest = inMemoryDb()
          dest.seedCatalog()
          dest.seedRestoreTarget()
          val ds = dataStore(backgroundScope)

          BackupManager(dest, ds).restoreBackup(backupGoldenRestoreInput())

          dest.dumpUserTables() shouldBe BackupGolden.RESTORED_TABLES
          ds.data.first()[stringPreferencesKey("app-theme")] shouldBe "dark"
          dest.close()
        }
      }
    }

    feature("theme setting, raw as stored (G2)") {
      val theme = stringPreferencesKey("app-theme")

      scenario("an unset theme is not exported") {
        runTest {
          val db = inMemoryDb()
          db.seedCatalog()

          BackupManager(db, dataStore(backgroundScope)).exportBackup(source = "t").settings shouldBe emptyMap()
          db.close()
        }
      }

      scenario("a theme stored as `system` is exported as `system`") {
        runTest {
          val db = inMemoryDb()
          db.seedCatalog()
          val ds = dataStore(backgroundScope)
          ds.edit { it[theme] = "system" }

          BackupManager(db, ds).exportBackup(source = "t").settings shouldBe mapOf("app-theme" to "system")
          db.close()
        }
      }

      scenario("restoring `system` over a dark theme stores `system`") {
        runTest {
          val db = inMemoryDb()
          db.seedCatalog()
          val ds = dataStore(backgroundScope)
          ds.edit { it[theme] = "dark" }

          BackupManager(db, ds).restoreBackup(Backup(settings = mapOf("app-theme" to "system")))

          ds.data.first()[theme] shouldBe "system"
          db.close()
        }
      }
    }
  })

private fun Backup.pinnedMetadata(): Backup {
  val metadata = Backup.Metadata(createdAt = LocalDateTime(2026, 1, 1, 0, 0), source = "golden")
  return copy(metadata = metadata)
}

/**
 * The golden export plus records the target cannot take (missing numbers / books) and a tag named like a predefined
 * one. Same as `goldenRestoreInput()` of pws-core's `BackupUseCasesTest`: keep the two in step.
 */
internal fun backupGoldenRestoreInput(): Backup {
  val backup = BackupService().readFromString(BackupGolden.EXPORT_YAML)
  val missing = SongNumber(BackupScenario.book1, 999)
  val ghost = Song(number = missing, version = Version(1, 0), locale = Locale.RU, name = "Ghost", lyric = "ghost")
  val sharedSongs = setOf(SongNumber(BackupScenario.book1, 2), SongNumber(BackupScenario.book1, 3), missing)
  return backup.copy(
    songs = backup.songs.orEmpty() + ghost,
    favorites = backup.favorites.orEmpty() + missing,
    tags = backup.tags.orEmpty() + Tag(name = "Shared", color = Color(0x0a, 0x0b, 0x0c), songs = sharedSongs),
    bookPreferences = backup.bookPreferences.orEmpty() + BookPreference(BackupScenario.missingBook, 7),
    history = backup.history.orEmpty() + Backup.HistoryEntry(missing, LocalDateTime(2026, 2, 2, 2, 2)),
  )
}
