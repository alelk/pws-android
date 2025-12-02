package io.github.alelk.pws.domain.core

import io.github.alelk.pws.domain.core.ids.BookId
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.checkAll

class SongNumberTest : StringSpec({
  "parse song number" {
    val songNumber = SongNumber.parse("book-1#1")
    songNumber.bookId shouldBe BookId.Companion.parse("book-1")
    songNumber.number shouldBe 1
  }

  "convert song number to string" {
    val songNumber = SongNumber(BookId.Companion.parse("book-1"), 1000)
    songNumber.toString() shouldBe "book-1#1000"
  }

  "convert random song number to string and parse it back" {
      checkAll(Arb.Companion.songNumber()) { songNumber ->
          val songNumberString = songNumber.toString()
          val parsedSongNumber = SongNumber.parse(songNumberString)
          parsedSongNumber shouldBe songNumber
      }
  }
})