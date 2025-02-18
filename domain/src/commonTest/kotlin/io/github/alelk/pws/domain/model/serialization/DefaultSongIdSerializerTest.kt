package io.github.alelk.pws.domain.model.serialization

import io.github.alelk.pws.domain.model.SongId
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class DefaultSongIdSerializerTest : StringSpec({

  "serialize song id to json" {
    val json = Json.encodeToString(SongId.serializer(), SongId(123))
    json shouldBe "123"
  }

  "deserialize song id from json" {
    val value = Json.decodeFromString(SongId.serializer(), "123")
    value shouldBe SongId.parse("123")
  }
})