package io.github.alelk.pws.domain.model.serialization

import io.github.alelk.pws.domain.model.BookId
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class DefaultBookIdSerializerTest : StringSpec({

  "serialize book id to json" {
    val json = Json.encodeToString(BookId.serializer(), BookId.parse("Book123"))
    json shouldBe """"Book123""""
  }

  "deserialize book id from json" {
    val s = Json.decodeFromString(BookId.serializer(), "\"Book-123\"")
    s shouldBe BookId.parse("Book-123")
  }
})