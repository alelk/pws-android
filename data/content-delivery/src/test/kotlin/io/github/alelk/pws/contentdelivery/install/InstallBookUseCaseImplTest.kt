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
import io.github.alelk.pws.contentdelivery.portableSongNumber
import io.github.alelk.pws.database.PwsDatabase
import io.github.alelk.pws.domain.booklibrary.model.BookCatalogEntry
import io.github.alelk.pws.domain.booklibrary.model.DownloadState
import io.github.alelk.pws.domain.core.Locale
import io.github.alelk.pws.domain.core.Version
import io.github.alelk.pws.domain.core.ids.BookId
import io.github.alelk.pws.portable.model.BookBundle
import io.github.alelk.pws.portable.serialization.BundleCrypto
import io.github.alelk.pws.portable.serialization.BundleSerializer
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.RandomSource
import io.kotest.property.arbitrary.constant
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.next
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import java.security.MessageDigest

@RobolectricTest(sdk = 34)
class InstallBookUseCaseImplTest : FeatureSpec({

  val keyHex = "00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff"
  val key = BundleCrypto.keyFromHex(keyHex)

  fun encrypt(bundle: BookBundle): ByteArray =
    BundleSerializer.encodeGzipEncrypted(bundle, key)

  fun sha256Hex(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

  fun entry(checksum: String, size: Long) = BookCatalogEntry(
    bookId = BookId.parse("Book-1"),
    locales = listOf(Locale.of("ru")),
    name = "Book Book-1",
    displayName = "Book Book-1",
    bundleVersion = Version(1, 0),
    downloadUrl = "https://example.test/Book-1-ru-1.0.book.yaml.gz.enc",
    fileSizeBytes = size,
    checksum = checksum,
    songCount = 1,
  )

  fun useCase(db: PwsDatabase, bytes: ByteArray, fail: Boolean = false): InstallBookUseCaseImpl {
    val engine = MockEngine {
      if (fail) throw java.io.IOException("connection reset")
      respond(
        content = bytes,
        headers = headersOf(HttpHeaders.ContentLength, bytes.size.toString()),
      )
    }
    return InstallBookUseCaseImpl(
      context = ApplicationProvider.getApplicationContext<Context>(),
      importer = BookImporterImpl(db),
      keyProvider = ContentKeyProvider { keyHex },
      httpClient = HttpClient(engine),
    )
  }

  val bookId = BookId.parse("Book-1")
  val bundle = Arb.bookBundle(
    book = Arb.portableBook(id = Arb.constant(bookId)),
    songs = Arb.list(Arb.portableSong(number = Arb.portableSongNumber(bookId = Arb.constant(bookId))), 1..3),
  ).next(RandomSource.seeded(20260621L))

  feature("successful install") {
    scenario("emits Done and imports the book into the database") {
      runTest {
        val db = inMemoryPwsDb()
        val bytes = encrypt(bundle)
        val states = useCase(db, bytes).invoke(entry(sha256Hex(bytes), bytes.size.toLong())).toList()

        states.last() shouldBe DownloadState.Done
        states.any { it is DownloadState.Downloading } shouldBe true
        db.bookDao().getById(BookId.parse("Book-1")).shouldNotBeNull()
        db.close()
      }
    }
  }

  feature("checksum verification") {
    scenario("emits Error and does not import when checksum mismatches") {
      runTest {
        val db = inMemoryPwsDb()
        val bytes = encrypt(bundle)
        val states = useCase(db, bytes).invoke(entry("deadbeef", bytes.size.toLong())).toList()

        states.last().shouldBeInstanceOf<DownloadState.Error>()
        db.bookDao().getById(BookId.parse("Book-1")).shouldBeNull()
        db.close()
      }
    }
  }

  feature("network failure") {
    scenario("emits Error without crashing the flow") {
      runTest {
        val db = inMemoryPwsDb()
        val states = useCase(db, ByteArray(0), fail = true).invoke(entry("x", 0)).toList()

        states.last().shouldBeInstanceOf<DownloadState.Error>()
        db.bookDao().getById(BookId.parse("Book-1")).shouldBeNull()
        db.close()
      }
    }
  }
})
