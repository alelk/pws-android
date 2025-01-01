package io.github.alelk.pws.backup.model.serialization

import com.charleskorn.kaml.Yaml
import io.github.alelk.pws.domain.model.Tonality
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class TonalitySerializerTest: StringSpec({

  "serialize tonality to yaml" {
    val yaml = Yaml.default.encodeToString(TonalitySerializer, Tonality.A_MINOR)
    yaml shouldBe "\"a minor\""
  }

  "deserialize tonality from yaml" {
    val tonality = Yaml.default.decodeFromString(TonalitySerializer, "\"a minor\"")
    tonality shouldBe Tonality.A_MINOR
  }
})