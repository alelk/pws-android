package io.github.alelk.pws.domain.model.serialization

import io.github.alelk.pws.domain.model.TagId
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class DefaultTagIdSerializerTest : StringSpec({

  "serialize tag id to json" {
    val json = Json.encodeToString(TagId.serializer(), TagId.parse("tag-123"))
    json shouldBe """"tag-123""""
  }

  "deserialize tag id from json" {
    val value = Json.decodeFromString(TagId.serializer(), "\"tag-123\"")
    value shouldBe TagId.parse("tag-123")
  }
})