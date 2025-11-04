package io.github.alelk.pws.domain.core.ids

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class SongNumberIdSerializerTest : StringSpec({
  val json = Json

  "serialize song number id to json string" {
    val id = SongNumberId(BookId.parse("book-A"), SongId(42))
    val encoded = json.encodeToString(SongNumberIdSerializer, id)
    encoded shouldBe "\"book-A/42\""
  }

  "deserialize song number id from json string" {
    val decoded = json.decodeFromString(SongNumberIdSerializer, "\"book-A/42\"")
    decoded.bookId shouldBe BookId.parse("book-A")
    decoded.songId shouldBe SongId(42)
    decoded.toString() shouldBe "book-A/42"
  }

  "round trip random sample" {
    val original = SongNumberId(BookId.parse("book-z"), SongId(9999))
    val asJson = json.encodeToString(SongNumberIdSerializer, original)
    val restored = json.decodeFromString(SongNumberIdSerializer, asJson)
    restored shouldBe original
  }
})

