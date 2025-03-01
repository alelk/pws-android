package io.github.alelk.pws.database.support

import br.com.colman.kotest.FeatureSpec
import br.com.colman.kotest.android.extensions.robolectric.RobolectricTest
import io.github.alelk.pws.database.TestDbPatch
import io.github.alelk.pws.database.setupTimberForTest
import io.github.alelk.pws.database.withSqliteDb
import io.github.alelk.pws.domain.model.BibleRef
import io.github.alelk.pws.domain.model.BookId
import io.github.alelk.pws.domain.model.Color
import io.github.alelk.pws.domain.model.Prayer
import io.github.alelk.pws.domain.model.Pv3300
import io.github.alelk.pws.domain.model.Pv800
import io.github.alelk.pws.domain.model.SongNumber
import io.github.alelk.pws.domain.model.TagId
import io.github.alelk.pws.domain.model.Tonality
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.datetime.LocalDateTime
import java.io.File

@RobolectricTest(sdk = 34)
class PwsDb1xDataProviderTest : FeatureSpec({

  beforeContainer {
    setupTimberForTest()
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
      db.version shouldBe 6

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
          first() shouldBe HistoryItem(SongNumber(BookId.Pv3300, 2), LocalDateTime.parse("2025-02-25T18:03:02"))
        }
      }

      scenario("get edited songs") {
        val songs = dbProvider.getEditedSongs()
        songs.isSuccess shouldBe true
        songs.getOrThrow().run {
          size shouldBe 17
          distinctBy { it.lyric } shouldHaveSize 5
          single { it.number == SongNumber(BookId.Pv800, 11) }.run {
            lyric shouldContain "This song is edited (text + bible ref)."
            bibleRef shouldBe BibleRef("Edited Bible Ref")
          }
          single { it.number == SongNumber(BookId.Pv3300, 16) }.run {
            lyric shouldContain "This song is edited (empty bible reference + tonality)"
            tonalities shouldBe listOf(Tonality.C_MINOR)
            bibleRef shouldBe null
          }
        }
      }

      scenario("get tags") {
        val tags = dbProvider.getTags()
        tags.isSuccess shouldBe true
        tags.getOrThrow().run {
          size shouldBe 31
          filter { it.predefined } shouldHaveSize 30
          single { !it.predefined }.let { customTag ->
            customTag.name shouldBe "Custom-Category"
            customTag.predefined shouldBe false
            customTag.id shouldBe TagId.parse("custom-00001")
            customTag.color shouldBe Color.parse("#6dc950")
            customTag.songNumbers shouldBe mapOf(BookId.Pv3300 to (20..22).toSet())
          }
          single { it.id == TagId.Prayer }.let { tag ->
            tag.predefined shouldBe true
            tag.songNumbers shouldBe mapOf(BookId.Pv3300 to (22..50).toSet())
          }
        }
      }
    }
  }

  feature("fetch data from database v1.8.0 (v7)") {
    withSqliteDb(
      File("src/androidUnitTest/resources/test-db/v6/pws.1.8.0.dbz"),
      patches = listOf(TestDbPatch.V6toV7, TestDbPatch.InsertCustomTags)
    ) { db ->
      db.version shouldBe 7

      val dbProvider = PwsDb1xDataProvider(db)

      scenario("get favorites") {
        val favorites = dbProvider.getFavorites()
        favorites.isSuccess shouldBe true
        favorites.getOrThrow() shouldBe expectedV180Favorites
      }

      scenario("get history") {
        val history = dbProvider.getHistory()
        history.isSuccess shouldBe true
        history.getOrThrow() shouldHaveSize 14
      }

      scenario("get edited songs") {
        val songs = dbProvider.getEditedSongs()
        songs.isSuccess shouldBe true
        songs.getOrThrow() shouldHaveSize 17
      }

      scenario("get tags") {
        val tags = dbProvider.getTags()
        tags.isSuccess shouldBe true
        tags.getOrThrow().run {
          size shouldBe 34
          filter { it.predefined } shouldHaveSize 30
          single { it.id == TagId("custom-00002") }.songNumbers.values.flatten() shouldHaveSize 3
        }
      }
    }
  }

  feature("fetch data from database v1.8.0 (v8)") {
    withSqliteDb(
      File("src/androidUnitTest/resources/test-db/v6/pws.1.8.0.dbz"),
      patches = listOf(TestDbPatch.InsertCustomTags, TestDbPatch.V6toV7, TestDbPatch.V7toV8)
    ) { db ->
      db.version shouldBe 8

      val dbProvider = PwsDb1xDataProvider(db)

      scenario("get favorites") {
        val favorites = dbProvider.getFavorites()
        favorites.isSuccess shouldBe true
        favorites.getOrThrow() shouldBe expectedV180Favorites
      }

      scenario("get history") {
        val history = dbProvider.getHistory()
        history.isSuccess shouldBe true
        history.getOrThrow() shouldHaveSize 14
      }

      scenario("get edited songs") {
        val songs = dbProvider.getEditedSongs()
        songs.isSuccess shouldBe true
        songs.getOrThrow() shouldHaveSize 17
      }

      scenario("get tags") {
        val tags = dbProvider.getTags()
        tags.isSuccess shouldBe true
        tags.getOrThrow() shouldHaveSize 34
      }
    }
  }

  feature("fetch data from database v1.8.0 (v9)") {
    withSqliteDb(
      File("src/androidUnitTest/resources/test-db/v6/pws.1.8.0.dbz"),
      patches = listOf(TestDbPatch.V6toV7, TestDbPatch.InsertCustomTags, TestDbPatch.V7toV8, TestDbPatch.V8toV9)
    ) { db ->
      db.version shouldBe 9

      val dbProvider = PwsDb1xDataProvider(db)

      scenario("get favorites") {
        val favorites = dbProvider.getFavorites()
        favorites.isSuccess shouldBe true
        favorites.getOrThrow() shouldBe expectedV180Favorites
      }

      scenario("get history") {
        val history = dbProvider.getHistory()
        history.isSuccess shouldBe true
        history.getOrThrow() shouldHaveSize 14
      }

      scenario("get edited songs") {
        val songs = dbProvider.getEditedSongs()
        songs.isSuccess shouldBe true
        songs.getOrThrow() shouldHaveSize 17
      }

      scenario("get tags") {
        val tags = dbProvider.getTags()
        tags.isSuccess shouldBe true
        tags.getOrThrow() shouldHaveSize 34
      }
    }
  }

  feature("fetch data from database v1.8.0 (v10)") {
    withSqliteDb(
      File("src/androidUnitTest/resources/test-db/v6/pws.1.8.0.dbz"),
      patches = listOf(TestDbPatch.V6toV7, TestDbPatch.InsertCustomTags, TestDbPatch.V7toV8, TestDbPatch.V8toV9, TestDbPatch.V9toV10)
    ) { db ->
      db.version shouldBe 10

      val dbProvider = PwsDb1xDataProvider(db)

      scenario("get favorites") {
        val favorites = dbProvider.getFavorites()
        favorites.isSuccess shouldBe true
        favorites.getOrThrow() shouldBe expectedV180Favorites
      }

      scenario("get history") {
        val history = dbProvider.getHistory()
        history.isSuccess shouldBe true
        history.getOrThrow() shouldHaveSize 14
      }

      scenario("get edited songs") {
        val songs = dbProvider.getEditedSongs()
        songs.isSuccess shouldBe true
        songs.getOrThrow() shouldHaveSize 17
      }

      scenario("get tags") {
        val tags = dbProvider.getTags()
        tags.isSuccess shouldBe true
        tags.getOrThrow() shouldHaveSize 34
      }
    }
  }
})