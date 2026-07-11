package io.github.alelk.pws.contentdelivery.install

import br.com.colman.kotest.FeatureSpec
import br.com.colman.kotest.android.extensions.robolectric.RobolectricTest
import io.github.alelk.pws.contentdelivery.bookBundle
import io.github.alelk.pws.contentdelivery.inMemoryPwsDb
import io.github.alelk.pws.contentdelivery.portableBook
import io.github.alelk.pws.contentdelivery.portableSong
import io.github.alelk.pws.contentdelivery.portableSongNumber
import io.github.alelk.pws.contentdelivery.portableTag
import io.github.alelk.pws.contentdelivery.songReference
import io.github.alelk.pws.database.PwsDatabase
import io.github.alelk.pws.database.history.HistoryEntity
import io.github.alelk.pws.domain.booklibrary.model.BookInstallSource
import io.github.alelk.pws.domain.core.Version
import io.github.alelk.pws.domain.core.ids.BookId
import io.github.alelk.pws.domain.core.ids.SongId
import io.github.alelk.pws.domain.core.ids.SongNumberId
import io.github.alelk.pws.portable.model.Song
import io.github.alelk.pws.portable.model.SongNumber
import io.github.alelk.pws.portable.model.SongReference
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.RandomSource
import io.kotest.property.arbitrary.constant
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.next
import io.kotest.property.checkAll

/**
 * Tests for [BookImporterImpl] — the core of pluggable-book delivery. Covers the high-risk
 * branches: preserving user-edited songs, deferring cross-book references until both songs are
 * installed, tag→song matching, and idempotency of repeated imports.
 *
 * Test data is produced by the project's `Arb` bundle generators (see `bundleArb.kt`); structural
 * scenarios pin the fields under test with `Arb.constant(...)` and randomise the rest.
 */
