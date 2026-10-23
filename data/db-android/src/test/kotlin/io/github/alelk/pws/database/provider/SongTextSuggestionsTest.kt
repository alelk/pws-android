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

  suspend fun PwsDatabase.seedSong() {
    val rs = RandomSource.seeded(42L)
    val book: BookEntity = Arb.bookEntity().next(rs)
    val song: SongEntity = Arb.songEntity().next(rs).copy(id = SongId(1), name = "Святий Боже", lyric = "Святий Боже, святий кріпкий", edited = false)
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
