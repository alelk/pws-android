package io.github.alelk.pws.backup.model.serialization

import com.charleskorn.kaml.Yaml
import io.github.alelk.pws.domain.model.Person
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class PersonSerializerTest : StringSpec({

  "serialize person to yaml" {
    val yaml = Yaml.default.encodeToString(PersonSerializer, Person("Person Name"))
    yaml shouldBe """"Person Name""""
  }

  "deserialize person from yaml" {
    val p = Yaml.default.decodeFromString(PersonSerializer, "Person Name")
    p shouldBe Person("Person Name")
  }
})