package io.github.alelk.pws.database

import android.database.sqlite.SQLiteException
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import br.com.colman.kotest.FeatureSpec
import br.com.colman.kotest.android.extensions.robolectric.RobolectricTest
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import java.lang.reflect.Proxy

/** A [SupportSQLiteDatabase] that answers nothing — the tests only ever compare identities. */
private fun fakeDatabase(label: String): SupportSQLiteDatabase =
  Proxy.newProxyInstance(
    SupportSQLiteDatabase::class.java.classLoader,
    arrayOf(SupportSQLiteDatabase::class.java)
  ) { proxy, method, args ->
    when (method.name) {
      "toString" -> label
      "hashCode" -> System.identityHashCode(proxy)
      "equals" -> proxy === args?.firstOrNull()
      else -> null
    }
  } as SupportSQLiteDatabase

private class FakeOpenHelper(
  private val database: SupportSQLiteDatabase,
  private val failure: Throwable? = null
) : SupportSQLiteOpenHelper {
  var writeAheadLoggingEnabled: Boolean? = null
  var closed = false

  override val databaseName: String = "pws.db"
  override val writableDatabase: SupportSQLiteDatabase get() = failure?.let { throw it } ?: database
  override val readableDatabase: SupportSQLiteDatabase get() = writableDatabase
  override fun setWriteAheadLoggingEnabled(enabled: Boolean) {
    writeAheadLoggingEnabled = enabled
  }

  override fun close() {
    closed = true
  }
}

@RobolectricTest(sdk = 34)
class SelfHealingOpenHelperTest : FeatureSpec({

  beforeContainer { setupTimberForTest() }

  feature("opening a healthy database") {

    scenario("returns the delegate database and never touches the file") {
      val database = fakeDatabase("healthy")
      var discardCalls = 0
      val helper = SelfHealingOpenHelper(
        newDelegate = { FakeOpenHelper(database) },
        discardDatabase = { discardCalls++; true }
      )

      helper.writableDatabase shouldBeSameInstanceAs database
      discardCalls shouldBe 0
    }
  }

  feature("opening a half-created database") {

    scenario("discards the file, rebuilds the delegate and opens again") {
      val database = fakeDatabase("recreated")
      val delegates = mutableListOf<FakeOpenHelper>()
      val helper = SelfHealingOpenHelper(
        newDelegate = {
          FakeOpenHelper(
            database = database,
            failure = if (delegates.isEmpty()) SQLiteException("vtable constructor failed: songs_fts") else null
          ).also { delegates += it }
        },
        discardDatabase = { true }
      )
      helper.setWriteAheadLoggingEnabled(true)

      helper.writableDatabase shouldBeSameInstanceAs database
      delegates.size shouldBe 2
      // the broken delegate is released before the file is deleted, the fresh one keeps WAL on
      delegates[0].closed shouldBe true
      delegates[1].writeAheadLoggingEnabled shouldBe true
    }

    scenario("retries after Room rejects the schema it found at version 0") {
      val database = fakeDatabase("recreated")
      var attempts = 0
      val helper = SelfHealingOpenHelper(
        newDelegate = {
          attempts++
          FakeOpenHelper(
            database = database,
            failure = if (attempts == 1) IllegalStateException("Pre-packaged database has an invalid schema") else null
          )
        },
        discardDatabase = { true }
      )

      helper.writableDatabase shouldBeSameInstanceAs database
      attempts shouldBe 2
    }

    scenario("heals at most once — a failure that survives the fresh start is rethrown") {
      var delegates = 0
      val helper = SelfHealingOpenHelper(
        newDelegate = { delegates++; FakeOpenHelper(fakeDatabase("never"), SQLiteException("still broken")) },
        discardDatabase = { true }
      )

      shouldThrow<SQLiteException> { helper.writableDatabase }.message shouldBe "still broken"
      shouldThrow<SQLiteException> { helper.writableDatabase }.message shouldBe "still broken"
      delegates shouldBe 2
    }
  }

  feature("opening a database that holds data") {

    scenario("rethrows without recreating the delegate when the file is kept") {
      var delegates = 0
      val helper = SelfHealingOpenHelper(
        newDelegate = { delegates++; FakeOpenHelper(fakeDatabase("never"), SQLiteException("file is not a database")) },
        discardDatabase = { false }
      )

      shouldThrow<SQLiteException> { helper.writableDatabase }.message shouldBe "file is not a database"
      delegates shouldBe 1
    }
  }
})
