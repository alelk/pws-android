package io.github.alelk.pws.domain.model.serialization

import io.github.alelk.pws.domain.model.Year
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class DefaultYearSerializerTest : StringSpec({

  "serialize year to json" {
    val json = Json.encodeToString(Year.serializer(), Year(2025))
    json shouldBe """"2025""""
  }

  "deserialize year from json" {
    val value = Json.decodeFromString(Year.serializer(), "\"2025\"")
    value shouldBe Year.parse("2025")
  }
})