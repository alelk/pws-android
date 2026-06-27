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

  feature("migrate data from database 1.8.0 (v6-with-user-data)") {
    withSqliteDb(File("src/test/resources/test-db/v6-with-user-data/pws.1.8.0.dbz")) { prevDb ->
      prevDb.migrateDataTo(db).isSuccess shouldBe true
    }
  }

  feature("migrate data from database 2.0.0 (v11-with-user-data)") {
    withSqliteDb(File("src/test/resources/test-db/v11-with-user-data/pws.2.0.0.dbz")) { prevDb ->
      prevDb.migrateDataTo(db).isSuccess shouldBe true
    }
  }

  feature("migrate data from database 3.0.0 (v12-with-user-data)") {
    withSqliteDb(File("src/test/resources/test-db/v12-with-user-data/pws.3.0.0.dbz")) { prevDb ->
      prevDb.migrateDataTo(db).isSuccess shouldBe true
    }
  }

  feature("migrate data from database 3.2.3 (v13-with-user-data)") {
    withSqliteDb(File("src/test/resources/test-db/v13-with-user-data/pws-ru-test-3.2.3.dbz")) { prevDb ->
      prevDb.migrateDataTo(db).isSuccess shouldBe true
    }
  }

  // The migrations above run into an *empty* Room DB, so they only prove the migration doesn't
  // crash. This one migrates into the *seeded* current-version DB (the realistic upgrade target),
  // verifying that user data actually lands — guarding against silent data loss on app upgrade.
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

})