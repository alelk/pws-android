package io.github.alelk.pws.contentdelivery.install

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import br.com.colman.kotest.FeatureSpec
import br.com.colman.kotest.android.extensions.robolectric.RobolectricTest
import io.github.alelk.pws.contentdelivery.ContentKeyProvider
import io.github.alelk.pws.contentdelivery.bookBundle
import io.github.alelk.pws.contentdelivery.inMemoryPwsDb
import io.github.alelk.pws.contentdelivery.portableBook
import io.github.alelk.pws.contentdelivery.portableSong
import io.github.alelk.pws.database.PwsDatabase
import io.github.alelk.pws.domain.booklibrary.model.BookInstallSource
import io.github.alelk.pws.domain.core.Version
import io.github.alelk.pws.domain.core.ids.BookId
import io.github.alelk.pws.domain.core.ids.SongId
import io.github.alelk.pws.portable.model.SongNumber
import io.github.alelk.pws.portable.serialization.BundleSerializer
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.RandomSource
import io.kotest.property.arbitrary.constant
import io.kotest.property.arbitrary.next

/**
 * Tests for [SeedBooksFromAssetsUseCase] — the first-launch importer for bundles baked into the APK
 * of "preloaded" build variants. Exercises the core [SeedBooksFromAssetsUseCase.seedBundles] logic
 * with in-memory bundle bytes (decoupled from `AssetManager`): ASSET marking, file-name idempotency,
 * version-aware re-seeding on an APK update, and the clean-variant return value.
 */
@RobolectricTest(sdk = 34)
class SeedBooksFromAssetsUseCaseTest : FeatureSpec({

  val rs = RandomSource.seeded(20260807L)

  // Plain-gzip bundle bytes (decodeBookAuto reads plain and encrypted alike, so the test key is
  // irrelevant to decoding — only its shape must be a valid 32-byte hex key).
  fun bundleBytes(book: BookId, songId: Long = 1L, number: Int = 1, version: Version = Version(1, 0), lyric: String = "lyric"): ByteArray {
    val song = Arb.portableSong(
      id = Arb.constant(SongId(songId)),
      number = Arb.constant(SongNumber(book, number)),
      version = Arb.constant(version),
      lyric = Arb.constant(lyric),
    ).next(rs)
    val bundle = Arb.bookBundle(book = Arb.portableBook(id = Arb.constant(book)), songs = Arb.constant(listOf(song))).next(rs)
    return BundleSerializer.encodeGzip(bundle)
  }

  // Fresh seeder with cleared idempotency prefs, so scenarios don't leak "already seeded" state.
  // The context is fetched here (inside a scenario), not at spec-init time when Robolectric's
  // Application is not yet up.
  fun seeder(db: PwsDatabase): SeedBooksFromAssetsUseCase {
    val context = ApplicationProvider.getApplicationContext<Context>()
    context.getSharedPreferences("pws_seed_books", Context.MODE_PRIVATE).edit().clear().commit()
    return SeedBooksFromAssetsUseCase(context, db, BookImporterImpl(db), ContentKeyProvider { "00".repeat(32) })
  }

  suspend fun <T> withDb(block: suspend (PwsDatabase) -> T): T {
    val db = inMemoryPwsDb()
    return try { block(db) } finally { db.close() }
  }

  feature("seeding preloaded bundles") {
    scenario("imports a bundle as a non-removable built-in and reports content present") {
      withDb { db ->
        val book = BookId.parse("Book-1")
        val bytes = bundleBytes(book)

        val ready = seeder(db).seedBundles(listOf("Book-1-debug-1.0.book.yaml.gz.enc")) { bytes }

        ready shouldBe true
        db.installedBookDao().getByBookId(book).shouldNotBeNull().source shouldBe BookInstallSource.ASSET
      }
    }
  }

  feature("idempotency by file name") {
    scenario("does not re-import a bundle whose file name was already seeded") {
      withDb { db ->
        val name = "Book-1-debug-1.0.book.yaml.gz.enc"
        val bookA = BookId.parse("Book-1")
        val bookB = BookId.parse("Book-2")
        val useCase = seeder(db)

        useCase.seedBundles(listOf(name)) { bundleBytes(bookA) }
        // Same file name, different content — must be skipped because `name` is already seeded.
        useCase.seedBundles(listOf(name)) { bundleBytes(bookB) }

        db.installedBookDao().getByBookId(bookA).shouldNotBeNull()
        db.installedBookDao().getByBookId(bookB).shouldBeNull()
      }
    }

    scenario("seeds a new file name (newer version) shipped by an APK update") {
      withDb { db ->
        val book = BookId.parse("Book-1")
        val useCase = seeder(db)

        useCase.seedBundles(listOf("Book-1-debug-1.0.book.yaml.gz.enc")) { bundleBytes(book, version = Version(1, 0), lyric = "v1") }
        // APK update ships a newer bundle → new file name → imported; song updated, still built-in.
        useCase.seedBundles(listOf("Book-1-debug-2.0.book.yaml.gz.enc")) { bundleBytes(book, version = Version(2, 0), lyric = "v2") }

        db.songDao().getById(SongId(1L)).shouldNotBeNull().lyric shouldBe "v2"
        db.installedBookDao().getByBookId(book).shouldNotBeNull().source shouldBe BookInstallSource.ASSET
      }
    }
  }

  feature("clean variant") {
    scenario("returns false when there are no seed assets and no built-in books") {
      withDb { db ->
        seeder(db).seedBundles(emptyList()) { error("must not read bytes") } shouldBe false
      }
    }

    scenario("reports built-in content already present from a previous launch") {
      withDb { db ->
        val book = BookId.parse("Book-1")
        val useCase = seeder(db)
        useCase.seedBundles(listOf("Book-1-debug-1.0.book.yaml.gz.enc")) { bundleBytes(book) }

        // A later launch finds no assets to read, but must still report content is present.
        useCase.seedBundles(emptyList()) { error("must not read bytes") } shouldBe true
      }
    }
  }
})
