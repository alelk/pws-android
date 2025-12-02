package io.github.alelk.pws.backup.model

import com.charleskorn.kaml.Yaml
import io.github.alelk.pws.domain.core.ids.BookId
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class SongNumberTest : StringSpec({

  "serialize song number to yaml" {
    val songNumber = SongNumber(bookId = BookId.parse("Book-123"), number = 1)
    val yaml = Yaml.default.encodeToString(SongNumber.serializer(), songNumber)
    yaml shouldBe """
      |bookId: "Book-123"
      |number: 1
    """.trimMargin()
  }

  "deserialize song number from yaml" {
    val yaml = """
      |bookId: "Book-123"
      |number: 1
    """.trimMargin()
    val songNumber = Yaml.default.decodeFromString(SongNumber.serializer(), yaml)
    songNumber shouldBe SongNumber(bookId = BookId.parse("Book-123"), number = 1)
  }

})