package io.github.alelk.pws.domain.model.serialization

import io.github.alelk.pws.domain.model.Person
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class DefaultPersonSerializerTest : StringSpec({

  "serialize person to json" {
    val json = Json.encodeToString(Person.serializer(), Person("Person Name"))
    json shouldBe """"Person Name""""
  }

  "deserialize person from json" {
    val p = Json.decodeFromString(Person.serializer(), "\"Person Name\"")
    p shouldBe Person("Person Name")
  }
})