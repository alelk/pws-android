package io.github.alelk.pws.backup.model.serialization

import com.charleskorn.kaml.Yaml
import io.github.alelk.pws.domain.model.BookId
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class BookExternalIdSerializerTest : StringSpec({

  "serialize book id to yaml" {
    val yaml = Yaml.default.encodeToString(BookIdSerializer, BookId.parse("Book123"))
    yaml shouldBe """"Book123""""
  }

  "deserialize book id from yaml" {
    val s = Yaml.default.decodeFromString(BookIdSerializer, "Book-123")
    s shouldBe BookId.parse("Book-123")
  }
})