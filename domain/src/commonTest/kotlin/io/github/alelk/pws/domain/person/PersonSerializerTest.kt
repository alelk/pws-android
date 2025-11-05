package io.github.alelk.pws.domain.person

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class PersonSerializerTest : StringSpec({

  "serialize person to json" {
    val json = Json.Default.encodeToString(Person.serializer(), Person("Person Name"))
    json shouldBe """"Person Name""""
  }

  "deserialize person from json" {
    val p = Json.Default.decodeFromString(Person.serializer(), "\"Person Name\"")
    p shouldBe Person("Person Name")
  }
})