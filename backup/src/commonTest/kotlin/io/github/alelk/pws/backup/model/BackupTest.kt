package io.github.alelk.pws.backup.model

import com.charleskorn.kaml.Yaml
import io.github.alelk.pws.domain.model.BookId
import io.github.alelk.pws.domain.model.Color
import io.github.alelk.pws.domain.model.Locale
import io.github.alelk.pws.domain.model.Pv3300
import io.github.alelk.pws.domain.model.Pv800
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDateTime

class BackupTest : StringSpec({

  val backupV1Yaml = """
    |metadata:
    |  createdAt: "2025-01-01T00:00:10"
    |  version: 1
    |songs: 
    |  - number:
    |      bookId: PV3300
    |      number: 1
    |    id: 1
    |    version: "1.0"
    |    locale: "en"
    |    name: "Song Name"
    |    lyric: |-
    |      Verse 1 Line 1
    |      Verse 1 Line 2
    |    
    |      Verse 2 Line 1
    |      Verse 2 Line 2
    |    tonalities:
    |    - "a major"
    |    - "b major"
    |    author: "Author"
    |    translator: "Translator"
    |    composer: "Composer"
    |    bibleRef: "Bible Ref"
    |favorites: 
    |  - bookId: "PV3300"
    |    number: 1
    |  - bookId: "PV3300"
    |    number: 2
    |tags: 
    |  - name: "tag1"
    |    color: "#ff0000"
    |    songs:
    |      "PV3300":
    |        - "1-3"
    |        - "10"
    |bookPreferences:
    |  - bookId: "PV3300"
    |    preference: 5
    |  - bookId: "PV800"
    |    preference: 10
    |settings:
    |  "setting-1": "value-1"
    |  "setting-2": "value-2"""".trimMargin()

  "deserialize backup of version 1 from yaml" {
    Yaml.default.decodeFromString(Backup.serializer(), backupV1Yaml).run {
      metadata.version shouldBe 1
      metadata.createdAt shouldBe LocalDateTime.parse("2025-01-01T00:00:10")
      metadata.defaultLocale shouldBe null
      songs!!.run {
        this shouldHaveSize 1
        get(0).number shouldBe SongNumber(BookId.Pv3300, 1)
      }
      favorites shouldContainExactly listOf(SongNumber(BookId.Pv3300, 1), SongNumber(BookId.Pv3300, 2))
      tags!!.run {
        this shouldHaveSize 1
        get(0) shouldBe
          Tag(
            "tag1",
            color = Color.parse("#ff0000"),
            setOf(SongNumber(BookId.Pv3300, 1), SongNumber(BookId.Pv3300, 2), SongNumber(BookId.Pv3300, 3), SongNumber(BookId.Pv3300, 10))
          )
      }
      bookPreferences shouldBe listOf(BookPreference(BookId.Pv3300, 5), BookPreference(BookId.Pv800, 10))
      settings shouldBe mapOf("setting-1" to "value-1", "setting-2" to "value-2")
    }
  }

  val backup1 = Backup(metadata = Backup.Metadata(LocalDateTime.parse("2025-01-01T00:00:10"), defaultLocale = Locale.EN, source = "source-1"))

  val backup1Yaml = """
    |metadata:
    |  createdAt: "2025-01-01T00:00:10"
    |  defaultLocale: "en"
    |  source: "source-1"
    |  version: 2
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