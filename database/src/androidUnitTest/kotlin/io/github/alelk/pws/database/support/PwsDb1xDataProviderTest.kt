package io.github.alelk.pws.database.support

import br.com.colman.kotest.FeatureSpec
import br.com.colman.kotest.android.extensions.robolectric.RobolectricTest
import io.github.alelk.pws.database.withSqliteDb
import io.github.alelk.pws.domain.model.BookId
import io.github.alelk.pws.domain.model.SongNumber
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDateTime
import timber.log.Timber
import java.io.File

@RobolectricTest(sdk = 34)
class PwsDb1xDataProviderTest : FeatureSpec({

  beforeContainer {
    Timber.plant(object : Timber.Tree() {
      override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        println("$tag: $message")
      }
    })
  }

  val expectedV180Favorites = listOf(
    SongNumber(BookId.parse("PV3300"), 1),
    SongNumber(BookId.parse("PV3300"), 3),
    SongNumber(BookId.parse("PV3300"), 5),
    SongNumber(BookId.parse("PV3300"), 7),
    SongNumber(BookId.parse("PV3300"), 9),
  )

  feature("fetch data from database v1.8.0 (v6)") {
    withSqliteDb(File("src/androidUnitTest/resources/test-db/v6/pws.1.8.0.dbz")) { db ->
      val dbProvider = PwsDb1xDataProvider(db)

      scenario("get favorites") {
        val favorites = dbProvider.getFavorites()
        favorites.isSuccess shouldBe true
        favorites.getOrThrow() shouldBe expectedV180Favorites
      }

      scenario("get history") {
        val history = dbProvider.getHistory()
        history.isSuccess shouldBe true
        history.getOrThrow().run {
          size shouldBe 14
          first() shouldBe HistoryItem(SongNumber(BookId.parse("PV3300"), 2), LocalDateTime.parse("2025-02-25T18:03:02"))
        }
      }

    }
  }
})