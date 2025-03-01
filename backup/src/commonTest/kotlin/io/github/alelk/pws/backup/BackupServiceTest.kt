package io.github.alelk.pws.backup

import io.github.alelk.pws.backup.model.Backup
import io.github.alelk.pws.backup.model.BookPreference
import io.github.alelk.pws.backup.model.Song
import io.github.alelk.pws.backup.model.SongNumber
import io.github.alelk.pws.backup.model.Tag
import io.github.alelk.pws.domain.model.BibleRef
import io.github.alelk.pws.domain.model.BookId
import io.github.alelk.pws.domain.model.Color
import io.github.alelk.pws.domain.model.Locale
import io.github.alelk.pws.domain.model.Person
import io.github.alelk.pws.domain.model.SongId
import io.github.alelk.pws.domain.model.Tonality
import io.github.alelk.pws.domain.model.Version
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDateTime

class BackupServiceTest : StringSpec({

  val book1Id = BookId.parse("book-1")
  val book2Id = BookId.parse("book-2")
  val backup1 = Backup(
    metadata = Backup.Metadata(createdAt = LocalDateTime.parse("2025-01-01T07:31:05")),
    songs = listOf(
      Song(
        number = SongNumber(book1Id, 1),
        id = SongId(100L),
        version = Version(1, 1),
        locale = Locale.of("en"),
        name = "Song 1",
        lyric = "Verse 1 Line 1\nVerse 1 Line 2\n\nVerse 2 Line 1\nVerse 2 Line 2",
        tonalities = listOf(Tonality.B_MAJOR, Tonality.A_MINOR),
        author = Person("Author 1"),
        translator = Person("Translator 1"),
        composer = Person("Composer 1"),
        bibleRef = BibleRef("Bible Ref")
      )
    ),
    favorites = listOf(
      SongNumber(book1Id, 2),
      SongNumber(book1Id, 3),
      SongNumber(book1Id, 4),
      SongNumber(book2Id, 2),
    ),
    tags = listOf(
      Tag(
        "tag-1",
        Color(100, 100, 100),
        setOf(
          SongNumber(book1Id, 1),
          SongNumber(book1Id, 2),
          SongNumber(book1Id, 3),
          SongNumber(book1Id, 10),
          SongNumber(book2Id, 10),
          SongNumber(book2Id, 20)
        )
      )
    ),
    bookPreferences = listOf(BookPreference(book1Id, 5), BookPreference(book2Id, 10)),
    settings = mapOf("setting-1" to "value-1", "setting-2" to "value-2")
  )
  val backup1Text = """
    |metadata:
    |  createdAt: "2025-01-01T07:31:05"
    |  version: 1
    |songs:
    |- number:
    |    bookId: "book-1"
    |    number: 1
    |  id: 100
    |  version: "1.1"
    |  locale: "en"
    |  name: "Song 1"
    |  lyric: |-
    |    Verse 1 Line 1
    |    Verse 1 Line 2
    |
    |    Verse 2 Line 1
    |    Verse 2 Line 2
    |  tonalities:
    |  - "b major"
    |  - "a minor"
    |  author: "Author 1"
    |  translator: "Translator 1"
    |  composer: "Composer 1"
    |  bibleRef: "Bible Ref"
    |favorites:
    |- bookId: "book-1"
    |  number: 2
    |- bookId: "book-1"
    |  number: 3
    |- bookId: "book-1"
    |  number: 4
    |- bookId: "book-2"
    |  number: 2
    |tags:
    |- name: "tag-1"
    |  color: "#646464"
    |  songs:
    |    "book-1":
    |    - "1-3"
    |    - "10"
    |    "book-2":
    |    - "10"
    |    - "20"
    |bookPreferences:
    |- bookId: "book-1"
    |  preference: 5
    |- bookId: "book-2"
    |  preference: 10
    |settings:
    |  "setting-1": "value-1"
    |  "setting-2": "value-2"""".trimMargin()

  "write backup as text" {
    val bkp = BackupService().writeAsString(backup1)
    bkp shouldBe backup1Text
  }

  "read backup from text" {
    val bkp = BackupService().readFromString(backup1Text)
    bkp shouldBe backup1
  }
})