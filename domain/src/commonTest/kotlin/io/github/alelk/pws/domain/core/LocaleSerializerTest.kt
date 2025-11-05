package io.github.alelk.pws.domain.core

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class LocaleSerializerTest : StringSpec({

  "serialize locale to json" {
    Json.Default.encodeToString(Locale.serializer(), Locale.of("en")) shouldBe "\"en\""
    Json.Default.encodeToString(Locale.serializer(), Locale.of("ru")) shouldBe "\"ru\""
    Json.Default.encodeToString(Locale.serializer(), Locale.of("RU")) shouldBe "\"ru\""
    Json.Default.encodeToString(Locale.serializer(), Locale.of("uk")) shouldBe "\"uk\""
  }

  "deserialize locale from json" {
    Json.Default.decodeFromString(Locale.serializer(), "\"ru-RU\"") shouldBe Locale.of("ru")
    Json.Default.decodeFromString(Locale.serializer(), "\"ru\"") shouldBe Locale.of("ru")
    Json.Default.decodeFromString(Locale.serializer(), "\"RU\"") shouldBe Locale.of("ru")
    Json.Default.decodeFromString(Locale.serializer(), "\"en-EN\"") shouldBe Locale.of("en")
    Json.Default.decodeFromString(Locale.serializer(), "\"uk\"") shouldBe Locale.of("uk")
  }
})