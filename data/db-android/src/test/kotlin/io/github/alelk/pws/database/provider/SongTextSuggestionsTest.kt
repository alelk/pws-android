package io.github.alelk.pws.database.provider

import android.app.SearchManager
import br.com.colman.kotest.FeatureSpec
import br.com.colman.kotest.android.extensions.robolectric.RobolectricTest
import io.github.alelk.pws.database.PwsDatabase
import io.github.alelk.pws.database.bookstatistic.BookStatisticEntity
import io.github.alelk.pws.database.pwsDbForTest
import io.github.alelk.pws.database.setupTimberForTest
import io.github.alelk.pws.database.song_number.SongNumberEntity
import io.github.alelk.pws.database.song.SongEntity
import io.github.alelk.pws.database.book.BookEntity
import io.github.alelk.pws.database.book.bookEntity
import io.github.alelk.pws.database.song.songEntity
import io.github.alelk.pws.domain.core.ids.SongId
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.RandomSource
import io.kotest.property.arbitrary.next

/**
 * Text suggestions of the system search: multi-word input must find songs (it used to become `свят*бог*`), and no
 * input may throw. Robolectric + in-memory Room; not runnable on the aarch64 agent sandbox (aapt2 is x86_64-only).
 */
@RobolectricTest(sdk = [34])
class SongTextSuggestionsTest : FeatureSpec({

  beforeContainer { setupTimberForTest() }

  suspend fun PwsDatabase.seedSong(name: String = "Святий Боже", lyric: String = "Святий Боже, святий кріпкий") {
    val rs = RandomSource.seeded(42L)
    val book: BookEntity = Arb.bookEntity().next(rs)
    val song: SongEntity = Arb.songEntity().next(rs).copy(id = SongId(1), name = name, lyric = lyric, edited = false)
    bookDao().insert(listOf(book))
    bookStatisticDao().insert(listOf(BookStatisticEntity(id = book.id, priority = 10)))
    songDao().insert(listOf(song))
    songNumberDao().insert(listOf(SongNumberEntity(bookId = book.id, songId = song.id, number = 1, priority = 0)))
  }

  feature("suggestionsByText") {

    scenario("a multi-word query finds the song by name") {
      val db = pwsDbForTest(inMemory = true, name = "suggestions-test.db")
      try {
        db.seedSong()
        val cursor = db.songDao().suggestionsByText("свят бо", limit = 10)
        cursor.count shouldBe 1
        cursor.moveToFirst()
        cursor.getString(cursor.getColumnIndexOrThrow(SearchManager.SUGGEST_COLUMN_TEXT_1)) shouldBe "Святий Боже"
      } finally {
        db.close()
      }
    }

    scenario("folded letters and apostrophes find the song and the original text is shown") {
      val db = pwsDbForTest(inMemory = true, name = "suggestions-test.db")
      try {
        db.seedSong(name = "Ґанок", lyric = "Єдиний їжак і пʼять яблук")
        fun texts(input: String): List<String> =
          db.songDao().suggestionsByText(input, limit = 10).use { c ->
            buildList {
              while (c.moveToNext()) add(c.getString(c.getColumnIndexOrThrow(SearchManager.SUGGEST_COLUMN_TEXT_1)))
            }
          }
        texts("ганок") shouldBe listOf("Ґанок") // by name
        texts("ГАНОК") shouldBe listOf("Ґанок")
        texts("ижак") shouldBe listOf("Ґанок") // by lyric: the name does not match, TEXT_1 still shows the original name
        texts("пять") shouldBe listOf("Ґанок") // «пʼять» (U+02BC) in the lyric
        texts("п'ять") shouldBe listOf("Ґанок")
        texts("едини") shouldBe listOf("Ґанок")
        texts("відсутнє") shouldBe emptyList()
      } finally {
        db.close()
      }
    }

    scenario("any input yields a cursor and never throws") {
      val db = pwsDbForTest(inMemory = true, name = "suggestions-test.db")
      try {
        db.seedSong()
        for (input in listOf("(бог", "а\"б", "бог AND", "NOT", "NEAR", "OR", "*", "^", ":", "-", "   ", "", "()\"*")) {
          db.songDao().suggestionsByText(input, limit = 10).close()
        }
      } finally {
        db.close()
      }
    }
  }
})
