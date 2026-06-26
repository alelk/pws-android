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
import io.github.alelk.pws.database.book.bookEntity
import io.github.alelk.pws.database.bookstatistic.BookStatisticEntity
import io.github.alelk.pws.database.song.songEntity
import io.github.alelk.pws.database.song_number.songNumberEntity
import io.github.alelk.pws.database.history.HistoryEntity
import io.github.alelk.pws.database.song_tag.SongTagEntity
import io.github.alelk.pws.database.tag.tagEntity
import io.github.alelk.pws.domain.core.Color
import io.github.alelk.pws.domain.core.ids.BookId
import io.github.alelk.pws.domain.core.ids.SongId
import io.github.alelk.pws.domain.core.ids.SongNumberId
import io.github.alelk.pws.portable.model.SongNumber
import kotlinx.datetime.LocalDateTime
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.RandomSource
import io.kotest.property.arbitrary.constant
import io.kotest.property.arbitrary.next
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import org.koin.core.context.stopKoin
import java.io.File
import java.util.UUID

/**
 * Round-trip tests for [BackupManager]. Backup is the user's last line of defence against data
 * loss, so the key guarantee is that everything a user can customise survives an
 * export → restore cycle: favourites, edited song lyrics, custom tags, book priorities and the
 * theme setting.
 */
