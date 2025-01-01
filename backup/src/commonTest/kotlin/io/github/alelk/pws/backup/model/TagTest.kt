package io.github.alelk.pws.backup.model

import com.charleskorn.kaml.Yaml
import io.github.alelk.pws.domain.model.BookExternalId
import io.github.alelk.pws.domain.model.Color
import io.github.alelk.pws.domain.model.TagId
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class TagTest : StringSpec({
  val book1Id = BookExternalId.parse("book-1")
  val book2Id = BookExternalId.parse("book-2")

  val tag1 = Tag(
    TagId.parse("tag1"),
    Color.parse("#ff0000"),
    setOf(
      SongNumber(book1Id, 1),
      SongNumber(book1Id, 2),
      SongNumber(book1Id, 3),
      SongNumber(book1Id, 10),
      SongNumber(book2Id, 3),
    )
  )

  val tag1Yaml = """
    |id: "tag1"
    |color: "#ff0000"
    |songs:
    |  "book-1":
    |  - "1-3"
    |  - "10"
    |  "book-2":
    |  - "3"""".trimMargin()

  "serialize tag to yaml" {
    Yaml.default.encodeToString(Tag.serializer(), tag1) shouldBe tag1Yaml
  }

  "deserialize tag from yaml" {
    Yaml.default.decodeFromString(Tag.serializer(), tag1Yaml) shouldBe tag1
  }
})