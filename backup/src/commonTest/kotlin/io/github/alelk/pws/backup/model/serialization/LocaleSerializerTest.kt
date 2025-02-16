package io.github.alelk.pws.backup.model.serialization

import com.charleskorn.kaml.Yaml
import io.github.alelk.pws.domain.model.Locale
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class LocaleSerializerTest : StringSpec({

  "serialize locale to yaml" {
    Yaml.default.encodeToString(LocaleSerializer, Locale.of("en")) shouldBe "\"en\""
    Yaml.default.encodeToString(LocaleSerializer, Locale.of("ru")) shouldBe "\"ru\""
    Yaml.default.encodeToString(LocaleSerializer, Locale.of("RU")) shouldBe "\"ru\""
    Yaml.default.encodeToString(LocaleSerializer, Locale.of("uk")) shouldBe "\"uk\""
  }

  "deserialize locale from yaml" {
    Yaml.default.decodeFromString(LocaleSerializer, "\"ru-RU\"") shouldBe Locale.of("ru")
    Yaml.default.decodeFromString(LocaleSerializer, "\"ru\"") shouldBe Locale.of("ru")
    Yaml.default.decodeFromString(LocaleSerializer, "\"RU\"") shouldBe Locale.of("ru")
    Yaml.default.decodeFromString(LocaleSerializer, "\"en-EN\"") shouldBe Locale.of("en")
    Yaml.default.decodeFromString(LocaleSerializer, "\"uk\"") shouldBe Locale.of("uk")
  }
})