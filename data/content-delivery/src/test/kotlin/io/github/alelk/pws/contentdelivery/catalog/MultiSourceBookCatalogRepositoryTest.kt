package io.github.alelk.pws.contentdelivery.catalog

import arrow.core.Either
import br.com.colman.kotest.FeatureSpec
import br.com.colman.kotest.android.extensions.robolectric.RobolectricTest
import io.github.alelk.pws.contentdelivery.portableBook
import io.github.alelk.pws.domain.booklibrary.model.ContentSource
import io.github.alelk.pws.domain.core.Version
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
import io.github.alelk.pws.portable.model.BookCatalogEntry as PortableEntry

@RobolectricTest(sdk = 34)
class MultiSourceBookCatalogRepositoryTest : FeatureSpec({

  val primaryUrl = "https://primary.test/catalog/index.json"
  val secondaryUrl = "https://secondary.test/catalog/index.json"

  fun catalogJson(bookId: String) = CatalogSerializer.encode(
    BookCatalog(
      version = "2026.1",
      books = listOf(
        PortableEntry(
          book = Arb.portableBook(
            id = Arb.constant(BookId.parse(bookId)),
            name = Arb.constant(bookId),
            version = Arb.constant(Version(1, 0)),
          ).next(RandomSource.seeded(20260621L)),
          songCount = 1,
          fileSizeBytes = 10,
          checksum = "sum",
        )
      ),
    )
  )

  fun sources() = listOf(
    ContentSource(name = "primary", catalogUrl = primaryUrl, priority = 0),
    ContentSource(name = "secondary", catalogUrl = secondaryUrl, priority = 1),
  )

  feature("source fallback") {
    scenario("falls over to the next source when the primary is unreachable") {
      runBlocking {
        var primaryHits = 0
        val engine = MockEngine { request ->
          if (request.url.toString().startsWith("https://primary.test")) {
            primaryHits++
            respondError(HttpStatusCode.BadGateway)
          } else {
            respond(catalogJson("Book-2"), headers = headersOf(HttpHeaders.ContentType, "application/json"))
          }
        }
        val repo = MultiSourceBookCatalogRepository(sources(), "ru", HttpClient(engine))

        val result = repo.getAvailableBooks()

        result.shouldBeInstanceOf<Either.Right<*>>()
        (result as Either.Right).value.single().bookId.toString() shouldBe "Book-2"
        // fast failover: the primary is hit once, not retried before trying the secondary
        primaryHits shouldBe 1
      }
    }

    scenario("returns Left when every source fails") {
      runBlocking {
        val engine = MockEngine { respondError(HttpStatusCode.InternalServerError) }
        val repo = MultiSourceBookCatalogRepository(sources(), "ru", HttpClient(engine), rounds = 1)

        repo.getAvailableBooks().shouldBeInstanceOf<Either.Left<*>>()
      }
    }

    scenario("prefers the highest-priority (lowest number) source") {
      runBlocking {
        val engine = MockEngine { request ->
          val body = if (request.url.toString().startsWith("https://primary.test")) catalogJson("Book-1") else catalogJson("Book-2")
          respond(body, headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val repo = MultiSourceBookCatalogRepository(sources(), "ru", HttpClient(engine))

        val result = repo.getAvailableBooks()

        result.shouldBeInstanceOf<Either.Right<*>>()
        (result as Either.Right).value.single().bookId.toString() shouldBe "Book-1"
      }
    }
  }
})
