package io.github.alelk.pws.database

import androidx.room.execSQL
import androidx.room.useReaderConnection
import androidx.room.useWriterConnection
import br.com.colman.kotest.FeatureSpec
import br.com.colman.kotest.android.extensions.robolectric.RobolectricTest
import io.github.alelk.pws.database.search.SongSearchSchema
import io.github.alelk.pws.domain.core.ids.BookId
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.io.File

/**
 * Tests that the current (v14) database shipped as an asset opens correctly.
 *
 * The most important assertion is that Room can open the asset DB at all: [withPwsDb] hands the
 * unzipped file to Room, which validates the schema identity hash against the generated
 * `14.json` schema and throws if they diverge. So this test fails loudly whenever the asset DB
 * goes out of sync with the Room entities/schema.
 */
@RobolectricTest(sdk = [34, 37])
class CurrentDatabaseTest : FeatureSpec({

  beforeContainer {
    setupTimberForTest()
  }

  val currentDbZip = File("src/test/resources/test-db/v14/pws-ru-test-3.3.3.dbz")

  feature("current database v3.3.3 (v14)") {

    scenario("opens as raw SQLite with the expected schema version") {
      withSqliteDb(currentDbZip) { db ->
        db.version shouldBe 14
      }
    }

    scenario("opens via Room and exposes the bundled seed data") {
      withPwsDb(currentDbZip, readOnly = true) { db ->
        // Reaching here means Room accepted the asset DB (schema identity hash matches).
        db.bookDao().count() shouldBe 27
        db.songDao().count() shouldBe 157
        db.songNumberDao().count() shouldBe 220
        db.songReferenceDao().count() shouldBe 46
        db.tagDao().count() shouldBe 34
        db.songTagDao().count() shouldBe 68

        // freshly shipped DB carries no user data
        db.favoriteDao().count() shouldBe 0
        db.historyDao().count() shouldBe 0

        // every predefined tag is present
        db.tagDao().getAllNotPredefined() shouldBe emptyList()

        // a known book from the seed catalogue is readable
        db.bookDao().getById(BookId.parse("Gusli")).shouldNotBeNull()
      }
    }

    // Room opens the v14 asset through MIGRATION_14_15 + MIGRATION_15_16 (KMP form, SupportSQLite mode here).
    scenario("migrates to v16: songs_fts is rebuilt over the folded view, Room's FTS triggers are gone") {
      withPwsDb(currentDbZip, readOnly = true) { db ->
        db.useReaderConnection { c ->
          suspend fun strings(sql: String) = c.usePrepared(sql) { st -> buildList { while (st.step()) add(st.getText(0)) } }
          suspend fun long(sql: String) = c.usePrepared(sql) { st ->
            st.step()
            st.getLong(0)
          }
          long("PRAGMA user_version") shouldBe 16
          strings("SELECT sql FROM sqlite_master WHERE name = 'songs_fts'") shouldBe listOf(SongSearchSchema.createFts)
          strings("SELECT name FROM sqlite_master WHERE type = 'trigger' AND tbl_name = 'songs'") shouldContainExactlyInAnyOrder
            SongSearchSchema.triggers.keys
          long("SELECT count(*) FROM songs_fts_docsize") shouldBe 157
          long("SELECT count(*) FROM songs_fts WHERE songs_fts MATCH '\"господ*\"'") shouldBeGreaterThan 0
        }
        db.useWriterConnection { it.execSQL(SongSearchSchema.INTEGRITY_CHECK) }
      }
    }
  }
})
