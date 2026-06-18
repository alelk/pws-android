package io.github.alelk.pws.database.support

import br.com.colman.kotest.FeatureSpec
import br.com.colman.kotest.android.extensions.robolectric.RobolectricTest
import io.github.alelk.pws.database.setupTimberForTest
import io.github.alelk.pws.database.withSqliteDb
import io.github.alelk.pws.domain.core.ids.AboutChurch
import io.github.alelk.pws.domain.core.BibleRef
import io.github.alelk.pws.domain.core.ids.BookId
import io.github.alelk.pws.domain.core.Color
import io.github.alelk.pws.domain.core.ids.PesnHvaly
import io.github.alelk.pws.domain.core.ids.Pv2001
import io.github.alelk.pws.domain.core.ids.Pv2555
import io.github.alelk.pws.domain.core.ids.Pv3300
import io.github.alelk.pws.domain.core.ids.Pv800
import io.github.alelk.pws.domain.core.SongNumber
import io.github.alelk.pws.domain.core.ids.TagId
import io.github.alelk.pws.domain.tonality.Tonality
import io.github.alelk.pws.domain.core.ids.YunostIisusu
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.datetime.LocalDateTime
import java.io.File

@RobolectricTest(sdk = 34)
class PwsDb2xDataProviderTest : FeatureSpec({

  beforeContainer {
    setupTimberForTest()
  }

  val expectedV200Favorites = listOf(
    SongNumber(BookId.Pv800, 1),
    SongNumber(BookId.YunostIisusu, 1),
    SongNumber(BookId.YunostIisusu, 2),
    SongNumber(BookId.PesnHvaly, 1),
  )

  val psalmsOfZion = BookId.parse("psalms-of-zion-3")
  val weWillSingAndPraise = BookId.parse("we-will-sing-and-praise")

  feature("fetch data from database v3.2.3 (v13-with-user-data)") {
    withSqliteDb(File("src/test/resources/test-db/v13-with-user-data/pws-ru-test-3.2.3.dbz")) { db ->
      db.version shouldBe 13

      val dbProvider = PwsDb2xDataProvider(db)

      scenario("get favorites") {
        val favorites = dbProvider.getFavorites()
        favorites.isSuccess shouldBe true
        favorites.getOrThrow().run {
          size shouldBe 4
          this shouldBe listOf(
            SongNumber(BookId.Pv2555, 10),
            SongNumber(BookId.Pv2555, 12),
            SongNumber(BookId.Pv2555, 14),
            SongNumber(weWillSingAndPraise, 5),
          )
        }
      }

      scenario("get history") {
        val history = dbProvider.getHistory()
        history.isSuccess shouldBe true
        history.getOrThrow().run {
          size shouldBe 8
          first() shouldBe HistoryItem(SongNumber(psalmsOfZion, 10), LocalDateTime.parse("2026-06-18T15:24:55.545656"))
          last() shouldBe HistoryItem(SongNumber(weWillSingAndPraise, 5), LocalDateTime.parse("2026-06-18T15:34:44.403645"))
        }
      }

      scenario("get edited songs") {
        val songs = dbProvider.getEditedSongs()
        songs.isSuccess shouldBe true
        songs.getOrThrow().run {
          size shouldBe 5
          distinctBy { it.lyric } shouldHaveSize 1
          single { it.number == SongNumber(BookId.Pv2001, 19) }.run {
            lyric shouldContain "Edited song Text!!!"
            tonalities shouldBe listOf(Tonality.F_MAJOR, Tonality.D_MAJOR)
            bibleRef shouldBe BibleRef("bible text")
          }
        }
      }

      scenario("get tags") {
        val tags = dbProvider.getTags()
        tags.isSuccess shouldBe true
        tags.getOrThrow().run {
          size shouldBe 36
          filter { it.predefined } shouldHaveSize 34
          val customTags = filter { !it.predefined }
          customTags shouldHaveSize 2
          customTags.single { it.name == "Test Tag" }.let { tag ->
            tag.id shouldBe TagId.parse("custom-15687")
            tag.color shouldBe Color.parse("#7986cb")
            tag.songNumbers shouldHaveSize 5
          }
          customTags.single { it.name == "Test Tag 2" }.let { tag ->
            tag.id shouldBe TagId.parse("custom-25630")
            tag.color shouldBe Color.parse("#ba68c8")
            tag.songNumbers shouldHaveSize 1
          }
        }
      }
    }
  }

  feature("fetch data from database v3.0.0 (v12-with-user-data)") {
    withSqliteDb(File("src/test/resources/test-db/v12-with-user-data/pws.3.0.0.dbz")) { db ->
      db.version shouldBe 12

      val dbProvider = PwsDb2xDataProvider(db)

      scenario("get favorites") {
        val favorites = dbProvider.getFavorites()
        favorites.isSuccess shouldBe true
        favorites.getOrThrow() shouldBe listOf(
          SongNumber(BookId.Pv3300, 10),
          SongNumber(BookId.Pv3300, 12),
          SongNumber(BookId.parse("DerjisHrista"), 9),
          SongNumber(BookId.Pv2001, 5),
        )
      }

      scenario("get history") {
        val history = dbProvider.getHistory()
        history.isSuccess shouldBe true
        history.getOrThrow().run {
          size shouldBe 9
          first() shouldBe HistoryItem(SongNumber(BookId.Pv3300, 10), LocalDateTime.parse("2026-06-18T16:54:41.153074"))
          last() shouldBe HistoryItem(SongNumber(BookId.Pv2001, 5), LocalDateTime.parse("2026-06-18T17:03:23.279121"))
        }
      }

      scenario("get edited songs") {
        val songs = dbProvider.getEditedSongs()
        songs.isSuccess shouldBe true
        songs.getOrThrow().run {
          size shouldBe 7
          distinctBy { it.lyric } shouldHaveSize 2
          single { it.number == SongNumber(BookId.Pv3300, 35) }.run {
            lyric shouldContain "Edited Text 1"
            tonalities shouldBe listOf(Tonality.E_FLAT_MAJOR)
            bibleRef shouldBe null
          }
          single { it.number == SongNumber(BookId.Pv3300, 37) }.run {
            lyric shouldContain "Edit 2!!!!"
            tonalities shouldBe listOf(Tonality.A_MAJOR, Tonality.B_FLAT_MAJOR, Tonality.B_FLAT_MINOR)
            bibleRef shouldBe BibleRef("test bible ref")
          }
        }
      }

      scenario("get tags") {
        val tags = dbProvider.getTags()
        tags.isSuccess shouldBe true
        tags.getOrThrow().run {
          size shouldBe 36
          filter { it.predefined } shouldHaveSize 34
          val customTags = filter { !it.predefined }
          customTags shouldHaveSize 2
          customTags.single { it.name == "New Tag 1" }.let { tag ->
            tag.id shouldBe TagId.parse("custom-44272")
            tag.color shouldBe Color.parse("#ba68c8")
            tag.songNumbers shouldHaveSize 4
          }
          customTags.single { it.name == "New Tag 2" }.let { tag ->
            tag.id shouldBe TagId.parse("custom-32555")
            tag.color shouldBe Color.parse("#e57373")
            tag.songNumbers shouldHaveSize 5
          }
        }
      }
    }
  }

  feature("fetch data from database v2.0.0 (v11-with-user-data)") {
    withSqliteDb(File("src/test/resources/test-db/v11-with-user-data/pws.2.0.0.dbz")) { db ->
      db.version shouldBe 11

      val dbProvider = PwsDb2xDataProvider(db)

      scenario("get favorites") {
        val favorites = dbProvider.getFavorites()
        favorites.isSuccess shouldBe true
        favorites.getOrThrow() shouldBe expectedV200Favorites
      }

      scenario("get history") {
        val history = dbProvider.getHistory()
        history.isSuccess shouldBe true
        history.getOrThrow().run {
          size shouldBe 10
          first() shouldBe HistoryItem(SongNumber(BookId.Pv3300, 1), LocalDateTime.parse("2025-10-29T18:25:04.412076"))
        }
      }

      scenario("get edited songs") {
        val songs = dbProvider.getEditedSongs()
        songs.isSuccess shouldBe true
        songs.getOrThrow().run {
          size shouldBe 15
          distinctBy { it.lyric } shouldHaveSize 5
          single { it.number == SongNumber(BookId.Pv3300, 1) }.run {
            lyric shouldContain "!!!Edited Song - 1!!!"
            tonalities shouldBe listOf(Tonality.B_MAJOR)
            bibleRef shouldBe BibleRef("Bible Ref - Edited!")
          }
          single { it.number == SongNumber(BookId.Pv2555, 2) }.run {
            lyric shouldContain "!!!Edited Song - 2!!!"
            tonalities shouldBe listOf()
            bibleRef shouldBe null
          }
          single { it.number == SongNumber(BookId.Pv2001, 3) }.run {
            lyric shouldContain "!!!Edited Song - 3!!!"
            tonalities shouldBe listOf(Tonality.E_FLAT_MAJOR)
            bibleRef shouldBe BibleRef("Edited Bible Ref")
          }
        }
      }

      scenario("get tags") {
        val tags = dbProvider.getTags()
        tags.isSuccess shouldBe true
        tags.getOrThrow().run {
          size shouldBe 32
          filter { it.predefined } shouldHaveSize 30
          val customTags = filter { !it.predefined }
          customTags shouldHaveSize 2
          customTags.single { it.name == "Custom - 1-1" }.let { customTag1 ->
            customTag1.predefined shouldBe false
            customTag1.id shouldBe TagId.parse("custom-00001")
            customTag1.color shouldBe Color.parse("#942729")
            customTag1.songNumbers shouldHaveSize 6
          }
          customTags.single { it.name == "Custom - 2" }.let { customTag2 ->
            customTag2.name shouldBe "Custom - 2"
            customTag2.predefined shouldBe false
            customTag2.id shouldBe TagId.parse("custom-00002")
            customTag2.color shouldBe Color.parse("#c55097")
            customTag2.songNumbers shouldHaveSize 5
          }
          single { it.id == TagId.AboutChurch }.let { tag ->
            tag.predefined shouldBe true
            tag.songNumbers shouldBe mapOf(BookId.YunostIisusu to (1..2).toSet(), BookId.PesnHvaly to setOf(1, 2, 5))
          }
        }
      }
    }
  }
})