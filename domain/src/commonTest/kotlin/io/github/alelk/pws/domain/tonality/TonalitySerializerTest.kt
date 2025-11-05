package io.github.alelk.pws.domain.tonality

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class TonalitySerializerTest : StringSpec({

  "serialize tonality to json" {
    val json = Json.encodeToString(Tonality.serializer(), Tonality.A_MINOR)
    json shouldBe "\"a minor\""
  }

  "deserialize tonality from yaml" {
    val tonality = Json.decodeFromString(Tonality.serializer(), "\"a minor\"")
    tonality shouldBe Tonality.A_MINOR
  }
})