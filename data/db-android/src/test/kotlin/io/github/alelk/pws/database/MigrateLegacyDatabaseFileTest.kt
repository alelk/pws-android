package io.github.alelk.pws.database

import android.content.Context
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import br.com.colman.kotest.FeatureSpec
import br.com.colman.kotest.android.extensions.robolectric.RobolectricTest
import io.github.alelk.pws.database.helper.unzip
import io.github.alelk.pws.database.support.BookMigrationData
import io.github.alelk.pws.database.support.PwsDb2xDataProvider
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.io.File

/**
 * End-to-end legacy migration through the real file lookup ([migrateDataFromPrevDatabase]), as it
 * runs on an in-place update: the legacy file sits next to the new `pws.db` under its original name.
 *
 * Fixtures:
 * - `v11-rustore-2.3.1` — the database shipped in the published fork APK 2.3.1, trimmed to three books
 *   (PV3300, PV800, YunostIisusu) with user data in all of them; built by
 *   `tools/make-rustore-fork-db-fixture.py`.
 * - `v11-with-user-data` (Room v11, the fork's schema) placed under the fork's file name.
 */
@RobolectricTest(sdk = [34])
class MigrateLegacyDatabaseFileTest : FeatureSpec({

  beforeContainer { setupTimberForTest() }

  // Resolved lazily: the Robolectric environment exists only inside features, not at spec construction.
  val context by lazy { ApplicationProvider.getApplicationContext<Context>() }
  val v11Fixture = File("src/test/resources/test-db/v11-with-user-data/pws.2.0.0.dbz") to "pws.2.0.0.db"
  val forkFixture = File("src/test/resources/test-db/v11-rustore-2.3.1/pws.2.3.0.dbz") to "pws.2.3.0.db"
  val forkDbName = "pws.2.3.0.db"

  fun legacyFile() = context.getDatabasePath(forkDbName)
  fun quarantinedFile() = File(legacyFile().parentFile, "$LEGACY_QUARANTINE_DIR/$forkDbName")

  /** Places the fixture where the fork kept its database and clears any state of previous runs. */
  fun placeForkDatabase(fixture: Pair<File, String> = v11Fixture) {
    context.getSharedPreferences("pws_legacy_migration", Context.MODE_PRIVATE).edit().clear().commit()
    quarantinedFile().delete()
    val tmp = File("test-db/legacy-file-test").apply { deleteRecursively(); mkdirs() }
    fixture.first.unzip(tmp)
    val target = legacyFile().apply { parentFile?.mkdirs() }
    File(tmp, fixture.second).copyTo(target, overwrite = true)
    tmp.deleteRecursively()
  }

  suspend fun sourceBooks(): List<BookMigrationData> =
    openReadOnlyDatabase(legacyFile(), ByteArray(0))!!.use { PwsDb2xDataProvider(it).getBooks().getOrThrow() }

  suspend fun sourceFavoriteCount(): Int =
    openReadOnlyDatabase(legacyFile(), ByteArray(0))!!.use { PwsDb2xDataProvider(it).getFavorites().getOrThrow().distinct().size }

  /**
   * Installs [books] into [db] the way a songbook install does (simulates seeding / later installs).
   * Like BookImporterImpl, songs that already exist are left in place: re-inserting them (REPLACE)
   * would cascade-delete their song numbers, favorites and history.
   */
  suspend fun installBooks(db: PwsDatabase, books: List<BookMigrationData>) = db.withTransaction {
    db.songDao().insert(books.flatMap { it.songs }.distinctBy { it.id }.filter { db.songDao().getById(it.id) == null })
    books.forEach { data ->
      db.bookDao().update(data.book)
      db.bookStatisticDao().upsert(data.bookStatistic)
      data.songNumbers.forEach { sn -> runCatching { db.songNumberDao().insert(sn) } }
      db.installedBookDao().upsert(data.installedBook)
    }
  }

  feature("T-A01: the RuStore fork database pws.2.3.0.db is migrated on update") {
    placeForkDatabase()
    val expectedFavorites = sourceFavoriteCount()
    val db = pwsDbForTest(inMemory = true, "pws-db")

    val outcomes = migrateDataFromPrevDatabase(context, db, ByteArray(0))

    scenario("the fork database is found and fully migrated") {
      outcomes shouldHaveSize 1
      outcomes.single().sourceName shouldBe forkDbName
      outcomes.single().result shouldBe LegacyMigrationOutcome.Result.COMPLETE
      outcomes.single().report!!.isComplete shouldBe true
    }

    scenario("books, favorites and history land in the new database") {
      db.bookDao().count() shouldBe 9
      expectedFavorites shouldBeGreaterThan 0
      db.favoriteDao().count() shouldBe expectedFavorites
      db.historyDao().count() shouldBe 10
    }

    scenario("the source file is quarantined, not deleted") {
      legacyFile().exists() shouldBe false
      quarantinedFile().exists() shouldBe true
    }

    scenario("a second run finds nothing to migrate") {
      migrateDataFromPrevDatabase(context, db, ByteArray(0)) shouldHaveSize 0
    }
  }

  feature("T-A02/T-A03: a book installed before the migration does not lose data of other books") {
    placeForkDatabase()
    val books = sourceBooks()
    val favoriteBookIds = openReadOnlyDatabase(legacyFile(), ByteArray(0))!!
      .use { PwsDb2xDataProvider(it).getFavorites().getOrThrow() }
      .map { it.bookId }.toSet()
    // "Book A": a book the user has no favorites in — stands for the seeded PV3300.
    val bookA = books.first { it.book.id !in favoriteBookIds }
    val expectedFavorites = sourceFavoriteCount()
    val db = pwsDbForTest(inMemory = true, "pws-db")
    installBooks(db, listOf(bookA))

    val first = migrateDataFromPrevDatabase(context, db, ByteArray(0)).single()

    scenario("unmatched records: partial result, the source file stays for a retry") {
      first.result shouldBe LegacyMigrationOutcome.Result.PARTIAL_RETRY
      first.report!!.favorites.migrated shouldBeLessThan first.report!!.favorites.found
      legacyFile().exists() shouldBe true
      quarantinedFile().exists() shouldBe false
    }

    scenario("after the remaining books are installed, a retry migrates everything and quarantines") {
      installBooks(db, books - bookA)
      val retry = migrateDataFromPrevDatabase(context, db, ByteArray(0), countAttempt = false).single()
      retry.result shouldBe LegacyMigrationOutcome.Result.COMPLETE
      db.favoriteDao().count() shouldBe expectedFavorites
      db.historyDao().count() shouldBe 10
      legacyFile().exists() shouldBe false
      quarantinedFile().exists() shouldBe true
    }
  }

  feature("T-A03: records that never match are given up on after $LEGACY_MAX_ATTEMPTS startup attempts") {
    placeForkDatabase()
    val books = sourceBooks()
    val favoriteBookIds = openReadOnlyDatabase(legacyFile(), ByteArray(0))!!
      .use { PwsDb2xDataProvider(it).getFavorites().getOrThrow() }
      .map { it.bookId }.toSet()
    val db = pwsDbForTest(inMemory = true, "pws-db")
    installBooks(db, listOf(books.first { it.book.id !in favoriteBookIds }))

    val results = (1..LEGACY_MAX_ATTEMPTS).map { migrateDataFromPrevDatabase(context, db, ByteArray(0)).single() }

    scenario("attempts before the cap keep the file") {
      results.dropLast(1).forEach { it.result shouldBe LegacyMigrationOutcome.Result.PARTIAL_RETRY }
      results.map { it.attempt } shouldBe (1..LEGACY_MAX_ATTEMPTS).toList()
    }

    scenario("the last attempt quarantines the file anyway") {
      results.last().result shouldBe LegacyMigrationOutcome.Result.PARTIAL_GAVE_UP
      legacyFile().exists() shouldBe false
      quarantinedFile().exists() shouldBe true
    }

    scenario("retries triggered by a book install do not count as attempts") {
      placeForkDatabase()
      repeat(LEGACY_MAX_ATTEMPTS + 1) {
        val outcome = migrateDataFromPrevDatabase(context, db, ByteArray(0), countAttempt = false).single()
        outcome.attempt shouldBe 0
        outcome.result shouldNotBe LegacyMigrationOutcome.Result.PARTIAL_GAVE_UP
      }
      legacyFile().exists() shouldBe true
    }
  }

  feature("T-A04: real fork 2.3.1 database - user data in PV3300 and in other books survives the upgrade") {
    placeForkDatabase(forkFixture)
    val books = sourceBooks()
    val db = pwsDbForTest(inMemory = true, "pws-db")

    // The startup order the shell guarantees: legacy migration first, seeding PV3300 afterwards.
    val outcome = migrateDataFromPrevDatabase(context, db, ByteArray(0)).single()
    installBooks(db, books.filter { it.book.id.identifier == "PV3300" }) // seed re-imports its own book

    scenario("migration is complete and the source is quarantined") {
      outcome.result shouldBe LegacyMigrationOutcome.Result.COMPLETE
      outcome.report!!.isComplete shouldBe true
      quarantinedFile().exists() shouldBe true
    }

    scenario("favorites, history, edited songs and tags of all three books are present") {
      db.bookDao().count() shouldBe 3
      db.favoriteDao().count() shouldBe 4
      db.historyDao().count() shouldBe 5
      outcome.report!!.favorites shouldBe MigrationCount(4, 4)
      outcome.report!!.history shouldBe MigrationCount(5, 5)
      // 2 edited songs; one of them is in two books, and edits are counted per (book, number) entry.
      outcome.report!!.editedSongs shouldBe MigrationCount(3, 3)
      val customTag = db.tagDao().getAllByName("Мои любимые").single()
      customTag.predefined shouldBe false
      outcome.report!!.tags.found shouldBeGreaterThan 3
    }
  }

  feature("C2 regression: PV3300 seeded before the migration - data of other books is kept for a retry, not lost") {
    placeForkDatabase(forkFixture)
    val books = sourceBooks()
    val db = pwsDbForTest(inMemory = true, "pws-db")
    installBooks(db, books.filter { it.book.id.identifier == "PV3300" })

    val outcome = migrateDataFromPrevDatabase(context, db, ByteArray(0)).single()

    scenario("PV3300 favorites are migrated, the rest waits; the source file is not removed") {
      outcome.result shouldBe LegacyMigrationOutcome.Result.PARTIAL_RETRY
      outcome.report!!.favorites shouldBe MigrationCount(found = 4, migrated = 2)
      legacyFile().exists() shouldBe true
    }
  }
})
