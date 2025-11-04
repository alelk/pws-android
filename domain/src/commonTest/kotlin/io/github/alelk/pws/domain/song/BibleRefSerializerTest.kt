package io.github.alelk.pws.domain.song

import io.github.alelk.pws.domain.core.BibleRef
import io.github.alelk.pws.domain.song.model.BibleRefSerializer
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class BibleRefSerializerTest : StringSpec({

  "serialize bible ref to json" {
    val json = Json.encodeToString(BibleRefSerializer, BibleRef("Bible Ref"))

    json shouldEqualJson "\"Bible Ref\""
  }

  "deserialize bible ref from json" {
    Json.decodeFromString(BibleRefSerializer, "\"Bible Ref\"") shouldBe BibleRef("Bible Ref")
  }
})