package io.github.alelk.pws.backup.model.serialization

import com.charleskorn.kaml.Yaml
import io.github.alelk.pws.domain.model.Color
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ColorSerializerTest : StringSpec({
  "serialize color to yaml" {
    Yaml.default.encodeToString(ColorSerializer, Color(100, 100, 100)) shouldBe "\"#646464\""
  }

  "deserialize color from yaml" {
    Yaml.default.decodeFromString(ColorSerializer, "\"#646464\"") shouldBe Color(100, 100, 100)
  }
})