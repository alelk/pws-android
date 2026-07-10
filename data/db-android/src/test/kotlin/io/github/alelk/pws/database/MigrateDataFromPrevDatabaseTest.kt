package io.github.alelk.pws.database

import br.com.colman.kotest.FeatureSpec
import br.com.colman.kotest.android.extensions.robolectric.RobolectricTest
import io.github.alelk.pws.database.support.PwsDb2xDataProvider
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import java.io.File

@RobolectricTest(sdk = 34)
class MigrateDataFromPrevDatabaseTest : FeatureSpec({

  lateinit var db: PwsDatabase

  beforeContainer {
    db = pwsDbForTest(inMemory = true, "pws-db")
    setupTimberForTest()
  }
  afterContainer { db.clean(); db.close() }

  // Tests 1–4: migration into an *empty* Room DB — verifies that books are installed
  // AND that user data is linked correctly, even before onboarding has ever run.

  feature("migrate data from database 1.8.0 (v6-with-user-data)") {
    withSqliteDb(File("src/test/resources/test-db/v6-with-user-data/pws.1.8.0.dbz")) { prevDb ->
      val result = prevDb.migrateDataTo(db)

      scenario("migration succeeds") {
        result.isSuccess shouldBe true
      }

      scenario("books are installed into empty target DB") {
        db.bookDao().count() shouldBe 9
      }

      scenario("user favorites are migrated") {
        db.favoriteDao().count() shouldBe 5
      }

      scenario("user history is migrated") {
        db.historyDao().count() shouldBe 14
      }
    }
  }

  feature("migrate data from database 2.0.0 (v11-with-user-data)") {
    withSqliteDb(File("src/test/resources/test-db/v11-with-user-data/pws.2.0.0.dbz")) { prevDb ->
      val result = prevDb.migrateDataTo(db)

      scenario("migration succeeds") {
        result.isSuccess shouldBe true
      }

      scenario("books are installed into empty target DB") {
        db.bookDao().count() shouldBe 9
      }

      scenario("user favorites are migrated") {
        db.favoriteDao().count() shouldBe 4
      }

      scenario("user history is migrated") {
        db.historyDao().count() shouldBe 10
      }
    }
  }

  feature("migrate data from database 3.0.0 (v12-with-user-data)") {
    withSqliteDb(File("src/test/resources/test-db/v12-with-user-data/pws.3.0.0.dbz")) { prevDb ->
      val result = prevDb.migrateDataTo(db)

      scenario("migration succeeds") {
        result.isSuccess shouldBe true
      }

      scenario("books are installed into empty target DB") {
        db.bookDao().count() shouldBe 9
      }

      scenario("user favorites are migrated") {
        db.favoriteDao().count() shouldBe 4
      }

      scenario("user history is migrated") {
        db.historyDao().count() shouldBe 9
      }
    }
  }

  feature("migrate data from database 3.2.3 (v13-with-user-data)") {
    withSqliteDb(File("src/test/resources/test-db/v13-with-user-data/pws-ru-test-3.2.3.dbz")) { prevDb ->
      val result = prevDb.migrateDataTo(db)

      scenario("migration succeeds") {
        result.isSuccess shouldBe true
      }

      scenario("books are installed into empty target DB") {
        db.bookDao().count() shouldBe 11
      }

      scenario("user favorites are migrated") {
        db.favoriteDao().count() shouldBe 4
      }

      scenario("user history is migrated") {
        db.historyDao().count() shouldBe 8
      }
    }
  }

  // Test 5: migration into the *seeded* current-version DB (the realistic upgrade target).
  // Books are already present → Phase 1 is skipped → user data still lands via Phase 2.
  feature("migrate v13 user data into the seeded current database") {
    val currentAsset = File("src/test/resources/test-db/v14/pws-ru-test-3.3.3.dbz")
    val oldDb = File("src/test/resources/test-db/v13-with-user-data/pws-ru-test-3.2.3.dbz")

    scenario("favourites from the old database are present after migrating into the seed") {
      withPwsDb(currentAsset, readOnly = false) { target ->
        target.favoriteDao().count() shouldBe 0  // seed ships no user data
        withSqliteDb(oldDb) { prevDb ->
          // sanity: the old database actually contains favourites to migrate
          val sourceFavorites = PwsDb2xDataProvider(prevDb).getFavorites().getOrThrow()
          sourceFavorites.size shouldBeGreaterThan 0

          prevDb.migrateDataTo(target).isSuccess shouldBe true
          target.favoriteDao().count() shouldBeGreaterThan 0
        }
      }
    }
  }

  // Test 6: idempotency — running migration twice should not duplicate books or user data.
  feature("migration is idempotent (v13)") {
    withSqliteDb(File("src/test/resources/test-db/v13-with-user-data/pws-ru-test-3.2.3.dbz")) { prevDb ->
      prevDb.migrateDataTo(db)
      val bookCountAfterFirst = db.bookDao().count()
      val favoriteCountAfterFirst = db.favoriteDao().count()
      val historyCountAfterFirst = db.historyDao().count()
      prevDb.migrateDataTo(db)

      scenario("first migration installs 11 books and 4 favorites") {
        bookCountAfterFirst shouldBe 11
        favoriteCountAfterFirst shouldBe 4
        historyCountAfterFirst shouldBe 8
      }

      scenario("book count is unchanged after second migration") {
        db.bookDao().count() shouldBe bookCountAfterFirst
      }

      scenario("favorite count is unchanged after second migration") {
        db.favoriteDao().count() shouldBe favoriteCountAfterFirst
      }

      scenario("history count is unchanged after second migration") {
        db.historyDao().count() shouldBe historyCountAfterFirst
      }
    }
  }

})
