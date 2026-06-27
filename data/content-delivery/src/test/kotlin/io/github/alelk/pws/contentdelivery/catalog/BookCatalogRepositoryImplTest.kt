package io.github.alelk.pws.contentdelivery.catalog

import br.com.colman.kotest.FeatureSpec
import br.com.colman.kotest.android.extensions.robolectric.RobolectricTest
import io.github.alelk.pws.contentdelivery.portableBook
import io.github.alelk.pws.domain.core.ids.BookId
import io.github.alelk.pws.portable.model.BookCatalog
import io.github.alelk.pws.portable.serialization.CatalogSerializer
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.RandomSource
import io.kotest.property.arbitrary.constant
import io.kotest.property.arbitrary.next
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import arrow.core.Either
import io.github.alelk.pws.portable.model.BookCatalogEntry as PortableEntry

@RobolectricTest(sdk = 34)
class BookCatalogRepositoryImplTest : FeatureSpec({

  val catalogUrl = "https://example.test/catalog/index.json"

  val catalog = BookCatalog(
    version = "2026.1",
    books = listOf(
      PortableEntry(
        book = Arb.portableBook(
          id = Arb.constant(BookId.parse("Book-1")),
          name = Arb.constant("Book One"),
          version = Arb.constant(io.github.alelk.pws.domain.core.Version(1, 0)),
        ).next(RandomSource.seeded(20260621L)),
        songCount = 12,
        fileSizeBytes = 3456,
        checksum = "abc123",
      )
    ),
  )

  fun repoWith(engine: MockEngine) =
    BookCatalogRepositoryImpl(catalogUrl, bundleVariant = "ru", httpClient = HttpClient(engine))

  feature("successful fetch") {
    scenario("maps catalog entries to domain and builds the download URL") {
      runBlocking {
        val json = CatalogSerializer.encode(catalog)
        val engine = MockEngine {
          respond(json, headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val result = repoWith(engine).getAvailableBooks()

        result.shouldBeInstanceOf<Either.Right<*>>()
        val entry = (result as Either.Right).value.single()
        entry.bookId.toString() shouldBe "Book-1"
        entry.name shouldBe "Book One"
        entry.songCount shouldBe 12
        entry.fileSizeBytes shouldBe 3456L
        entry.downloadUrl shouldBe "https://example.test/catalog/Book-1-ru-2026.1.book.yaml.gz.enc"
      }
    }
  }

  feature("retry behaviour") {
    scenario("recovers when the first attempt fails and the second succeeds") {
      runBlocking {
        var attempt = 0
        val json = CatalogSerializer.encode(catalog)
        val engine = MockEngine {
          attempt++
          if (attempt == 1) respondError(HttpStatusCode.BadGateway)
          else respond(json, headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val result = repoWith(engine).getAvailableBooks()

        result.shouldBeInstanceOf<Either.Right<*>>()
        attempt shouldBe 2
      }
    }

    scenario("returns Left after all attempts fail") {
      runBlocking {
        var attempt = 0
        val engine = MockEngine {
          attempt++
          respondError(HttpStatusCode.InternalServerError)
        }
        val result = repoWith(engine).getAvailableBooks()

        result.shouldBeInstanceOf<Either.Left<*>>()
        attempt shouldBe 3
      }
    }
  }
})
