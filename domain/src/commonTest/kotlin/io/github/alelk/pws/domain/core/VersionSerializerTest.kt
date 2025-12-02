package io.github.alelk.pws.domain.core

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.checkAll
import kotlinx.serialization.json.Json

class VersionSerializerTest : StringSpec({

  "serialize/deserialize version to/from json" {
    checkAll(10, Arb.version()) { version ->
      val json = Json.encodeToString(Version.serializer(), version)
      json shouldBe "\"${version.major}.${version.minor}\""
      val version2 = Json.decodeFromString(Version.serializer(), json)
      version2 shouldBe version
    }
  }
})