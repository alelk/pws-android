package io.github.alelk.pws.database

import android.app.SearchManager
import android.content.Context
import androidx.room.Room
import androidx.room.execSQL
import androidx.room.useReaderConnection
import androidx.room.useWriterConnection
import androidx.test.core.app.ApplicationProvider
import br.com.colman.kotest.FeatureSpec
import br.com.colman.kotest.android.extensions.robolectric.RobolectricTest
import io.github.alelk.pws.database.book.bookEntity
import io.github.alelk.pws.database.bookstatistic.BookStatisticEntity
import io.github.alelk.pws.database.provider.suggestionsByText
import io.github.alelk.pws.database.search.SongSearchSchema
import io.github.alelk.pws.database.search.buildFtsPrefixQuery
import io.github.alelk.pws.database.song.SongEntity
import io.github.alelk.pws.database.song.songEntity
import io.github.alelk.pws.database.song_number.SongNumberEntity
import io.github.alelk.pws.domain.core.ids.SongId
import io.kotest.core.test.Enabled
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.RandomSource
import io.kotest.property.arbitrary.next
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

/**
 * The v16 search schema (`songs_fts` over `songs_search_v`, `pws_fts_sync_*` triggers, plan 2026-10-23 T-0.1 / T-2.3)
 * on the Android engines, in Room's SupportSQLite ("compatibility") mode — the mode the app runs in:
 *
 * - **framework SQLite** (Robolectric's): always runs;
 * - **SQLCipher** (`net.zetetic:sqlcipher-android`, the production engine): runs only where its native library loads.
 *   The AAR ships Android ABIs only, so on a host JVM the scenario is reported as disabled with the reason — the
 *   SQLCipher half of T-0.1 then still needs a device/emulator run.
 *
 * Not runnable on the aarch64 agent sandbox (aapt2 is x86_64-only).
 */
