package io.github.alelk.pws.backup.model

import com.charleskorn.kaml.Yaml
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDateTime

class BackupTest : StringSpec({

  val backup1 = Backup(metadata = Backup.Metadata(LocalDateTime.parse("2025-01-01T00:00:10")))

  val backup1Yaml = """
    |metadata:
    |  createdAt: "2025-01-01T00:00:10"
    |  version: 1
    |songs: null
    |favorites: null
    |tags: null
    |bookPreferences: null
    |settings: null""".trimMargin()

  "serialize empty backup to yaml" {
    Yaml.default.encodeToString(Backup.serializer(), backup1) shouldBe backup1Yaml
  }

  "deserialize empty backup from yaml" {
    Yaml.default.decodeFromString(Backup.serializer(), backup1Yaml) shouldBe backup1
  }

  val backup2Yaml = """
    |metadata:
    |  createdAt: "2025-01-01T00:00:00"
    |  version: 210""".trimMargin()

  "deserialize backup with version 210 from yaml" {
    val backup2 = Yaml.default.decodeFromString(Backup.serializer(), backup2Yaml)
    backup2.metadata.version shouldBe 210
  }

})