package io.github.alelk.pws.backup.model

import com.charleskorn.kaml.MultiLineStringStyle
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import io.github.alelk.pws.domain.model.BibleRef
import io.github.alelk.pws.domain.model.BookExternalId
import io.github.alelk.pws.domain.model.Person
import io.github.alelk.pws.domain.model.Tonality
import io.github.alelk.pws.domain.model.Version
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.util.Locale

class SongTest : StringSpec({

  val yamlConverter = Yaml(configuration = YamlConfiguration(multiLineStringStyle = MultiLineStringStyle.Literal))

  val song = Song(
    number = SongNumber(BookExternalId.parse("Book-1"), 1),
    id = 1,
    version = Version(1, 0),
    locale = Locale.forLanguageTag("en"),
    name = "Song Name",
    lyric = "Verse 1 Line 1\nVerse 1 Line 2\n\nVerse 2 Line 1\nVerse 2 Line 2",
    tonalities = listOf(Tonality.A_MAJOR, Tonality.B_MAJOR),
    author = Person("Author"),
    translator = Person("Translator"),
    composer = Person("Composer"),
    bibleRef = BibleRef("Bible Ref")
  )

  val yaml = """
      |number:
      |  bookId: "Book-1"
      |  number: 1
      |id: 1
      |version: "1.0"
      |locale: "en"
      |name: "Song Name"
      |lyric: |-
      |  Verse 1 Line 1
      |  Verse 1 Line 2
      |
      |  Verse 2 Line 1
      |  Verse 2 Line 2
      |tonalities:
      |- "a major"
      |- "b major"
      |author: "Author"
      |translator: "Translator"
      |composer: "Composer"
      |bibleRef: "Bible Ref"
      """.trimMargin()

  "serialize song to yaml" {
    yamlConverter.encodeToString(Song.serializer(), song) shouldBe yaml
  }

  "deserialize song from yaml" {
    yamlConverter.decodeFromString(Song.serializer(), yaml) shouldBe song
  }
})