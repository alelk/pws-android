package io.github.alelk.pws.domain.bible

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class BibleRefSerializerTest : StringSpec({

  "serialize bible ref to json" {
    val json = Json.Default.encodeToString(BibleRef("Bible Ref"))
    json shouldEqualJson "\"Bible Ref\""
  }

  "deserialize bible ref from json" {
    Json.Default.decodeFromString<BibleRef>("\"Bible Ref\"") shouldBe BibleRef("Bible Ref")
  }
})