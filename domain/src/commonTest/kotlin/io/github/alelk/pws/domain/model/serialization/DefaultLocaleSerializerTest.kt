package io.github.alelk.pws.domain.model.serialization

import io.github.alelk.pws.domain.model.Locale
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class DefaultLocaleSerializerTest : StringSpec({

  "serialize locale to json" {
    Json.encodeToString(Locale.serializer(), Locale.of("en")) shouldBe "\"en\""
    Json.encodeToString(Locale.serializer(), Locale.of("ru")) shouldBe "\"ru\""
    Json.encodeToString(Locale.serializer(), Locale.of("RU")) shouldBe "\"ru\""
    Json.encodeToString(Locale.serializer(), Locale.of("uk")) shouldBe "\"uk\""
  }

  "deserialize locale from json" {
    Json.decodeFromString(Locale.serializer(), "\"ru-RU\"") shouldBe Locale.of("ru")
    Json.decodeFromString(Locale.serializer(), "\"ru\"") shouldBe Locale.of("ru")
    Json.decodeFromString(Locale.serializer(), "\"RU\"") shouldBe Locale.of("ru")
    Json.decodeFromString(Locale.serializer(), "\"en-EN\"") shouldBe Locale.of("en")
    Json.decodeFromString(Locale.serializer(), "\"uk\"") shouldBe Locale.of("uk")
  }
})