@RobolectricTest(sdk = 34)
class BackupManagerTest : FeatureSpec({

  val bookId = BookId.parse("Book-1")
  val rs = RandomSource.seeded(20260621L)

  // Robolectric instantiates the real PwsComposeApplication for each test, and its onCreate
  // starts Koin. Stop it between tests so the next Application.onCreate can start it cleanly.
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

  // Seeds the catalogue content (book + two songs + numbers + statistic row) that user data
  // references. Both the source and destination databases must share this base content. Content is
  // produced by the db-room `Arb` entity generators with ids/numbers pinned, the rest randomised.
  suspend fun seedBaseContent(db: PwsDatabase) {
    db.bookDao().insert(Arb.bookEntity(id = Arb.constant(bookId)).next(rs))
    db.bookStatisticDao().insert(BookStatisticEntity(id = bookId, priority = 0))
    db.songDao().insert(
      listOf(
        Arb.songEntity(id = Arb.constant(SongId(1L))).next(rs),
        Arb.songEntity(id = Arb.constant(SongId(2L))).next(rs),
      )
    )
    db.songNumberDao().insert(
      listOf(
        Arb.songNumberEntity(bookId = Arb.constant(bookId), songId = Arb.constant(SongId(1L)), number = Arb.constant(1), priority = Arb.constant(0)).next(rs),
        Arb.songNumberEntity(bookId = Arb.constant(bookId), songId = Arb.constant(SongId(2L)), number = Arb.constant(2), priority = Arb.constant(0)).next(rs),
      )
    )
  }

  feature("export/restore round-trip") {
    scenario("favourites, edited songs, custom tags, book prefs and theme all survive") {
      runTest {
        // --- source DB with user data ---
        val source = inMemoryDb()
        seedBaseContent(source)

        // favourite song #1
        source.favoriteDao().addToFavorites(SongNumberId(bookId, SongId(1L)))
        // edited lyric on song #2
        source.songDao().update(source.songDao().getById(SongId(2L))!!.copy(lyric = "my edited lyric", edited = true))
        // custom tag on song #1
        val tagId = source.tagDao().getNextCustomTagId()
        source.tagDao().insert(
          Arb.tagEntity(
            id = Arb.constant(tagId),
            name = Arb.constant("My Tag"),
            color = Arb.constant(Color.parse("#123456")),
            predefined = Arb.constant(false),
          ).next(rs)
        )
        source.songTagDao().insert(SongTagEntity(songId = SongId(1L), tagId = tagId))
        // book priority
        source.bookStatisticDao().upsert(BookStatisticEntity(id = bookId, priority = 5))
        // history entry for song #1
        source.historyDao().insert(HistoryEntity(SongNumberId(bookId, SongId(1L)), accessTimestamp = LocalDateTime(2026, 1, 1, 12, 0)))
        // theme setting
        val sourceDs = dataStore(backgroundScope)
        sourceDs.edit { it[stringPreferencesKey("app-theme")] = "dark" }

        val backup = BackupManager(source, sourceDs).exportBackup(source = "test")

        // sanity: the backup actually captured the user data
        backup.favorites.shouldNotBeNull().map { it.number } shouldContainExactly listOf(1)
        backup.songs.shouldNotBeNull().single().lyric shouldBe "my edited lyric"
        backup.tags.shouldNotBeNull().single().name shouldBe "My Tag"
        backup.bookPreferences.shouldNotBeNull().single().preference shouldBe 5
        backup.history.shouldNotBeNull().single().let {
          it.songNumber shouldBe SongNumber(bookId, 1)
          it.accessTimestamp shouldBe LocalDateTime(2026, 1, 1, 12, 0)
        }
        backup.settings?.get("app-theme") shouldBe "dark"

        // --- destination DB with only base content ---
        val dest = inMemoryDb()
        seedBaseContent(dest)
        val destDs = dataStore(backgroundScope)
        BackupManager(dest, destDs).restoreBackup(backup)

        // re-export from destination and compare the user-data payload
        val restored = BackupManager(dest, destDs).exportBackup(source = "test")

        restored.favorites shouldContainExactlyInAnyOrder backup.favorites!!
        restored.songs.shouldNotBeNull().single().lyric shouldBe "my edited lyric"
        restored.bookPreferences shouldContainExactlyInAnyOrder backup.bookPreferences!!
        restored.tags.shouldNotBeNull().single().let {
          it.name shouldBe "My Tag"
          it.color shouldBe Color.parse("#123456")
          it.songs shouldBe backup.tags!!.single().songs
        }
        restored.history.shouldNotBeNull().single().let {
          it.songNumber shouldBe SongNumber(bookId, 1)
          it.accessTimestamp shouldBe LocalDateTime(2026, 1, 1, 12, 0)
        }
        restored.settings?.get("app-theme") shouldBe "dark"

        source.close(); dest.close()
      }
    }
  }

  feature("restore robustness") {
    scenario("ignores favourites and edits for songs not present in the catalogue") {
      runTest {
        val dest = inMemoryDb()
        seedBaseContent(dest)
        val ds = dataStore(backgroundScope)

        // backup references song number 999 which does not exist in dest
        val source = inMemoryDb()
        seedBaseContent(source)
        source.favoriteDao().addToFavorites(SongNumberId(bookId, SongId(1L)))
        val backup = BackupManager(source, dataStore(backgroundScope)).exportBackup(source = "test")
          .let { it.copy(favorites = it.favorites!! + io.github.alelk.pws.portable.model.SongNumber(bookId, 999)) }

        BackupManager(dest, ds).restoreBackup(backup)

        // only the existing favourite (number 1) is restored; the missing one is skipped
        dest.favoriteDao().count() shouldBe 1
        source.close(); dest.close()
      }
    }

    scenario("ignores an invalid theme identifier") {
      runTest {
        val dest = inMemoryDb()
        seedBaseContent(dest)
        val ds = dataStore(backgroundScope)

        val backup = io.github.alelk.pws.portable.model.Backup(settings = mapOf("app-theme" to "not-a-theme"))
        BackupManager(dest, ds).restoreBackup(backup)

        val stored = exportThemeOf(dest, ds)
        stored shouldBe null
        dest.close()
      }
    }
  }
})

private suspend fun exportThemeOf(db: PwsDatabase, ds: DataStore<Preferences>): String? =
  BackupManager(db, ds).exportBackup(source = "t").settings?.get("app-theme")
