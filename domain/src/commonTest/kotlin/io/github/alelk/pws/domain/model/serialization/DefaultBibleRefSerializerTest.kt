package io.github.alelk.pws.domain.model.serialization

import io.github.alelk.pws.domain.model.BibleRef
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class DefaultBibleRefSerializerTest : StringSpec({

  "serialize bible ref to json" {
    val json = Json.encodeToString(BibleRef("Bible Ref"))
    json shouldEqualJson "\"Bible Ref\""
  }

  "deserialize bible ref from json" {
    Json.decodeFromString<BibleRef>("\"Bible Ref\"") shouldBe BibleRef("Bible Ref")
  }
})