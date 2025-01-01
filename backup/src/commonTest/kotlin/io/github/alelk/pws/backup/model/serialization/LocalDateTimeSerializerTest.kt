package io.github.alelk.pws.backup.model.serialization

import com.charleskorn.kaml.Yaml
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class LocalDateTimeSerializerTest : StringSpec({

  "serialize local date time to yaml" {
    val yaml = Yaml.default.encodeToString(LocalDateTimeSerializer, LocalDateTime.parse("2024-12-31T01:57:59"))
    yaml shouldBe "\"2024-12-31T01:57:59\""
  }

  "deserialize local date time from yaml" {
    Yaml.default.decodeFromString(LocalDateTimeSerializer, "\"2024-12-31T01:57:59\"") shouldBe LocalDateTime.parse("2024-12-31T01:57:59")
  }
})