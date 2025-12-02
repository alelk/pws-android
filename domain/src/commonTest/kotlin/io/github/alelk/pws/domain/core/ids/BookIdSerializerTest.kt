package io.github.alelk.pws.domain.core.ids

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class BookIdSerializerTest : StringSpec({

  "serialize book id to json" {
    val json = Json.encodeToString(BookId.serializer(), BookId.parse("Book123"))
    json shouldBe """"Book123""""
  }

  "deserialize book id from json" {
    val s = Json.decodeFromString(BookId.serializer(), "\"Book-123\"")
    s shouldBe BookId.parse("Book-123")
  }
})