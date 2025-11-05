package io.github.alelk.pws.domain.core.ids

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class TagIdSerializerTest : StringSpec({

  "serialize tag id to json" {
    val json = Json.encodeToString(TagId.serializer(), TagId.parse("tag-123"))
    json shouldBe """"tag-123""""
  }

  "deserialize tag id from json" {
    val value = Json.decodeFromString(TagId.serializer(), "\"tag-123\"")
    value shouldBe TagId.parse("tag-123")
  }
})