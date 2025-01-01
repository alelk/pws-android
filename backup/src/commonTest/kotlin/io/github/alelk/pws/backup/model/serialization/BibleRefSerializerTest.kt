package io.github.alelk.pws.backup.model.serialization

import com.charleskorn.kaml.Yaml
import io.github.alelk.pws.domain.model.BibleRef
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class BibleRefSerializerTest : StringSpec({

  "serialize bible ref to yaml" {
    val yaml = Yaml.default.encodeToString(BibleRefSerializer, BibleRef("Bible Ref"))
    yaml shouldBe "\"Bible Ref\""
  }

  "deserialize bible ref from yaml" {
    Yaml.default.decodeFromString(BibleRefSerializer, "\"Bible Ref\"") shouldBe BibleRef("Bible Ref")
  }
})