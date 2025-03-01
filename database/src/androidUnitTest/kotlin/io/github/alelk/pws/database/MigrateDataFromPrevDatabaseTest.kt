package io.github.alelk.pws.database

import br.com.colman.kotest.FeatureSpec
import br.com.colman.kotest.android.extensions.robolectric.RobolectricTest
import io.kotest.matchers.shouldBe
import java.io.File

@RobolectricTest(sdk = 34)
class MigrateDataFromPrevDatabaseTest : FeatureSpec({

  lateinit var db: PwsDatabase

  beforeContainer {
    db = pwsDbForTest(inMemory = true)
    setupTimberForTest()
  }
  afterContainer { db.clean(); db.close() }

  feature("migrate data from database 1.8.0 (v6)") {
    withSqliteDb(File("src/androidUnitTest/resources/test-db/v6/pws.1.8.0.dbz")) { prevDb ->
      prevDb.migrateDataTo(db).isSuccess shouldBe true
    }
  }

})