@RobolectricTest(sdk = [34])
class SearchSchemaAndroidTest : FeatureSpec({

  beforeContainer { setupTimberForTest() }

  fun context(): Context = ApplicationProvider.getApplicationContext()

  val sqlCipherLoaded: Boolean by lazy { runCatching { System.loadLibrary("sqlcipher") }.isSuccess }

  fun frameworkDb(name: String) = Room.databaseBuilder(context(), PwsDatabase::class.java, name).withPwsSchema().build()

  fun sqlCipherDb(name: String) = Room.databaseBuilder(context(), PwsDatabase::class.java, name)
    .openHelperFactory(SupportOpenHelperFactory("test-passphrase".toByteArray()))
    .withPwsSchema()
    .build()

  suspend fun PwsDatabase.strings(sql: String): List<String> = useReaderConnection { c ->
    c.usePrepared(sql) { st -> buildList { while (st.step()) add(st.getText(0)) } }
  }

  suspend fun PwsDatabase.long(sql: String): Long = useReaderConnection { c ->
    c.usePrepared(sql) { st ->
      st.step()
      st.getLong(0)
    }
  }

  suspend fun PwsDatabase.indexed(text: String): List<Long> = useReaderConnection { c ->
    c.usePrepared("SELECT docid FROM songs_fts WHERE songs_fts MATCH ? ORDER BY docid") { st ->
      st.bindText(1, buildFtsPrefixQuery(text))
      buildList { while (st.step()) add(st.getLong(0)) }
    }
  }

  suspend fun PwsDatabase.integrityCheck() = useWriterConnection { it.execSQL(SongSearchSchema.INTEGRITY_CHECK) }

  /** The same checks on any engine. */
  suspend fun checkSearchSchema(engine: String, open: (String) -> PwsDatabase) {
    val name = "search-schema-$engine.db"
    context().deleteDatabase(name)
    val db = open(name)
    try {
      println("$engine: sqlite ${db.strings("SELECT sqlite_version()")}, compile options ${db.strings("PRAGMA compile_options")}")

      // fresh database: view, FTS4 over the view, our triggers only
      db.strings("SELECT sql FROM sqlite_master WHERE name = 'songs_fts'") shouldBe listOf(SongSearchSchema.createFts)
      db.strings("SELECT sql FROM sqlite_master WHERE name = 'songs_search_v'") shouldBe listOf(SongSearchSchema.createView)
      db.strings("SELECT name FROM sqlite_master WHERE type = 'trigger' AND tbl_name = 'songs'") shouldContainExactlyInAnyOrder
        SongSearchSchema.triggers.keys
      // R9: REPLACE fires the BEFORE DELETE trigger only with recursive_triggers on; Room sets it on the write connection
      val recursiveTriggers = db.useWriterConnection { c ->
        c.usePrepared("PRAGMA recursive_triggers") {
          it.step()
          it.getLong(0)
        }
      }
      recursiveTriggers shouldBe 1L

      val rs = RandomSource.seeded(3)
      val book = Arb.bookEntity().next(rs)
      fun song(id: Long, name: String, lyric: String): SongEntity =
        Arb.songEntity().next(rs).copy(id = SongId(id), name = name, lyric = lyric, edited = false)
      db.bookDao().insert(listOf(book))
      db.bookStatisticDao().insert(listOf(BookStatisticEntity(id = book.id, priority = 10)))
      db.songDao().insert(listOf(song(1, "Ґанок", "Єдиний їжак"), song(2, "П'ять хлібів", "пʼять хлібів")))
      db.songNumberDao().insert(
        listOf(
          SongNumberEntity(bookId = book.id, songId = SongId(1), number = 1, priority = 0),
          SongNumberEntity(bookId = book.id, songId = SongId(2), number = 2, priority = 0),
        ),
      )

      // folded search through the repository-side DAO and through the SearchManager suggestions
      db.songDao().findBySongText(buildFtsPrefixQuery("ганок едини"), 10).map { it.songId } shouldBe listOf(SongId(1))
      db.songDao().findBySongText(buildFtsPrefixQuery("пять"), 10).map { it.songId } shouldBe listOf(SongId(2))
      db.songDao().suggestionsByText("ганок", limit = 10).use { cursor ->
        cursor.count shouldBe 1
        cursor.moveToFirst()
        cursor.getString(cursor.getColumnIndexOrThrow(SearchManager.SUGGEST_COLUMN_TEXT_1)) shouldBe "Ґанок"
      }

      // every write path keeps the index consistent
      db.songDao().update(song(2, "П'ять хлібів", "дві рибини"))
      db.songDao().insert(song(1, "Ґанок", "замінений текст")) // REPLACE of an existing song
      db.songDao().insert(song(3, "Їжачок", "нова"))
      db.songDao().deleteById(SongId(2))
      db.indexed("едини").shouldBeEmpty()
      db.indexed("замінени") shouldBe listOf(1L)
      db.indexed("ижачок") shouldBe listOf(3L)
      db.indexed("рибини").shouldBeEmpty()
      db.long("SELECT count(*) FROM songs_fts_docsize") shouldBe db.long("SELECT count(*) FROM songs")
      db.integrityCheck()
    } finally {
      db.close()
    }

    // re-open: the open callback finds everything in place, nothing is rebuilt or lost
    val again = open(name)
    try {
      again.strings("SELECT name FROM sqlite_master WHERE type = 'trigger' AND tbl_name = 'songs'") shouldContainExactlyInAnyOrder
        SongSearchSchema.triggers.keys
      again.indexed("ижачок") shouldBe listOf(3L)
      again.integrityCheck()
    } finally {
      again.close()
      context().deleteDatabase(name)
    }
  }

  feature("search schema in SupportSQLite mode") {

    scenario("framework SQLite") {
      checkSearchSchema("framework", ::frameworkDb)
    }

    scenario("SQLCipher").config(
      enabledOrReasonIf = {
        if (sqlCipherLoaded) {
          Enabled.enabled
        } else {
          Enabled.disabled("SQLCipher native library is not loadable on this JVM — run on a device/emulator (plan T-0.1)")
        }
      },
    ) {
      checkSearchSchema("sqlcipher", ::sqlCipherDb)
    }
  }
})
