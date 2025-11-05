package io.github.alelk.pws.backup.model

import com.charleskorn.kaml.Yaml
import io.github.alelk.pws.domain.core.ids.BookId
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class BookPreferenceTest : StringSpec({

  "serialize book preference to yaml" {
    Yaml.default.encodeToString(BookPreference.serializer(), BookPreference(BookId.parse("book-1"), 5)) shouldBe
      """|bookId: "book-1"
         |preference: 5""".trimMargin()
  }

  "deserialize book preference from yaml" {
    val yaml =
      """|bookId: "book-1"
         |preference: 5""".trimMargin()
    Yaml.default.decodeFromString(BookPreference.serializer(), yaml) shouldBe
      BookPreference(BookId.parse("book-1"), 5)
  }
})