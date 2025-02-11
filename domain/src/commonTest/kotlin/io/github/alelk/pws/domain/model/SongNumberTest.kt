package io.github.alelk.pws.domain.model

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.checkAll

class SongNumberTest : StringSpec({
  "parse song number id" {
    val songNumberId = SongNumberId.parse("book-1/1")
    songNumberId.bookId shouldBe BookId.parse("book-1")
    songNumberId.songId shouldBe SongId(1)
  }

  "convert song number id to string" {
    val songNumberId = SongNumberId(BookId.parse("book-1"), SongId(1))
    songNumberId.toString() shouldBe "book-1/1"
  }

  "convert random song number id to string and parse it back" {
    checkAll(Arb.songNumberId()) { songNumberId ->
      val songNumberIdString = songNumberId.toString()
      val parsedSongNumberId = SongNumberId.parse(songNumberIdString)
      parsedSongNumberId shouldBe songNumberId
    }
  }
})