package io.github.alelk.pws.domain.model.serialization

import io.github.alelk.pws.domain.model.Color
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class DefaultColorSerializerTest : StringSpec({
  "serialize color to json" {
    Json.encodeToString(Color.serializer(), Color(100, 100, 100)) shouldBe "\"#646464\""
  }

  "deserialize color from json" {
    Json.decodeFromString(Color.serializer(), "\"#646464\"") shouldBe Color(100, 100, 100)
  }
})