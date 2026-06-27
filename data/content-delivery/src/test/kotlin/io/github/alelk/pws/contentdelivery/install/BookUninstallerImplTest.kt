package io.github.alelk.pws.contentdelivery.install

import br.com.colman.kotest.FeatureSpec
import br.com.colman.kotest.android.extensions.robolectric.RobolectricTest
import io.github.alelk.pws.contentdelivery.inMemoryPwsDb
import io.github.alelk.pws.database.PwsDatabase
import io.github.alelk.pws.database.book.bookEntity
import io.github.alelk.pws.database.bookstatistic.bookStatisticEntity
import io.github.alelk.pws.database.installed_book.InstalledBookEntity
import io.github.alelk.pws.database.song.songEntity
import io.github.alelk.pws.database.song_number.songNumberEntity
import io.github.alelk.pws.database.song_tag.SongTagEntity
import io.github.alelk.pws.database.tag.tagEntity
import io.github.alelk.pws.domain.booklibrary.model.BookInstallSource
import io.github.alelk.pws.domain.core.Version
import io.github.alelk.pws.domain.core.ids.BookId
import io.github.alelk.pws.domain.core.ids.SongId
import io.github.alelk.pws.domain.core.ids.SongNumberId
import io.github.alelk.pws.domain.core.ids.TagId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.RandomSource
import io.kotest.property.arbitrary.constant
import io.kotest.property.arbitrary.next

/**
 * Tests for [BookUninstallerImpl] — removes a downloaded book and its data. The critical guarantee
 * is **orphan detection**: a song shared with another book must survive, while a song that belongs
 * only to the uninstalled book (and its favourites / tags via FK cascade) must be removed.
 */
@RobolectricTest(sdk = 34)
class BookUninstallerImplTest : FeatureSpec({

  val rs = RandomSource.seeded(20260621L)
  val book1 = BookId.parse("Book-1")
  val book2 = BookId.parse("Book-2")
  val orphanSong = SongId(1L)   // lives only in Book-1
  val sharedSong = SongId(2L)   // lives in Book-1 and Book-2

  suspend fun seed(db: PwsDatabase, book1Source: BookInstallSource = BookInstallSource.DOWNLOADED) {
    db.bookDao().insert(Arb.bookEntity(id = Arb.constant(book1)).next(rs))
    db.bookDao().insert(Arb.bookEntity(id = Arb.constant(book2)).next(rs))
    db.bookStatisticDao().insert(Arb.bookStatisticEntity(id = Arb.constant(book1)).next(rs))
    db.bookStatisticDao().insert(Arb.bookStatisticEntity(id = Arb.constant(book2)).next(rs))
    db.songDao().insert(Arb.songEntity(id = Arb.constant(orphanSong)).next(rs))
    db.songDao().insert(Arb.songEntity(id = Arb.constant(sharedSong)).next(rs))
    db.songNumberDao().insert(
      listOf(
        Arb.songNumberEntity(bookId = Arb.constant(book1), songId = Arb.constant(orphanSong), number = Arb.constant(1)).next(rs),
        Arb.songNumberEntity(bookId = Arb.constant(book1), songId = Arb.constant(sharedSong), number = Arb.constant(2)).next(rs),
        Arb.songNumberEntity(bookId = Arb.constant(book2), songId = Arb.constant(sharedSong), number = Arb.constant(5)).next(rs),
      )
    )
    // favourite the orphan in Book-1 and the shared song in Book-2
    db.favoriteDao().addToFavorites(SongNumberId(book1, orphanSong))
    db.favoriteDao().addToFavorites(SongNumberId(book2, sharedSong))
    // tag on the orphan song
    val tagId = TagId.parse("prayer")
    db.tagDao().insert(Arb.tagEntity(id = Arb.constant(tagId), predefined = Arb.constant(true)).next(rs))
    db.songTagDao().insert(SongTagEntity(songId = orphanSong, tagId = tagId))
    // installed records
    db.installedBookDao().upsert(InstalledBookEntity(book1, Version(1, 0), 0, book1Source))
    db.installedBookDao().upsert(InstalledBookEntity(book2, Version(1, 0), 0, BookInstallSource.DOWNLOADED))
  }

  suspend fun <T> withDb(block: suspend (PwsDatabase) -> T): T {
    val db = inMemoryPwsDb()
    return try { block(db) } finally { db.close() }
  }

  feature("uninstall a downloaded book") {
    scenario("removes the book, its statistic and installed record but keeps the other book") {
      withDb { db ->
        seed(db)
        BookUninstallerImpl(db).uninstall(book1)

        db.bookDao().getById(book1).shouldBeNull()
        db.bookStatisticDao().getById(book1).shouldBeNull()
        db.installedBookDao().getByBookId(book1).shouldBeNull()
        db.bookDao().getById(book2).shouldNotBeNull()
        db.installedBookDao().getByBookId(book2).shouldNotBeNull()
      }
    }

    scenario("deletes an orphan song but keeps a song shared with another book") {
      withDb { db ->
        seed(db)
        BookUninstallerImpl(db).uninstall(book1)

        db.songDao().getById(orphanSong).shouldBeNull()
        db.songDao().getById(sharedSong).shouldNotBeNull()
        // the shared song's Book-2 number survives; no Book-1 numbers remain
        db.songNumberDao().getByBookIds(listOf(book1)) shouldBe emptyList()
        db.songNumberDao().getById(book2, sharedSong).shouldNotBeNull()
      }
    }

    scenario("cascades removal of the orphan's favourite and tag, keeps the shared song's favourite") {
      withDb { db ->
        seed(db)
        BookUninstallerImpl(db).uninstall(book1)

        // orphan favourite gone (cascade via song_number), shared song's Book-2 favourite remains
        db.favoriteDao().count() shouldBe 1
        db.favoriteDao().getById(book2, sharedSong).shouldNotBeNull()
        // orphan song's tag association gone (cascade via song delete)
        db.songTagDao().count() shouldBe 0
      }
    }
  }

  feature("uninstall guards") {
    scenario("refuses to uninstall a built-in (ASSET) book") {
      withDb { db ->
        seed(db, book1Source = BookInstallSource.ASSET)
        shouldThrow<IllegalStateException> { BookUninstallerImpl(db).uninstall(book1) }
        db.bookDao().getById(book1).shouldNotBeNull()  // nothing removed
      }
    }

    scenario("fails when the book is not installed") {
      withDb { db ->
        shouldThrow<IllegalStateException> { BookUninstallerImpl(db).uninstall(BookId.parse("Unknown")) }
      }
    }
  }
})

