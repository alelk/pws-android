package io.github.alelk.pws.backup.model.serialization

import com.charleskorn.kaml.Yaml
import io.github.alelk.pws.backup.model.Lyric
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class LyricSerializerTest : StringSpec({

  "serialize lyric to yaml" {
    val yaml = Yaml.default.encodeToString(LyricSerializer, Lyric("Lyric"))
    yaml shouldBe """"Lyric""""
  }

  "deserialize lyric from yaml" {
    val l = Yaml.default.decodeFromString(LyricSerializer, "Lyric")
    l shouldBe Lyric("Lyric")
  }
})