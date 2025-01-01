package io.github.alelk.pws.backup.model.serialization

import com.charleskorn.kaml.Yaml
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.of
import io.kotest.property.checkAll
import java.util.*

class LocaleSerializerTest : StringSpec({

  "serialize locale to yaml" {
    checkAll(10, Arb.of(listOf("en-EN", "ru-RU", "ru", "uk"))) { locale ->
      val yaml = Yaml.default.encodeToString(LocaleSerializer, Locale.forLanguageTag(locale))
      yaml shouldBe "\"$locale\""
    }
  }

  "deserialize locale from yaml" {
    Yaml.default.decodeFromString(LocaleSerializer, "\"ru-RU\"") shouldBe Locale.forLanguageTag("ru-RU")
  }
})