@RobolectricTest(sdk = 34)
class BookImporterImplTest : FeatureSpec({

  val rs = RandomSource.seeded(20260621L)
  val bookId = BookId.parse("Book-1")

  // A bundle whose songs all belong to [bookId] and have distinct ids — the shape the importer
  // expects for a single book.
  fun bundleForBook(id: BookId) = Arb.bookBundle(
    book = Arb.portableBook(id = Arb.constant(id)),
    songs = Arb.list(Arb.portableSong(number = Arb.portableSongNumber(bookId = Arb.constant(id))), 1..4)
      .map { songs -> songs.distinctBy { it.id } },
  )

  suspend fun <T> withDb(block: suspend (PwsDatabase) -> T): T {
    val db = inMemoryPwsDb()
    return try { block(db) } finally { db.close() }
  }

  // Builders for structural single-book scenarios: a song pinned to a (book, number) and version.
  fun songIn(book: BookId, id: Long, number: Int, version: Version, lyric: String = "lyric"): Song =
    Arb.portableSong(
      id = Arb.constant(SongId(id)),
      number = Arb.constant(SongNumber(book, number)),
      version = Arb.constant(version),
      lyric = Arb.constant(lyric),
    ).next(rs)

  fun song(id: Long, number: Int, version: Version, lyric: String = "lyric"): Song =
    songIn(bookId, id, number, version, lyric)

  fun bundleOf(book: BookId, songs: List<Song>, refs: List<SongReference>? = null) =
    Arb.bookBundle(
      book = Arb.portableBook(id = Arb.constant(book)),
      songs = Arb.constant(songs),
      songReferences = Arb.constant(refs),
    ).next(rs)

  feature("clean import") {
    scenario("inserts the book, all songs and their numbers, and marks the book DOWNLOADED") {
      checkAll(15, bundleForBook(bookId)) { bundle ->
        withDb { db ->
          BookImporterImpl(db).import(bundle)

          db.bookDao().getById(bookId).shouldNotBeNull()
          db.bookStatisticDao().getById(bookId).shouldNotBeNull().priority shouldBe bundle.book.priority
          db.songDao().count() shouldBe bundle.songs.size
          db.installedBookDao().getByBookId(bookId).shouldNotBeNull().source shouldBe BookInstallSource.DOWNLOADED
        }
      }
    }
  }

  feature("user-edited songs are preserved on re-import") {
    scenario("does not overwrite the lyric of a song marked edited") {
      withDb { db ->
        val importer = BookImporterImpl(db)
        val song = Arb.portableSong(
          id = Arb.constant(SongId(1L)),
          number = Arb.constant(SongNumber(bookId, 1)),
          lyric = Arb.constant("original"),
        ).next(rs)
        importer.import(Arb.bookBundle(book = Arb.portableBook(id = Arb.constant(bookId)), songs = Arb.constant(listOf(song))).next(rs))

        // user edits the song
        db.songDao().update(db.songDao().getById(SongId(1L))!!.copy(lyric = "my edit", edited = true))

        // re-import a newer bundle shipping a different lyric for the same song
        val updated = song.copy(lyric = "updated from bundle", version = Version(2, 0))
        importer.import(
          Arb.bookBundle(
            book = Arb.portableBook(id = Arb.constant(bookId), version = Arb.constant(Version(2, 0))),
            songs = Arb.constant(listOf(updated)),
          ).next(rs)
        )

        db.songDao().getById(SongId(1L)).shouldNotBeNull().lyric shouldBe "my edit"
      }
    }
  }

  feature("cross-book song references") {
    scenario("defers a reference until the referenced song's book is installed") {
      withDb { db ->
        val importer = BookImporterImpl(db)
        val book2Id = BookId.parse("Book-2")
        val ref = Arb.songReference(songId = Arb.constant(SongId(1L)), refSongId = Arb.constant(SongId(2L))).next(rs)

        // Book-1 ships song 1 plus a ref to song 2 (which lives in Book-2, not yet installed)
        val song1 = Arb.portableSong(id = Arb.constant(SongId(1L)), number = Arb.constant(SongNumber(bookId, 1))).next(rs)
        importer.import(
          Arb.bookBundle(book = Arb.portableBook(id = Arb.constant(bookId)), songs = Arb.constant(listOf(song1)), songReferences = Arb.constant(listOf(ref))).next(rs)
        )
        db.songReferenceDao().getById(SongId(1L), SongId(2L)).shouldBeNull()

        // installing Book-2 (containing song 2) with the same ref now creates it
        val song2 = Arb.portableSong(id = Arb.constant(SongId(2L)), number = Arb.constant(SongNumber(book2Id, 1))).next(rs)
        importer.import(
          Arb.bookBundle(book = Arb.portableBook(id = Arb.constant(book2Id)), songs = Arb.constant(listOf(song2)), songReferences = Arb.constant(listOf(ref))).next(rs)
        )
        db.songReferenceDao().getById(SongId(1L), SongId(2L)).shouldNotBeNull()
      }
    }
  }

  feature("predefined tags from the bundle") {
    scenario("binds a tag to the song matched by book id and number; skips unmatched") {
      withDb { db ->
        val songs = listOf(
          Arb.portableSong(id = Arb.constant(SongId(1L)), number = Arb.constant(SongNumber(bookId, 1))).next(rs),
          Arb.portableSong(id = Arb.constant(SongId(2L)), number = Arb.constant(SongNumber(bookId, 2))).next(rs),
        )
        val tag = Arb.portableTag(
          id = Arb.constant(io.github.alelk.pws.domain.core.ids.TagId.parse("prayer")),
          name = Arb.constant("Prayer"),
          songs = Arb.constant(setOf(SongNumber(bookId, 1))),
        ).next(rs)

        BookImporterImpl(db).import(
          Arb.bookBundle(book = Arb.portableBook(id = Arb.constant(bookId)), songs = Arb.constant(songs), tags = Arb.constant(listOf(tag))).next(rs)
        )

        db.songTagDao().getBySongId(SongId(1L)).map { it.tagId.toString() } shouldBe listOf("prayer")
        db.songTagDao().getBySongId(SongId(2L)) shouldBe emptyList()
      }
    }
  }

  feature("idempotency") {
    scenario("importing the same bundle twice does not duplicate rows") {
      checkAll(15, bundleForBook(bookId)) { bundle ->
        withDb { db ->
          val importer = BookImporterImpl(db)
          importer.import(bundle)
          importer.import(bundle)
          db.songDao().count() shouldBe bundle.songs.size
        }
      }
    }
  }

  feature("smart song-number binding") {
    scenario("remaps a number to a new song id and cleans up the old orphan") {
      withDb { db ->
        val importer = BookImporterImpl(db)
        importer.import(bundleOf(bookId, listOf(song(id = 1L, number = 1, version = Version(1, 0)))))
        // optimizer reassigned the id: number 1 now points to song 2
        importer.import(bundleOf(bookId, listOf(song(id = 2L, number = 1, version = Version(1, 0)))))

        db.songNumberDao().getByBookIdAndSongNumber(bookId, 1).shouldNotBeNull().songId shouldBe SongId(2L)
        db.songDao().getById(SongId(1L)).shouldBeNull()    // orphan removed
        db.songDao().getById(SongId(2L)).shouldNotBeNull()
      }
    }

    scenario("survives a song-id swap between two numbers without a constraint crash") {
      withDb { db ->
        val importer = BookImporterImpl(db)
        importer.import(bundleOf(bookId, listOf(song(1L, 1, Version(1, 0)), song(2L, 2, Version(1, 0)))))
        // numbers trade song ids — the case a per-number upsert cannot express
        importer.import(bundleOf(bookId, listOf(song(1L, 2, Version(1, 0)), song(2L, 1, Version(1, 0)))))

        db.songNumberDao().getByBookIdAndSongNumber(bookId, 1).shouldNotBeNull().songId shouldBe SongId(2L)
        db.songNumberDao().getByBookIdAndSongNumber(bookId, 2).shouldNotBeNull().songId shouldBe SongId(1L)
      }
    }

    scenario("drops a number that disappeared from the re-imported bundle") {
      withDb { db ->
        val importer = BookImporterImpl(db)
        importer.import(bundleOf(bookId, listOf(song(1L, 1, Version(1, 0)), song(2L, 2, Version(1, 0)), song(3L, 3, Version(1, 0)))))
        importer.import(bundleOf(bookId, listOf(song(1L, 1, Version(1, 0)), song(2L, 2, Version(1, 0)))))

        db.songNumberDao().getByBookIdAndSongNumber(bookId, 3).shouldBeNull()
        db.songDao().getById(SongId(3L)).shouldBeNull()    // orphan removed
        db.songNumberDao().getByBookIdAndSongNumber(bookId, 1).shouldNotBeNull()
      }
    }

    scenario("keeps a user-edited song on its number even when the bundle remaps it") {
      withDb { db ->
        val importer = BookImporterImpl(db)
        importer.import(bundleOf(bookId, listOf(song(1L, 1, Version(1, 0), lyric = "original"))))
        db.songDao().update(db.songDao().getById(SongId(1L))!!.copy(lyric = "my edit", edited = true))

        importer.import(bundleOf(bookId, listOf(song(2L, 1, Version(1, 0)))))   // remap number 1 → song 2

        db.songNumberDao().getByBookIdAndSongNumber(bookId, 1).shouldNotBeNull().songId shouldBe SongId(1L)
        db.songDao().getById(SongId(1L)).shouldNotBeNull().lyric shouldBe "my edit"
      }
    }
  }

  feature("song version gating") {
    scenario("ignores an older bundle version and applies a newer one") {
      withDb { db ->
        val importer = BookImporterImpl(db)
        importer.import(bundleOf(bookId, listOf(song(1L, 1, Version(2, 0), lyric = "v2"))))

        importer.import(bundleOf(bookId, listOf(song(1L, 1, Version(1, 0), lyric = "v1"))))   // older → ignored
        db.songDao().getById(SongId(1L)).shouldNotBeNull().lyric shouldBe "v2"

        importer.import(bundleOf(bookId, listOf(song(1L, 1, Version(3, 0), lyric = "v3"))))   // newer → applied
        db.songDao().getById(SongId(1L)).shouldNotBeNull().lyric shouldBe "v3"
      }
    }
  }

  feature("update preserves user data") {
    scenario("favorites, history and cross-book numbers survive a book update") {
      withDb { db ->
        val importer = BookImporterImpl(db)
        val book2 = BookId.parse("Book-2")
        // song 1 lives in both books
        importer.import(bundleOf(bookId, listOf(song(1L, 1, Version(1, 0)))))
        importer.import(bundleOf(book2, listOf(songIn(book2, 1L, 7, Version(1, 0)))))

        db.favoriteDao().addToFavorites(SongNumberId(bookId, SongId(1L)))
        db.historyDao().insert(HistoryEntity(SongNumberId(bookId, SongId(1L))))

        // update Book-1 to a newer version (same id, same number → row left untouched)
        importer.import(bundleOf(bookId, listOf(song(1L, 1, Version(2, 0), lyric = "updated"))))

        db.favoriteDao().getById(bookId, SongId(1L)).shouldNotBeNull()
        db.historyDao().count() shouldBe 1
        db.songNumberDao().getByBookIdAndSongNumber(book2, 7).shouldNotBeNull().songId shouldBe SongId(1L)
        db.songDao().getById(SongId(1L)).shouldNotBeNull().lyric shouldBe "updated"
      }
    }
  }

  feature("song references are recalculated on re-import") {
    scenario("removes a reference that is no longer present in the bundle") {
      withDb { db ->
        val importer = BookImporterImpl(db)
        val ref = Arb.songReference(songId = Arb.constant(SongId(1L)), refSongId = Arb.constant(SongId(2L))).next(rs)
        importer.import(bundleOf(bookId, listOf(song(1L, 1, Version(1, 0)), song(2L, 2, Version(1, 0))), refs = listOf(ref)))
        db.songReferenceDao().getById(SongId(1L), SongId(2L)).shouldNotBeNull()

        importer.import(bundleOf(bookId, listOf(song(1L, 1, Version(1, 0)), song(2L, 2, Version(1, 0)))))   // no refs
        db.songReferenceDao().getById(SongId(1L), SongId(2L)).shouldBeNull()
      }
    }
  }
})
