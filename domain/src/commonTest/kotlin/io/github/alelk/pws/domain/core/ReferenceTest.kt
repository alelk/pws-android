package io.github.alelk.pws.domain.core

import io.github.alelk.pws.domain.core.ids.BookId
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe

class ReferenceTest : FeatureSpec({

  val json = kotlinx.serialization.json.Json { encodeDefaults = true }

  feature("bible ref serialize/deserialize") {
    val r = BibleRef(text = "John 3:16")
    val expectedJson = """{"type":"bible-ref","value":"John 3:16"}"""

    scenario("serialise to json") {
      json.encodeToString(Reference.serializer(), r) shouldEqualJson expectedJson
    }

    scenario("deserialize from json") {
      json.decodeFromString(Reference.serializer(), expectedJson) shouldBe r
    }
  }

  feature("song ref serialize/deserialize") {
    val r = SongRef(
      reason = SongRefReason.Variation,
      number = SongNumber(BookId.parse("book1"), 10),
      80
    )
    val expectedJson = """{"type":"song-ref","reason":"variation","bookId":"book1","number":10,"volume":80}"""

    scenario("serialise to json") {
      json.encodeToString(Reference.serializer(), r) shouldEqualJson expectedJson
    }

    scenario("deserialize from json") {
      json.decodeFromString(Reference.serializer(), expectedJson) shouldBe r
    }
  }

})