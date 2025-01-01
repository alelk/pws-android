package io.github.alelk.pws.backup.model.serialization

import com.charleskorn.kaml.Yaml
import io.github.alelk.pws.backup.model.Numbers
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class NumbersSerializerTest : StringSpec({
    "serialize numbers range" {
        Yaml.default.encodeToString(NumbersSerializer, Numbers.Range(1, 10)) shouldBe "\"1-10\""
    }

    "serialize numbers single" {
        Yaml.default.encodeToString(NumbersSerializer, Numbers.Single(15)) shouldBe "\"15\""
    }

    "deserialize numbers range" {
        Yaml.default.decodeFromString(NumbersSerializer, "\"1-10\"") shouldBe Numbers.Range(1, 10)
    }
    "deserialize numbers single" {
        Yaml.default.decodeFromString(NumbersSerializer, "\"15\"") shouldBe Numbers.Single(15)
    }
})