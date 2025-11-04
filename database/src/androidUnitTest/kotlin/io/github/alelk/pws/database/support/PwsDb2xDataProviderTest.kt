package io.github.alelk.pws.database.support

import br.com.colman.kotest.FeatureSpec
import br.com.colman.kotest.android.extensions.robolectric.RobolectricTest
import io.github.alelk.pws.database.setupTimberForTest
import io.github.alelk.pws.database.withSqliteDb
import io.github.alelk.pws.domain.core.ids.AboutChurch
import io.github.alelk.pws.domain.bible.BibleRef
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

  feature("fetch data from database v2.0.0 (v11-with-user-data)") {
    withSqliteDb(File("src/androidUnitTest/resources/test-db/v11-with-user-data/pws.2.0.0.dbz")) { db ->
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