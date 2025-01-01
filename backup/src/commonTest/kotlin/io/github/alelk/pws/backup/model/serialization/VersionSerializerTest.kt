package io.github.alelk.pws.backup.model.serialization

import com.charleskorn.kaml.Yaml
import io.github.alelk.pws.domain.model.version
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.checkAll

class VersionSerializerTest : StringSpec({

  "serialize/deserialize version to/from yaml" {
    checkAll(10, Arb.version()) { version ->
      val yaml = Yaml.default.encodeToString(VersionSerializer, version)
      yaml shouldBe "\"${version.major}.${version.minor}\""
      val version2 = Yaml.default.decodeFromString(VersionSerializer, yaml)
      version2 shouldBe version
    }
  }
})